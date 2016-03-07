package com.qiniu.android.storage;

import com.qiniu.android.http.Client;
import com.qiniu.android.http.CompletionHandler;
import com.qiniu.android.http.PostArgs;
import com.qiniu.android.http.ProgressHandler;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.utils.Crc32;
import com.qiniu.android.utils.StringMap;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * 表单上传
 * <p/>
 * 通过表单，你可以将一个图片或者一个文本文件等上传到七牛服务器。这个表单
 * 就是标准的http表单，即<code>enctype="multipart/form-data"</code>
 * 格式的表单。
 */
final class FormUploader {

    /**
     * 上传数据，并以指定的key保存文件
     *
     * @param httpManager       HTTP连接管理器
     * @param data              上传的数据
     * @param key               上传的数据保存的文件名
     * @param token             上传凭证
     * @param completionHandler 上传完成后续处理动作
     * @param options           上传时的可选参数
     */
    static void upload(Client httpManager, Configuration config, byte[] data, String key, UpToken token, final UpCompletionHandler completionHandler,
                       final UploadOptions options) {
        post(data, null, key, token, completionHandler, options, httpManager, config);
    }

    /**
     * 上传文件，并以指定的key保存文件
     *
     * @param client            HTTP连接管理器
     * @param file              上传的文件
     * @param key               上传的数据保存的文件名
     * @param token             上传凭证
     * @param completionHandler 上传完成后续处理动作
     * @param options           上传时的可选参数
     */
    static void upload(Client client, Configuration config, File file, String key, UpToken token, UpCompletionHandler completionHandler,
                       UploadOptions options) {
        post(null, file, key, token, completionHandler, options, client, config);
    }

    private static void post(byte[] data, File file, String k, final UpToken token, final UpCompletionHandler completionHandler,
                             final UploadOptions optionsIn, final Client client, final Configuration config) {
        final String key = k;
        StringMap params = new StringMap();
        final PostArgs args = new PostArgs();
        if (k != null) {
            params.put("key", key);
            args.fileName = key;
        } else {
            args.fileName = "?";
        }

        // data is null , or file is null
        if (file != null) {
            args.fileName = file.getName();
        }

        params.put("token", token.token);

        final UploadOptions options = optionsIn != null ? optionsIn : UploadOptions.defaultOptions();
        params.putFileds(options.params);

        if (options.checkCrc) {
            long crc = 0;
            if (file != null) {
                try {
                    crc = Crc32.file(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                crc = Crc32.bytes(data);
            }
            params.put("crc32", "" + crc);
        }

        final ProgressHandler progress = new ProgressHandler() {
            @Override
            public void onProgress(int bytesWritten, int totalSize) {
                double percent = (double) bytesWritten / (double) totalSize;
                if (percent > 0.95) {
                    percent = 0.95;
                }
                options.progressHandler.progress(key, percent);
            }
        };

        args.data = data;
        args.file = file;
        args.mimeType = options.mimeType;
        args.params = params;

        CompletionHandler completion = new CompletionHandler() {
            @Override
            public void complete(ResponseInfo info, JSONObject response) {
                if (info.isOK()) {
                    options.progressHandler.progress(key, 1.0);
                    completionHandler.complete(key, info, response);
                } else if (options.cancellationSignal.isCancelled()) {
                    ResponseInfo i = ResponseInfo.cancelled();
                    completionHandler.complete(key, i, null);
                } else if (info.needRetry() || (info.isNotQiniu() && !token.hasReturnUrl())) {
                    CompletionHandler retried = new CompletionHandler() {
                        @Override
                        public void complete(ResponseInfo info, JSONObject response) {
                            if (info.isOK()) {
                                options.progressHandler.progress(key, 1.0);
                            }
                            completionHandler.complete(key, info, response);
                        }
                    };
                    URI u = config.up.address;
                    if (info.needSwitchServer() || info.isNotQiniu()) {
                        u = config.upBackup.address;
                    }

                    client.asyncMultipartPost(u.toString(), args, progress, retried, options.cancellationSignal);
                } else {
                    completionHandler.complete(key, info, response);
                }
            }
        };

        client.asyncMultipartPost(config.up.address.toString(), args, progress, completion, options.cancellationSignal);
    }
}
