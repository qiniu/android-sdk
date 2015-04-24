package com.qiniu.android.storage;

import com.qiniu.android.common.Config;
import com.qiniu.android.http.HttpManager;
import com.qiniu.android.http.Proxy;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.StatReport;
import com.qiniu.android.utils.AsyncRun;

import java.io.File;

/**
 * 七牛文件上传管理器
 *
 * 一般默认可以使用这个类的方法来上传数据和文件。这个类自动检测文件的大小，
 * 只要超过了{@link com.qiniu.android.common.Config#PUT_THRESHOLD}
 */
public final class UploadManager {
    private final Recorder recorder;
    private final HttpManager httpManager;
    private final KeyGenerator keyGen;

    public UploadManager() {
        this(null, null, null);
    }

    public UploadManager(Recorder recorder, KeyGenerator keyGen) {
        this(recorder, keyGen, null);
    }

    /**
     * @param recorder 本地持久化断点上传纪录的类
     * @param keyGen   本地持久化断点上传纪录时需要的key生成器
     * @param proxy    http 代理
     */
    public UploadManager(Recorder recorder, KeyGenerator keyGen, Proxy proxy) {
        this.recorder = recorder;
        this.httpManager = new HttpManager(proxy, new StatReport(), Config.UP_IP_BACKUP);
        this.keyGen = getKeyGen(keyGen);
    }

    public UploadManager(Recorder recorder) {
        this(recorder, null, null);
    }

    private KeyGenerator getKeyGen(KeyGenerator keyGen) {
        if(keyGen == null) {
            keyGen = new KeyGenerator() {
                @Override
                public String gen(String key, File file) {
                    return key + "_._" + new StringBuffer(file.getAbsolutePath()).reverse();
                }
            };
        }
        return keyGen;
    }

    private static boolean areInvalidArg(final String key, byte[] data, File f,
                                         String token, final UpCompletionHandler completionHandler) {
        if (completionHandler == null) {
            throw new IllegalArgumentException("no UpCompletionHandler");
        }
        String message = null;
        if (f == null && data == null) {
            message = "no input data";
        } else if (token == null || token.equals("")) {
            message = "no token";
        }
        if (message != null) {
            final ResponseInfo info = ResponseInfo.invalidArgument(message);
            AsyncRun.run(new Runnable() {
                @Override
                public void run() {
                    completionHandler.complete(key, info, null);
                }
            });
            return true;
        }
        return false;
    }

    /**
     * 上传数据
     *
     * @param data              上传的数据
     * @param key               上传数据保存的文件名
     * @param token             上传凭证
     * @param completionHandler 上传完成后续处理动作
     * @param options           上传数据的可选参数
     */
    public void put(final byte[] data, final String key, final String token,
                    final UpCompletionHandler completionHandler, final UploadOptions options) {
        if (areInvalidArg(key, data, null, token, completionHandler)) {
            return;
        }
        AsyncRun.run(new Runnable() {
            @Override
            public void run() {
                FormUploader.upload(httpManager, data, key, token, completionHandler, options);
            }
        });
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
    public void put(String filePath, String key, String token, UpCompletionHandler completionHandler,
                    final UploadOptions options) {
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
    public void put(File file, String key, String token, UpCompletionHandler completionHandler,
                    final UploadOptions options) {
        if (areInvalidArg(key, null, file, token, completionHandler)) {
            return;
        }
        long size = file.length();
        if (size <= Config.PUT_THRESHOLD) {
            FormUploader.upload(httpManager, file, key, token, completionHandler, options);
            return;
        }
        String recorderKey =  keyGen.gen(key, file);
        ResumeUploader uploader = new ResumeUploader(httpManager, recorder, file, key,
                                            token, completionHandler, options, recorderKey);

        AsyncRun.run(uploader);
    }
}
