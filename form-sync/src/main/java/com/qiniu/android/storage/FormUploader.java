package com.qiniu.android.storage;

import com.qiniu.android.http.Client;
import com.qiniu.android.http.PostArgs;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.utils.AndroidNetwork;
import com.qiniu.android.utils.Crc32;
import com.qiniu.android.utils.StringMap;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Created by Simon on 26/12/2016.
 */

public class FormUploader {

    public static ResponseInfo upload(Client client, Configuration config, byte[] data, File file,
                                      String key, UpToken token, UploadOptions optionsIn) {
        try {
            return upload0(client, config, data, file, key, token, optionsIn);
        } catch (Exception e) {
            return ResponseInfo.create(null, ResponseInfo.NetworkError, "", "", "", "",
                    "", "", 80, 0, 0, e.getMessage(), token);
        }
    }

    private static ResponseInfo upload0(Client client, Configuration config, byte[] data, File file,
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


        args.data = data;
        args.file = file;
        args.mimeType = options.mimeType;
        args.params = params;

        ResponseInfo info = client.syncMultipartPost(config.zone.upHost(token.token).address.toString(), args, token);

        if (info.isOK()) {
            return info;
        }

        if (info.needRetry() || (info.isNotQiniu() && !token.hasReturnUrl())) {
            if (info.isNetworkBroken() && !AndroidNetwork.isNetWorkReady()) {
                options.netReadyHandler.waitReady();
                if (!AndroidNetwork.isNetWorkReady()) {
                    return info;
                }
            }

            URI u = config.zone.upHost(token.token).address;
            if (config.zone.upHostBackup(token.token) != null
                    && (info.needSwitchServer() || info.isNotQiniu())) {
                u = config.zone.upHostBackup(token.token).address;
            }

            return client.syncMultipartPost(u.toString(), args, token);
        }

        return info;
    }

}
