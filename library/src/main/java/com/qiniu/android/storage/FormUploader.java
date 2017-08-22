package com.qiniu.android.storage;

import android.util.Log;

import com.qiniu.android.http.Client;
import com.qiniu.android.http.CompletionHandler;
import com.qiniu.android.http.PostArgs;
import com.qiniu.android.http.ProgressHandler;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.utils.AndroidNetwork;
import com.qiniu.android.utils.Crc32;
import com.qiniu.android.utils.StringMap;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import static android.R.attr.key;

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
    static void upload(Client client, Configuration config, File file, String key, UpToken token,
                       UpCompletionHandler completionHandler, UploadOptions options) {
        post(null, file, key, token, completionHandler, options, client, config);
    }

    private static void post(byte[] data, File file, String k, final UpToken token,
                             final UpCompletionHandler completionHandler,
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

        final ProgressHandler progress = new ProgressHandler() {
            @Override
            public void onProgress(long bytesWritten, long totalSize) {
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

        final String upHost = config.zone.upHost(token.token, config.useHttps, null);
        Log.d("Qiniu.FormUploader", "upload use up host " + upHost);
        CompletionHandler completion = new CompletionHandler() {
            @Override
            public void complete(ResponseInfo info, JSONObject response) {
                if (info.isNetworkBroken() && !AndroidNetwork.isNetWorkReady()) {
                    options.netReadyHandler.waitReady();
                    if (!AndroidNetwork.isNetWorkReady()) {
                        completionHandler.complete(key, info, response);
                        return;
                    }
                }

                if (info.isOK()) {
                    options.progressHandler.progress(key, 1.0);
                    completionHandler.complete(key, info, response);
                } else if (info.needRetry()) {
                    final String upHostRetry = config.zone.upHost(token.token, config.useHttps, upHost);
                    Log.d("Qiniu.FormUploader", "retry upload first time use up host " + upHostRetry);
                    CompletionHandler retried = new CompletionHandler() {
                        @Override
                        public void complete(ResponseInfo info, JSONObject response) {
                            if (info.isOK()) {
                                options.progressHandler.progress(key, 1.0);
                                completionHandler.complete(key, info, response);
                            } else if (info.needRetry()) {
                                final String upHostRetry2 = config.zone.upHost(token.token, config.useHttps, upHostRetry);
                                Log.d("Qiniu.FormUploader", "retry upload second time use up host " + upHostRetry2);
                                CompletionHandler retried2 = new CompletionHandler() {
                                    @Override
                                    public void complete(ResponseInfo info2, JSONObject response2) {
                                        if (info2.isOK()) {
                                            options.progressHandler.progress(key, 1.0);
                                        } else if (info2.needRetry()) {
                                            config.zone.frozenDomain(upHostRetry2);
                                        }
                                        completionHandler.complete(key, info2, response2);
                                    }
                                };
                                client.asyncMultipartPost(upHostRetry2, args, token, progress, retried2, options.cancellationSignal);
                            } else {
                                completionHandler.complete(key, info, response);
                            }
                        }
                    };
                    client.asyncMultipartPost(upHostRetry, args, token, progress, retried, options.cancellationSignal);
                } else {
                    completionHandler.complete(key, info, response);
                }
            }
        };

        client.asyncMultipartPost(upHost, args, token, progress, completion, options.cancellationSignal);
    }

    /**
     * 上传数据，并以指定的key保存文件
     *
     * @param client  HTTP连接管理器
     * @param data    上传的数据
     * @param key     上传的数据保存的文件名
     * @param token   上传凭证
     * @param options 上传时的可选参数
     * @return 响应信息 ResponseInfo#response 响应体，序列化后 json 格式
     */
    public static ResponseInfo syncUpload(Client client, Configuration config, byte[] data, String key, UpToken token, UploadOptions options) {
        try {
            return syncUpload0(client, config, data, null, key, token, options);
        } catch (Exception e) {

            return ResponseInfo.create(null, ResponseInfo.UnknownError, "", "", "", "", "", "", 0, 0, 0,
                    e.getMessage(), token, data != null ? data.length : 0);
        }
    }

    /**
     * 上传文件，并以指定的key保存文件
     *
     * @param client  HTTP连接管理器
     * @param file    上传的文件
     * @param key     上传的数据保存的文件名
     * @param token   上传凭证
     * @param options 上传时的可选参数
     * @return 响应信息 ResponseInfo#response 响应体，序列化后 json 格式
     */
    public static ResponseInfo syncUpload(Client client, Configuration config, File file, String key, UpToken token, UploadOptions options) {
        try {
            return syncUpload0(client, config, null, file, key, token, options);
        } catch (Exception e) {
            return ResponseInfo.create(null, ResponseInfo.UnknownError, "", "", "", "", "", "", 0, 0, 0,
                    e.getMessage(), token, file != null ? file.length() : 0);
        }
    }

    private static ResponseInfo syncUpload0(Client client, Configuration config, byte[] data, File file,
                                            String key, UpToken token, UploadOptions optionsIn) {
        StringMap params = new StringMap();
        final PostArgs args = new PostArgs();
        if (key != null) {
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

        args.data = data;
        args.file = file;
        args.mimeType = options.mimeType;
        args.params = params;

        boolean success = config.zone.preQuery(token.token);
        if (!success) {
            return ResponseInfo.invalidToken("failed to get up host");
        }


        final String upHost = config.zone.upHost(token.token, config.useHttps, null);
        Log.d("Qiniu.FormUploader", "sync upload use up host " + upHost);
        ResponseInfo info = client.syncMultipartPost(upHost, args, token);

        if (info.isOK()) {
            return info;
        }

        //retry for the first time
        if (info.needRetry()) {
            if (info.isNetworkBroken() && !AndroidNetwork.isNetWorkReady()) {
                options.netReadyHandler.waitReady();
                if (!AndroidNetwork.isNetWorkReady()) {
                    return info;
                }
            }

            //retry for the second time
            String upHostRetry = config.zone.upHost(token.token, config.useHttps, upHost);
            Log.d("Qiniu.FormUploader", "sync upload retry first time use up host " + upHostRetry);
            info = client.syncMultipartPost(upHostRetry, args, token);

            if (info.needRetry()) {
                if (info.isNetworkBroken() && !AndroidNetwork.isNetWorkReady()) {
                    options.netReadyHandler.waitReady();
                    if (!AndroidNetwork.isNetWorkReady()) {
                        return info;
                    }
                }

                String upHostRetry2 = config.zone.upHost(token.token, config.useHttps, upHostRetry);
                Log.d("Qiniu.FormUploader", "sync upload retry second time use up host " + upHostRetry2);
                info = client.syncMultipartPost(upHostRetry2, args, token);
                if (info.needRetry()) {
                    config.zone.frozenDomain(upHostRetry2);
                }
            }
        }

        return info;
    }
}
