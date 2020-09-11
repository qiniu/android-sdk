package com.qiniu.android.storage;

import com.qiniu.android.collect.ReportItem;
import com.qiniu.android.collect.UploadInfoReporter;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.dns.DnsPrefetchTransaction;
import com.qiniu.android.http.metrics.UploadTaskMetrics;
import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.Utils;
import com.qiniu.android.utils.Wait;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
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
        this.config = config;
        DnsPrefetchTransaction.addDnsLocalLoadTransaction();
        DnsPrefetchTransaction.setDnsCheckWhetherCachedValidTransactionAction();
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
        if (checkAndNotifyError(key, token, data, complete)){
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
        if (checkAndNotifyError(key, token, filePath, completionHandler)){
            return;
        }
        put(new File(filePath), key, token, completionHandler, options);
    }


    /**
     * 上传文件
     *
     * @param file     上传的文件对象
     * @param key      上传文件保存的文件名
     * @param token    上传凭证
     * @param completionHandler 上传完成的后续处理动作
     * @param options  上传数据的可选参数
     */
    public void put(final File file,
                    final String key,
                    final String token,
                    final UpCompletionHandler completionHandler,
                    final UploadOptions options) {
        if (checkAndNotifyError(key, token, file, completionHandler)){
            return;
        }
        putFile(file, key, token, options, completionHandler);
    }

    /**
     * 同步上传文件。使用 form 表单方式上传，建议只在数据较小情况下使用此方式，如 file.size() < 1024 * 1024。
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

        if (!checkAndNotifyError(key, token, data, completionHandler)){
            putData(data, null, key, token, options, completionHandler);
        }

        wait.startWait();

        if (responseInfos.size() > 0){
            return responseInfos.get(0);
        } else {
            return null;
        }
    }

    /**
     * 同步上传文件。使用 form 表单方式上传，建议只在文件较小情况下使用此方式，如 file.size() < 1024 * 1024。
     *
     * @param file    上传的文件对象
     * @param key     上传数据保存的文件名
     * @param token   上传凭证
     * @param options 上传数据的可选参数
     * @return 响应信息 ResponseInfo#response 响应体，序列化后 json 格式
     */
    public ResponseInfo syncPut(File file,
                                String key,
                                String token,
                                UploadOptions options) {

        final ArrayList<ResponseInfo> responseInfos = new ArrayList<ResponseInfo>();
        UpCompletionHandler completionHandler = new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                if (info != null) {
                    responseInfos.add(info);
                }
            }
        };
        if (checkAndNotifyError(key, token, file, completionHandler)){
            return responseInfos.size() > 0 ? responseInfos.get(0) : null;
        }

        byte[] data = null;
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
            data = new byte[(int)file.length()];
            randomAccessFile.read(data, 0, (int)file.length());
        } catch (FileNotFoundException e) {
            return ResponseInfo.fileError(e);
        } catch (IOException e) {
            return ResponseInfo.fileError(e);
        } finally {
            if (randomAccessFile != null){
                try {
                    randomAccessFile.close();
                } catch (IOException ignored) {
                }
            }
        }

        return syncPut(data, key, token, options);
    }

    /**
     * 同步上传文件。使用 form 表单方式上传，建议只在文件较小情况下使用此方式，如 file.size() < 1024 * 1024。
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


    private void putData(final byte[] data,
                         final String fileName,
                         final String key,
                         final String token,
                         final UploadOptions option,
                         final UpCompletionHandler completionHandler){

        final UpToken t = UpToken.parse(token);
        if (t == null) {
            ResponseInfo info = ResponseInfo.invalidToken("invalid token");
            completeAction(token, key, info, null, null, completionHandler);
            return;
        }

        DnsPrefetchTransaction.addDnsCheckAndPrefetchTransaction(config.zone, t);

        BaseUpload.UpTaskCompletionHandler completionHandlerP = new BaseUpload.UpTaskCompletionHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, String key, UploadTaskMetrics requestMetrics, JSONObject response) {
                completeAction(token, key, responseInfo, response, requestMetrics, completionHandler);
            }
        };
        final FormUpload up = new FormUpload(data, key, fileName, t, option, config, completionHandlerP);
        AsyncRun.runInBack(up);
    }

    private void putFile(final File file,
                         final String key,
                         final String token,
                         final UploadOptions option,
                         final UpCompletionHandler completionHandler){

        final UpToken t = UpToken.parse(token);
        if (t == null) {
            ResponseInfo info = ResponseInfo.invalidToken("invalid token");
            completeAction(token, key, info, null, null, completionHandler);
            return;
        }

        DnsPrefetchTransaction.addDnsCheckAndPrefetchTransaction(config.zone, t);

        if (file.length() <= config.putThreshold) {
            ResponseInfo errorInfo = null;
            byte[] data = new byte[(int) file.length()];
            RandomAccessFile randomAccessFile = null;
            try {
                randomAccessFile = new RandomAccessFile(file, "r");
                randomAccessFile.read(data);
            } catch (FileNotFoundException e) {
                errorInfo = ResponseInfo.localIOError("get upload file data error");
            } catch (IOException e) {
                errorInfo = ResponseInfo.localIOError("get upload file data error");
            } finally {
                if (randomAccessFile != null){
                    try {
                        randomAccessFile.close();
                    } catch (IOException ignored){}
                }
            }
            if (errorInfo == null){
                putData(data, file.getName(), key, token, option, completionHandler);
            } else {
                completeAction(token, key, errorInfo, null, null, completionHandler);
            }
            return;
        }

        String recorderKey = key;
        if (config.recorder != null && config.keyGen != null) {
            recorderKey = config.keyGen.gen(key, file);
        }

        BaseUpload.UpTaskCompletionHandler completionHandlerP = new BaseUpload.UpTaskCompletionHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, String key, UploadTaskMetrics requestMetrics, JSONObject response) {
                completeAction(token, key, responseInfo, response, requestMetrics, completionHandler);
            }
        };
        if (config.useConcurrentResumeUpload) {
            final ConcurrentResumeUpload up = new ConcurrentResumeUpload(file, recorderKey, t, option, config, config.recorder, key, completionHandlerP);
            AsyncRun.runInBack(up);
        } else {
            final ResumeUpload up = new ResumeUpload(file, key, t, option, config, config.recorder, recorderKey, completionHandlerP);
            AsyncRun.runInBack(up);
        }
    }

    private boolean checkAndNotifyError(String key,
                                        String token,
                                        Object input,
                                        UpCompletionHandler completionHandler){
        if (completionHandler == null){
            throw new NullPointerException("complete handler is null");
        }

        ResponseInfo responseInfo = null;
        if (input == null){
            responseInfo = ResponseInfo.zeroSize("no input data");
        } else if (input instanceof byte[] && ((byte[])input).length == 0){
            responseInfo = ResponseInfo.zeroSize("no input data");
        } else if (input instanceof File && ((File)input).length() == 0){
            responseInfo = ResponseInfo.zeroSize("file is empty");
        } else if (token == null || token.length() == 0){
            responseInfo = ResponseInfo.invalidToken("no token");
        }
        if (responseInfo != null){
            completeAction(token, key, responseInfo, responseInfo.response, null, completionHandler);
            return true;
        } else {
            return false;
        }
    }

    private void completeAction(final String token,
                                final String key,
                                final ResponseInfo responseInfo,
                                final JSONObject response,
                                final UploadTaskMetrics taskMetrics,
                                final UpCompletionHandler completionHandler){

        reportQuality(responseInfo, taskMetrics, token);
        if (completionHandler != null){
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

    private void reportQuality(ResponseInfo responseInfo,
                               UploadTaskMetrics taskMetrics,
                               String token){

        UploadTaskMetrics taskMetricsP = taskMetrics != null ? taskMetrics : new UploadTaskMetrics(null);

        ReportItem item = new ReportItem();
        item.setReport(ReportItem.LogTypeQuality, ReportItem.QualityKeyLogType);
        item.setReport((Utils.currentTimestamp()/1000), ReportItem.QualityKeyUpTime);
        item.setReport(ReportItem.qualityResult(responseInfo), ReportItem.QualityKeyResult);
        item.setReport(taskMetricsP.totalElapsedTime(), ReportItem.QualityKeyTotalElapsedTime);
        item.setReport(taskMetricsP.requestCount(), ReportItem.QualityKeyRequestsCount);
        item.setReport(taskMetricsP.regionCount(), ReportItem.QualityKeyRegionsCount);
        item.setReport(taskMetricsP.bytesSend(), ReportItem.QualityKeyBytesSent);
        UploadInfoReporter.getInstance().report(item, token);
    }
}
