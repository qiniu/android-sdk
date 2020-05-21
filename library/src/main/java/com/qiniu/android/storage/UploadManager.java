package com.qiniu.android.storage;

import com.qiniu.android.common.Zone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.newHttp.metrics.UploadTaskMetrics;
import com.qiniu.android.utils.AsyncRun;

import junit.framework.Assert;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class UploadManager {

    private final Configuration config;

    /**
     * default 3 Threads
     */
    public UploadManager() {
        this(new Configuration.Builder().build());
    }

    /**
     * @param config Configuration, default 1 Thread
     */
    public UploadManager(Configuration config) {
        this.config = config;
    }

    public UploadManager(Recorder recorder) {
        this(recorder, null);
    }

    public UploadManager(Recorder recorder, KeyGenerator keyGen) {
        this(new Configuration.Builder().recorder(recorder, keyGen).build());
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
        putData(data, null, key, token, true, options, complete);
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
        put(new File(filePath), key, token, completionHandler, options);
    }


    /**
     * 上传文件
     *
     * @param file     上传的文件对象
     * @param key      上传文件保存的文件名
     * @param token    上传凭证
     * @param complete 上传完成的后续处理动作
     * @param options  上传数据的可选参数
     */
    public void put(final File file,
                    final String key,
                    final String token,
                    final UpCompletionHandler complete,
                    final UploadOptions options) {
        if (checkAndNotifyError(key, token, file, complete)){
            return;
        }
        putFile(file, key, token, options, complete);
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
        final ArrayList<ResponseInfo> responseInfos = new ArrayList<ResponseInfo>();
        UpCompletionHandler completionHandler = new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                if (info != null) {
                    responseInfos.add(info);
                }
            }
        };
        if (!checkAndNotifyError(key, token, data, completionHandler)){
            putData(data, null, key, token, false, options, completionHandler);
        }

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
        if (!checkAndNotifyError(key, token, file, completionHandler)){
            putFile(file, key, token, options, completionHandler);
        }

        if (responseInfos.size() > 0){
            return responseInfos.get(0);
        } else {
            return null;
        }
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
                         boolean isAsyn,
                         final UploadOptions option,
                         final UpCompletionHandler completionHandler){

        final UpToken t = UpToken.parse(token);
        if (t == null) {
            ResponseInfo info = ResponseInfo.invalidArgument("invalid token");
            completeAction(token, key, info, null, null, completionHandler);
            return;
        }

        config.zone.preQuery(t, new Zone.QueryHandler() {
            @Override
            public void complete(int code, ResponseInfo responseInfo) {
                if (code != 0){
                    completeAction(token, key, responseInfo, responseInfo.response, null, completionHandler);
                    return;
                }
                BaseUpload.UpTaskCompletionHandler completionHandlerP = new BaseUpload.UpTaskCompletionHandler() {
                    @Override
                    public void complete(ResponseInfo responseInfo, String key, UploadTaskMetrics requestMetrics, JSONObject response) {
                        completeAction(token, key, responseInfo, response, requestMetrics, completionHandler);
                    }
                };
                final FormUpload up = new FormUpload(data, key, fileName, t, option, config, completionHandlerP);
                AsyncRun.runInMain(new Runnable() {
                    @Override
                    public void run() {
                        up.run();
                    }
                });
            }
        });
    }

    private void putFile(final File file,
                         final String key,
                         final String token,
                         final UploadOptions option,
                         final UpCompletionHandler completionHandler){
        final UpToken t = UpToken.parse(token);
        if (t == null) {
            ResponseInfo info = ResponseInfo.invalidArgument("invalid token");
            completeAction(token, key, info, null, null, completionHandler);
            return;
        }
        config.zone.preQuery(t, new Zone.QueryHandler() {
            @Override
            public void complete(int code, ResponseInfo responseInfo) {
                if (code != 0) {
                    completeAction(token, key, responseInfo, responseInfo.response, null, completionHandler);
                    return;
                }

                if (file.length() <= config.putThreshold) {
                    byte[] data = new byte[(int) file.length()];
                    RandomAccessFile randomAccessFile = null;
                    try {
                        randomAccessFile = new RandomAccessFile(file, "r");
                        randomAccessFile.seek(0);
                        randomAccessFile.read(data);
                        randomAccessFile.close();
                    } catch (FileNotFoundException ignored) {
                    } catch (IOException e) {
                    }
                    putData(data, file.getName(), key, token, true, option, completionHandler);
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
                    final ConcurrentResumeUpload up = new ConcurrentResumeUpload(file, key, t, option, config, config.recorder, key, completionHandlerP);
                    AsyncRun.runInMain(new Runnable() {
                        @Override
                        public void run() {
                            up.run();
                        }
                    });
                } else {
                    final ResumeUpload up = new ResumeUpload(file, key, t, option, config, config.recorder, key, completionHandlerP);
                    AsyncRun.runInMain(new Runnable() {
                        @Override
                        public void run() {
                            up.run();
                        }
                    });
                }
            }
        });
    }

    private boolean checkAndNotifyError(String key,
                                        String token,
                                        Object input,
                                        UpCompletionHandler completionHandler){
        if (completionHandler == null){
            Assert.assertNotNull("complete handler is null", null);
            return true;
        }

        String desc = null;
        if (input == null){
            desc = "no input data";
        } else if (token == null || token.length() == 0){
            desc = "no token";
        }
        if (desc != null){
            ResponseInfo info = ResponseInfo.invalidArgument(desc);
            completeAction(token, key, info, null, null, completionHandler);
            return true;
        } else {
            return false;
        }
    }

    private void completeAction(String token,
                                String key,
                                ResponseInfo responseInfo,
                                JSONObject response,
                                UploadTaskMetrics taskMetrics,
                                UpCompletionHandler completionHandler){

        reportQuality(responseInfo, taskMetrics, token);
        if (completionHandler != null){
            completionHandler.complete(key, responseInfo, response);
        }
    }

    private void reportQuality(ResponseInfo responseInfo,
                               UploadTaskMetrics taskMetrics,
                               String token){

    }
}
