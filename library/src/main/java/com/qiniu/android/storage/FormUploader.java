package com.qiniu.android.storage;

import com.qiniu.android.common.Config;
import com.qiniu.android.http.CompletionHandler;
import com.qiniu.android.http.HttpManager;
import com.qiniu.android.http.PostArgs;
import com.qiniu.android.http.ProgressHandler;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.utils.Crc32;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

final class FormUploader {
    static void upload(HttpManager httpManager, byte[] data, String k, String token, final UpCompletionHandler completionHandler,
                       final UploadOptions options) {
        post(data, null, k, token, completionHandler, options, httpManager);
    }

    static void upload(HttpManager httpManager, File file, String key, String token, UpCompletionHandler completionHandler,
                       UploadOptions options) {
        post(null, file, key, token, completionHandler, options, httpManager);
    }

    private static void post(byte[] data, File file, String k, String token, final UpCompletionHandler completionHandler,
                             final UploadOptions options, final HttpManager httpManager) {
        final String key = k;
        Map<String, String> params = new HashMap<String, String>();
        final PostArgs args = new PostArgs();
        if (k != null) {
            params.put("key", key);
            args.fileName = key;
        } else {
            args.fileName = "?";
        }

        params.put("token", token);
        if (options != null) {
            params.putAll(options.params);
        }

        String mimeType = "application/octet-stream";
        if (options != null && options.mimeType != null && options.mimeType.equals("")) {
            mimeType = options.mimeType;
        }

        if (options != null && options.checkCrc) {
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


        args.data = data;
        args.file = file;
        args.mimeType = mimeType;
        args.params = params;

        ProgressHandler progress = null;
        if (options != null && options.progressHandler != null) {
            progress = new ProgressHandler() {
                @Override
                public void onProgress(int bytesWritten, int totalSize) {
                    double percent = (double) bytesWritten / (double) totalSize;
                    if (percent > 0.95) {
                        percent = 0.95;
                    }
                    options.progressHandler.progress(key, percent);
                }
            };
        }

        final ProgressHandler progress2 = progress;

        CompletionHandler completion = new CompletionHandler() {
            @Override
            public void complete(ResponseInfo info, JSONObject response) {
                if (info.isOK()) {
                    if (progress2 != null) {
                        options.progressHandler.progress(key, 1.0);
                    }
                    completionHandler.complete(key, info, response);
                    return;
                }
                CompletionHandler retried = new CompletionHandler() {
                    @Override
                    public void complete(ResponseInfo info, JSONObject response) {
                        if (info.isOK() && progress2 != null) {
                            options.progressHandler.progress(key, 1.0);
                        }
                        completionHandler.complete(key, info, response);
                    }
                };
                String host = Config.UP_HOST;
                if (info.isNetworkBroken()) {
                    host = Config.UP_HOST_BACKUP;
                }
                httpManager.multipartPost("http://" + host, args, progress2, retried);
            }
        };

        httpManager.multipartPost("http://" + Config.UP_HOST, args, progress, completion);
    }
}
