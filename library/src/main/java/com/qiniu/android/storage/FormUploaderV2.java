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

/**
 * Created by jemy on 2019/9/25.
 * upload
 * syncUpload
 */

public class FormUploaderV2 {

    Client client;
    Configuration config;
    byte[] data;
    File file;
    String key;
    UpToken token;
    UpCompletionHandler completionHandler;
    UploadOptions upOptions;
    String upHost;
    PostArgs args = null;
    ProgressHandler progress = null;
    int upRetried = 1;
    int needRetried = 0;

    FormUploaderV2(Client client, Configuration config, byte[] data, File file, String key, UpToken
            token, final UpCompletionHandler completionHandler, final UploadOptions options) {
        this.client = client;
        this.config = config;
        this.data = data;
        this.file = file;
        this.key = key;
        this.token = token;
        this.completionHandler = completionHandler;
        this.upOptions = options;

    }


    public void upload() {
        if (data != null || file != null) {
            this.needRetried = config.zone.getZoneInfo(token.token).upDomainsList.size();
            post();
        } else {
            ResponseInfo responseInfo = ResponseInfo.zeroSize(token);
            completionHandler.complete(key, responseInfo, null);
        }

    }

    private CompletionHandler getCompletionHandler(final UploadOptions options) {
        return new CompletionHandler() {
            @Override
            public void complete(ResponseInfo info, JSONObject response) {
                if (info.isOK()) {
                    options.progressHandler.progress(key, 1.0);
                    completionHandler.complete(key, info, response);
                } else if (info.needRetry() && upRetried < needRetried) {
                    upHost = config.zone.upHost(token.token, config.useHttps, upHost);
                    Log.d("Qiniu.FormUploader", "retry upload second time use up host " + upHost);
                    upRetried += 1;
                    startUpload(options, upHost);
                } else {
                    completionHandler.complete(key, info, response);
                }
            }
        };
    }

    private void post() {
        StringMap params = new StringMap();
        args = new PostArgs();
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

        final UploadOptions options = upOptions != null ? upOptions : UploadOptions.defaultOptions();
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

        progress = new ProgressHandler() {
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
        upHost = config.zone.upHost(token.token, config.useHttps, null);
        Log.d("Qiniu.FormUploader", "upload use up host " + upHost);
        startUpload(options, upHost);
    }

    private void startUpload(UploadOptions options, String upHost) {
        client.asyncMultipartPost(upHost, args, token, progress, getCompletionHandler(options), options.cancellationSignal);
    }


    public ResponseInfo syncUpload() {
        if (data != null || file != null) {
            try {
                return syncUploadStart();
            } catch (Exception e) {
                return ResponseInfo.create(null, ResponseInfo.UnknownError, "", "", "", "", "", "", 0, 0, 0,
                        e.getMessage(), token, data != null ? data.length : 0);
            }
        } else {
            return ResponseInfo.zeroSize(token);
        }
    }


    private ResponseInfo syncUploadStart() throws InterruptedException {
        StringMap params = new StringMap();
        args = new PostArgs();
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

        final UploadOptions options = upOptions != null ? upOptions : UploadOptions.defaultOptions();
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
        this.needRetried = config.zone.getZoneInfo(token.token).upDomainsList.size();
        ResponseInfo info = null;
        boolean needRetry = true;
        while ((upRetried <= needRetried) && needRetry) {
            upHost = config.zone.upHost(token.token, config.useHttps, upHost);
            Log.d("Qiniu.FormUploader", "sync upload use up host " + upHost);

            info = client.syncMultipartPost(upHost, args, token);
            upRetried += 1;

            if (info.isOK()) {
                needRetry = false;
                return info;
            }
            if (info.needRetry()) {
                if (info.isNetworkBroken() && !AndroidNetwork.isNetWorkReady()) {
                    options.netReadyHandler.waitReady();
                    if (!AndroidNetwork.isNetWorkReady()) {
                        needRetry = false;
                        return info;
                    }
                }
                continue;
            }
            Thread.sleep(50);
        }
        return info;
    }
}
