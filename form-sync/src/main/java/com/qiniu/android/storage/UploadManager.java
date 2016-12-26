package com.qiniu.android.storage;

import com.qiniu.android.http.Client;
import com.qiniu.android.http.ResponseInfo;

import java.io.File;

/**
 * 七牛文件上传管理器
 * 一般默认可以使用这个类的方法来上传数据和文件。
 */
public final class UploadManager {
    private final Configuration config;
    private final Client client;

    public UploadManager() {
        this(new Configuration.Builder().build());
    }

    public UploadManager(Configuration config) {
        this.config = config;
        this.client = new Client(config.connectTimeout, config.connectTimeout, config.dns);
    }


    private static ResponseInfo areInvalidArg(final String key, byte[] data, File f, String token,
                                              UpToken decodedToken) {
        String message = null;
        if (f == null && data == null) {
            message = "no input data";
        } else if (token == null || token.equals("")) {
            message = "no token";
        }
        ResponseInfo info = null;
        if (decodedToken == UpToken.NULL) {
            info = ResponseInfo.invalidToken("invalid token");
        }
        if (message != null) {
            info = ResponseInfo.invalidArgument(message, decodedToken);
        }
        if ((f != null && f.length() == 0) || (data != null && data.length == 0)) {
            info = ResponseInfo.zeroSize(decodedToken);
        }
        return info;
    }

    /**
     * 上传数据
     *
     * @param data    上传的数据
     * @param key     上传数据保存的文件名
     * @param token   上传凭证
     * @param options 上传数据的可选参数
     */
    public ResponseInfo put(final byte[] data, final String key, final String token, final UploadOptions options) {
        return put(data, null, key, token, options);
    }

    /**
     * 上传文件
     *
     * @param filePath 上传的文件路径
     * @param key      上传文件保存的文件名
     * @param token    上传凭证
     * @param options  上传数据的可选参数
     */
    public ResponseInfo put(String filePath, String key, String token, final UploadOptions options) {
        return put(new File(filePath), key, token, options);
    }

    /**
     * 上传文件
     *
     * @param file    上传的文件对象
     * @param key     上传文件保存的文件名
     * @param token   上传凭证
     * @param options 上传数据的可选参数
     */
    public ResponseInfo put(final File file, final String key, String token, final UploadOptions options) {
        return put(null, file, key, token, options);
    }

    private ResponseInfo put(final byte[] data, final File file, final String key, String token, final UploadOptions options) {
        final UpToken decodedToken = UpToken.parse(token);
        ResponseInfo info = areInvalidArg(key, data, file, token, decodedToken);
        if (info != null) {
            return info;
        }
        return FormUploader.upload(client, config, data, file, key, decodedToken, options);
    }
}
