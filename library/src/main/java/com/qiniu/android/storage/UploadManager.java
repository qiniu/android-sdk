package com.qiniu.android.storage;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Build;

import com.qiniu.android.collect.ReportItem;
import com.qiniu.android.collect.UploadInfoReporter;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.dns.DnsPrefetchTransaction;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.metrics.UploadSingleRequestMetrics;
import com.qiniu.android.http.metrics.UploadTaskMetrics;
import com.qiniu.android.storage.serverConfig.ServerConfig;
import com.qiniu.android.storage.serverConfig.ServerConfigMonitor;
import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.ContextGetter;
import com.qiniu.android.utils.Utils;
import com.qiniu.android.utils.Wait;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


public class UploadManager {

    private final Configuration config;


    public UploadManager(Recorder recorder) {
        this(recorder, null);
    }

    public UploadManager(Recorder recorder, KeyGenerator keyGen) {
        this(new Configuration.Builder().recorder(recorder, keyGen).build());
    }

    /**
     * default 1 Threads
     */
    public UploadManager() {
        this(new Configuration.Builder().build());
    }

    /**
     * @param config Configuration, default 1 Thread
     */
    public UploadManager(Configuration config) {
        this.config = config != null ? config : new Configuration.Builder().build();
        DnsPrefetchTransaction.addDnsLocalLoadTransaction();
        DnsPrefetchTransaction.setDnsCheckWhetherCachedValidTransactionAction();
        ServerConfigMonitor.startMonitor();
    }

    /**
     * 上传数据
     *
     * @param data     上传的数据
     * @param key      上传数据保存的文件名
     * @param token    上传凭证
     * @param complete 上传完成后续处理动作
     * @param options  上传数据的可选参数
     */
    public void put(final byte[] data,
                    final String key,
                    final String token,
                    final UpCompletionHandler complete,
                    final UploadOptions options) {
        if (checkAndNotifyError(key, token, data, complete)) {
            return;
        }
        putData(data, null, key, token, options, complete);
    }

    /**
     * 上传文件
     *
     * @param filePath          上传的文件路径
     * @param key               上传文件保存的文件名
     * @param token             上传凭证
     * @param completionHandler 上传完成的后续处理动作
     * @param options           上传数据的可选参数
     */
    public void put(String filePath,
                    String key,
                    String token,
                    UpCompletionHandler completionHandler,
                    final UploadOptions options) {
        if (checkAndNotifyError(key, token, filePath, completionHandler)) {
            return;
        }
        put(new File(filePath), key, token, completionHandler, options);
    }


    /**
     * 上传文件
     *
     * @param file              上传的文件对象
     * @param key               上传文件保存的文件名
     * @param token             上传凭证
     * @param completionHandler 上传完成的后续处理动作
     * @param options           上传数据的可选参数
     */
    public void put(final File file,
                    final String key,
                    final String token,
                    final UpCompletionHandler completionHandler,
                    final UploadOptions options) {
        if (checkAndNotifyError(key, token, file, completionHandler)) {
            return;
        }
        putSource(new UploadSourceFile(file), key, token, options, completionHandler);
    }

    /**
     * 上传文件
     *
     * @param uri               上传的文件对象 Uri
     * @param resolver          resolver, 在根据 Uri 构建 InputStream 时使用
     *                          注：为 null 时，使用 {@link ContextGetter#applicationContext()} 获取 resolver
     * @param key               上传文件保存的文件名
     * @param token             上传凭证
     * @param completionHandler 上传完成的后续处理动作
     * @param options           上传数据的可选参数
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void put(final Uri uri,
                    final ContentResolver resolver,
                    final String key,
                    final String token,
                    final UpCompletionHandler completionHandler,
                    final UploadOptions options) {
        if (checkAndNotifyError(key, token, uri, completionHandler)) {
            return;
        }
        putSource(new UploadSourceUri(uri, resolver), key, token, options, completionHandler);
    }

    /**
     * 上传文件
     *
     * @param inputStream       上传的资源流
     *                          注：资源流需要在上传结束后自行关闭，SDK 内部不做关闭操作
     * @param id                资源 id, 作为构建断点续传信息保存的 key, 如果为空则使用 fileName
     * @param size              上传资源的大小，不知道大小，配置 -1
     * @param fileName          上传资源流的文件名
     * @param key               上传资源保存的文件名
     * @param token             上传凭证
     * @param completionHandler 上传完成的后续处理动作
     * @param options           上传数据的可选参数
     */
    public void put(final InputStream inputStream,
                    final String id,
                    final long size,
                    final String fileName,
                    final String key,
                    final String token,
                    final UpCompletionHandler completionHandler,
                    final UploadOptions options) {
        if (checkAndNotifyError(key, token, inputStream, completionHandler)) {
            return;
        }
        UploadSourceStream stream = new UploadSourceStream(inputStream);
        stream.setId(id);
        stream.setSize(size);
        stream.setFileName(fileName);
        putSource(stream, key, token, options, completionHandler);
    }

    /**
     * 同步上传文件。使用 form 表单方式上传，建议只在数据较小情况下使用此方式，如 file.size() < 1024 * 1024。
     * 注：切勿在主线程调用
     *
     * @param data    上传的数据
     * @param key     上传数据保存的文件名
     * @param token   上传凭证
     * @param options 上传数据的可选参数
     * @return 响应信息 ResponseInfo#response 响应体，序列化后 json 格式
     */
    public ResponseInfo syncPut(byte[] data,
                                String key,
                                String token,
                                UploadOptions options) {

        final Wait wait = new Wait();

        final ArrayList<ResponseInfo> responseInfos = new ArrayList<ResponseInfo>();
        UpCompletionHandler completionHandler = new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                if (info != null) {
                    responseInfos.add(info);
                }
                wait.stopWait();
            }
        };

        if (!checkAndNotifyError(key, token, data, completionHandler)) {
            putData(data, null, key, token, options, completionHandler);
        }

        wait.startWait();

        if (responseInfos.size() > 0) {
            return responseInfos.get(0);
        } else {
            return null;
        }
    }

    /**
     * 同步上传文件。使用 form 表单方式上传，建议只在文件较小情况下使用此方式，如 file.size() < 1024 * 1024。
     * 注：切勿在主线程调用
     *
     * @param file    上传的文件绝对路径
     * @param key     上传数据保存的文件名
     * @param token   上传凭证
     * @param options 上传数据的可选参数
     * @return 响应信息 ResponseInfo#response 响应体，序列化后 json 格式
     */
    public ResponseInfo syncPut(String file, String key, String token, UploadOptions options) {
        return syncPut(new File(file), key, token, options);
    }

    /**
     * 同步上传文件。使用 form 表单方式上传，建议只在文件较小情况下使用此方式，如 file.size() < 1024 * 1024。
     * 注：切勿在主线程调用
     *
     * @param file    上传的文件对象
     * @param key     上传数据保存的文件名
     * @param token   上传凭证
     * @param options 上传数据的可选参数
     * @return 响应信息 ResponseInfo#response 响应体，序列化后 json 格式
     */
    public ResponseInfo syncPut(File file, String key, String token, UploadOptions options) {
        return syncPut(new UploadSourceFile(file), key, token, options);
    }

    /**
     * 同步上传文件。使用 form 表单方式上传，建议只在文件较小情况下使用此方式，如 file.size() < 1024 * 1024。
     * 注：切勿在主线程调用
     *
     * @param uri      上传的文件对象 Uri
     * @param resolver resolver, 在根据 Uri 构建 InputStream 时使用
     *                 注：为 null 时，使用 {@link ContextGetter#applicationContext()} 获取 resolver
     * @param key      上传数据保存的文件名
     * @param token    上传凭证
     * @param options  上传数据的可选参数
     * @return 响应信息 ResponseInfo#response 响应体，序列化后 json 格式
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public ResponseInfo syncPut(Uri uri,
                                ContentResolver resolver,
                                String key,
                                String token,
                                UploadOptions options) {

        return syncPut(new UploadSourceUri(uri, resolver), key, token, options);
    }

    /**
     * 同步上传文件。使用 form 表单方式上传，建议只在文件较小情况下使用此方式，如 file.size() < 1024 * 1024。
     * 注：切勿在主线程调用
     *
     * @param inputStream 上传的资源流
     *                    注：资源流需要在上传结束后自行关闭，SDK 内部不做关闭操作
     * @param id          资源 id, 作为构建断点续传信息保存的 key, 如果为空则使用 fileName
     * @param size        上传资源的大小，不知道大小，配置 -1
     * @param fileName    上传资源流的文件名
     * @param key         上传数据保存的文件名
     * @param token       上传凭证
     * @param options     上传数据的可选参数
     * @return 响应信息 ResponseInfo#response 响应体，序列化后 json 格式
     */
    public ResponseInfo syncPut(InputStream inputStream,
                                String id,
                                long size,
                                String fileName,
                                String key,
                                String token,
                                UploadOptions options) {

        UploadSourceStream stream = new UploadSourceStream(inputStream);
        stream.setId(id);
        stream.setSize(size);
        stream.setFileName(fileName);
        return syncPut(stream, key, token, options);
    }

    /**
     * 同步上传文件。使用 form 表单方式上传，建议只在文件较小情况下使用此方式，如 file.size() < 1024 * 1024。
     * 注：切勿在主线程调用
     *
     * @param source  上传的文件对象
     * @param key     上传数据保存的文件名
     * @param token   上传凭证
     * @param options 上传数据的可选参数
     * @return 响应信息 ResponseInfo#response 响应体，序列化后 json 格式
     */
    private ResponseInfo syncPut(UploadSource source,
                                 String key,
                                 String token,
                                 UploadOptions options) {

        final Wait wait = new Wait();
        final ArrayList<ResponseInfo> responseInfos = new ArrayList<ResponseInfo>();
        UpCompletionHandler completionHandler = new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                if (info != null) {
                    responseInfos.add(info);
                }
                wait.stopWait();
            }
        };

        if (!checkAndNotifyError(key, token, source, completionHandler)) {
            putSource(source, key, token, options, completionHandler);
        }

        wait.startWait();

        if (responseInfos.size() > 0) {
            return responseInfos.get(0);
        } else {
            return null;
        }
    }

    private void putData(final byte[] data,
                         final String fileName,
                         final String key,
                         final String token,
                         final UploadOptions option,
                         final UpCompletionHandler completionHandler) {

        final UpToken t = UpToken.parse(token);
        if (t == null || !t.isValid()) {
            ResponseInfo info = ResponseInfo.invalidToken("invalid token");
            completeAction(token, key, data, info, null, null, completionHandler);
            return;
        }

        DnsPrefetchTransaction.addDnsCheckAndPrefetchTransaction(config.zone, t);
        ServerConfigMonitor.setToken(token);

        BaseUpload.UpTaskCompletionHandler completionHandlerP = new BaseUpload.UpTaskCompletionHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, String key, UploadTaskMetrics requestMetrics, JSONObject response) {
                completeAction(token, key, data, responseInfo, response, requestMetrics, completionHandler);
            }
        };
        final FormUpload up = new FormUpload(data, key, fileName, t, option, config, completionHandlerP);
        AsyncRun.runInBack(up);
    }

    private void putSource(final UploadSource source,
                           final String key,
                           final String token,
                           final UploadOptions option,
                           final UpCompletionHandler completionHandler) {

        if (checkAndNotifyError(key, token, source, completionHandler)) {
            return;
        }

        final UpToken t = UpToken.parse(token);
        if (t == null || !t.isValid()) {
            ResponseInfo info = ResponseInfo.invalidToken("invalid token");
            completeAction(token, key, source, info, null, null, completionHandler);
            return;
        }

        DnsPrefetchTransaction.addDnsCheckAndPrefetchTransaction(config.zone, t);
        ServerConfigMonitor.setToken(token);

        if (source.getSize() > 0 && source.getSize() <= config.putThreshold) {
            ResponseInfo errorInfo = null;
            byte[] data = null;
            try {
                data = source.readData((int) source.getSize(), 0);
            } catch (IOException e) {
                errorInfo = ResponseInfo.localIOError("get upload file data error:" + e.getMessage());
            } finally {
                source.close();
            }
            if (errorInfo == null) {
                putData(data, source.getFileName(), key, token, option, completionHandler);
            } else {
                completeAction(token, key, source, errorInfo, null, null, completionHandler);
            }
            return;
        }

        String recorderKey = key;
        if (config.recorder != null && config.keyGen != null) {
            recorderKey = config.keyGen.gen(key, source.getId());
        }

        BaseUpload.UpTaskCompletionHandler completionHandlerP = new BaseUpload.UpTaskCompletionHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, String key, UploadTaskMetrics requestMetrics, JSONObject response) {
                completeAction(token, key, source, responseInfo, response, requestMetrics, completionHandler);
            }
        };
        if (config.useConcurrentResumeUpload) {
            final ConcurrentResumeUpload up = new ConcurrentResumeUpload(source, key, t, option, config, config.recorder, recorderKey, completionHandlerP);
            AsyncRun.runInBack(up);
        } else {
            final PartsUpload up = new PartsUpload(source, key, t, option, config, config.recorder, recorderKey, completionHandlerP);
            AsyncRun.runInBack(up);
        }
    }

    private boolean checkAndNotifyError(String key,
                                        String token,
                                        Object input,
                                        UpCompletionHandler completionHandler) {
        if (completionHandler == null) {
            throw new NullPointerException("complete handler is null");
        }

        ResponseInfo responseInfo = null;
        if (input == null) {
            responseInfo = ResponseInfo.zeroSize("no input data");
        } else if (input instanceof byte[] && ((byte[]) input).length == 0) {
            responseInfo = ResponseInfo.zeroSize("no input data");
        } else if (input instanceof File && ((File) input).length() == 0) {
            responseInfo = ResponseInfo.zeroSize("file is empty");
        } else if (input instanceof UploadSource && ((UploadSource) input).getSize() == 0) {
            responseInfo = ResponseInfo.zeroSize("file is empty");
        } else if (token == null || token.length() == 0) {
            responseInfo = ResponseInfo.invalidToken("no token");
        }
        if (responseInfo != null) {
            completeAction(token, key, null, responseInfo, responseInfo.response, null, completionHandler);
            return true;
        } else {
            return false;
        }
    }

    private void completeAction(final String token,
                                final String key,
                                final Object source,
                                final ResponseInfo responseInfo,
                                final JSONObject response,
                                final UploadTaskMetrics taskMetrics,
                                final UpCompletionHandler completionHandler) {

        reportQuality(key, source, responseInfo, taskMetrics, token);
        if (completionHandler != null) {
            final Wait wait = new Wait();
            AsyncRun.runInMain(new Runnable() {
                @Override
                public void run() {
                    completionHandler.complete(key, responseInfo, response);
                    wait.stopWait();
                }
            });
            wait.startWait();
        }
    }

    private void reportQuality(String key,
                               Object source,
                               ResponseInfo responseInfo,
                               UploadTaskMetrics taskMetrics,
                               String token) {

        UpToken upToken = UpToken.parse(token);
        if (upToken == null || !upToken.isValid()) {
            return;
        }

        UploadTaskMetrics taskMetricsP = taskMetrics != null ? taskMetrics : new UploadTaskMetrics(null);

        ReportItem item = new ReportItem();
        item.setReport(ReportItem.LogTypeQuality, ReportItem.QualityKeyLogType);
        item.setReport(taskMetricsP.getUpType(), ReportItem.QualityKeyUpType);
        item.setReport((Utils.currentTimestamp() / 1000), ReportItem.QualityKeyUpTime);
        item.setReport(ReportItem.qualityResult(responseInfo), ReportItem.QualityKeyResult);
        item.setReport(key, ReportItem.QualityKeyTargetKey);
        item.setReport(upToken.bucket, ReportItem.QualityKeyTargetBucket);
        item.setReport(taskMetricsP.totalElapsedTime(), ReportItem.QualityKeyTotalElapsedTime);
        if (taskMetricsP.getUcQueryMetrics() != null) {
            item.setReport(taskMetricsP.getUcQueryMetrics().totalElapsedTime(), ReportItem.QualityKeyUcQueryElapsedTime);
        }
        item.setReport(taskMetricsP.requestCount(), ReportItem.QualityKeyRequestsCount);
        item.setReport(taskMetricsP.regionCount(), ReportItem.QualityKeyRegionsCount);
        item.setReport(taskMetricsP.bytesSend(), ReportItem.QualityKeyBytesSent);

        item.setReport(Utils.systemName(), ReportItem.QualityKeyOsName);
        item.setReport(Utils.systemVersion(), ReportItem.QualityKeyOsVersion);
        item.setReport(Utils.sdkLanguage(), ReportItem.QualityKeySDKName);
        item.setReport(Utils.sdkVerion(), ReportItem.QualityKeySDKVersion);

        UploadRegionRequestMetrics lastRegionMetrics = taskMetricsP.lastMetrics();
        if (lastRegionMetrics != null) {
            UploadSingleRequestMetrics lastSingleMetrics = lastRegionMetrics.lastMetrics();
            if (lastSingleMetrics != null) {
                item.setReport(lastSingleMetrics.getHijacked(), ReportItem.BlockKeyHijacking);
            }
        }

        String errorType = ReportItem.requestReportErrorType(responseInfo);
        item.setReport(errorType, ReportItem.QualityKeyErrorType);
        if (responseInfo != null && errorType != null) {
            String errorDesc = responseInfo.error != null ? responseInfo.error : responseInfo.message;
            item.setReport(errorDesc, ReportItem.QualityKeyErrorDescription);
        }

        long fileSize = 0;
        if (source instanceof UploadSource) {
            fileSize = ((UploadSource) source).getSize();
        } else if (source instanceof byte[]) {
            fileSize = ((byte[]) source).length;
        }
        item.setReport((Long)fileSize, ReportItem.QualityKeyFileSize);

        // 统计当前文件上传速度，也即用户感知速度： 总文件大小 / 总耗时
        if (source != null && responseInfo.isOK() && taskMetrics.totalElapsedTime() > 0) {
            if (fileSize > 0) {
                item.setReport(Utils.calculateSpeed(fileSize, taskMetrics.totalElapsedTime()), ReportItem.QualityKeyPerceptiveSpeed);
            }
        }

        UploadInfoReporter.getInstance().report(item, token);
    }
}
