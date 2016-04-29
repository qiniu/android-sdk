package com.qiniu.android.storage;

import com.qiniu.android.http.Client;
import com.qiniu.android.http.CompletionHandler;
import com.qiniu.android.http.ProgressHandler;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.utils.Crc32;
import com.qiniu.android.utils.StringMap;
import com.qiniu.android.utils.StringUtils;
import com.qiniu.android.utils.UrlSafeBase64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;

import static java.lang.String.format;

/**
 * 分片上传
 * 文档：<a href="http://developer.qiniu.com/docs/v6/api/overview/up/chunked-upload.html">分片上传</a>
 * <p/>
 * 分片上传通过将一个文件分割为固定大小的块(4M)，然后再将每个块分割为固定大小的片，每次
 * 上传一个分片的内容，等待所有块的所有分片都上传完成之后，再将这些块拼接起来，构成一个
 * 完整的文件。另外分片上传还支持纪录上传进度，如果本次上传被暂停，那么下次还可以从上次
 * 上次完成的文件偏移位置，继续开始上传，这样就实现了断点续传功能。
 * <p/>
 * 分片上传在网络环境较差的情况下，可以有效地实现大文件的上传。
 */
final class ResumeUploader implements Runnable {

    private final int size;
    private final String key;
    private final UpCompletionHandler completionHandler;
    private final UploadOptions options;
    private final Client client;
    private final Configuration config;
    private final byte[] chunkBuffer;
    private final String[] contexts;
    //    private final Header[] headers;
    private final StringMap headers;
    private final long modifyTime;
    private final String recorderKey;
    private RandomAccessFile file;
    private File f;
    private long crc32;
    private UpToken token;

    ResumeUploader(Client client, Configuration config, File f, String key, UpToken token,
                   final UpCompletionHandler completionHandler, UploadOptions options, String recorderKey) {
        this.client = client;
        this.config = config;
        this.f = f;
        this.recorderKey = recorderKey;
        this.size = (int) f.length();
        this.key = key;
        this.headers = new StringMap().put("Authorization", "UpToken " + token.token);
        this.file = null;
        this.completionHandler = new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                if (file != null) {
                    try {
                        file.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                completionHandler.complete(key, info, response);
            }
        };
        this.options = options != null ? options : UploadOptions.defaultOptions();
        chunkBuffer = new byte[config.chunkSize];
        long count = (size + Configuration.BLOCK_SIZE - 1) / Configuration.BLOCK_SIZE;
        contexts = new String[(int) count];
        modifyTime = f.lastModified();
        this.token = token;
    }

    private static boolean isChunkOK(ResponseInfo info, JSONObject response) {
        return info.statusCode == 200 && info.error == null && (info.hasReqId() || isChunkResOK(response));
    }

    private static boolean isChunkResOK(JSONObject response) {
        try {
            // getXxxx 若获取不到值,会抛出异常
            response.getString("ctx");
            response.getLong("crc32");
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private static boolean isNotChunkToQiniu(ResponseInfo info, JSONObject response) {
        return info.statusCode < 500 && info.statusCode >= 200 && (!info.hasReqId() && !isChunkResOK(response));
    }

    public void run() {
        int offset = recoveryFromRecord();
        try {
            file = new RandomAccessFile(f, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            completionHandler.complete(key, ResponseInfo.fileError(e), null);
            return;
        }
        nextTask(offset, 0, config.up.address);
    }

    /**
     * 创建块，并上传第一个分片内容
     *
     * @param address            上传主机
     * @param offset             本地文件偏移量
     * @param blockSize          分块的块大小
     * @param chunkSize          分片的片大小
     * @param progress           上传进度
     * @param _completionHandler 上传完成处理动作
     */
    private void makeBlock(URI address, int offset, int blockSize, int chunkSize, ProgressHandler progress,
                           CompletionHandler _completionHandler, UpCancellationSignal c) {
        String path = format(Locale.ENGLISH, "/mkblk/%d", blockSize);
        try {
            file.seek(offset);
            file.read(chunkBuffer, 0, chunkSize);
        } catch (IOException e) {
            completionHandler.complete(key, ResponseInfo.fileError(e), null);
            return;
        }
        this.crc32 = Crc32.bytes(chunkBuffer, 0, chunkSize);
        URI u = newURI(address, path);
        post(u, chunkBuffer, 0, chunkSize, progress, _completionHandler, c);
    }

    private void putChunk(URI address, int offset, int chunkSize, String context, ProgressHandler progress,
                          CompletionHandler _completionHandler, UpCancellationSignal c) {
        int chunkOffset = offset % Configuration.BLOCK_SIZE;
        String path = format(Locale.ENGLISH, "/bput/%s/%d", context, chunkOffset);
        try {
            file.seek(offset);
            file.read(chunkBuffer, 0, chunkSize);
        } catch (IOException e) {
            completionHandler.complete(key, ResponseInfo.fileError(e), null);
            return;
        }
        this.crc32 = Crc32.bytes(chunkBuffer, 0, chunkSize);
        URI u = newURI(address, path);
        post(u, chunkBuffer, 0, chunkSize, progress, _completionHandler, c);
    }

    private void makeFile(URI uri, CompletionHandler _completionHandler, UpCancellationSignal c) {
        String mime = format(Locale.ENGLISH, "/mimeType/%s/fname/%s",
                UrlSafeBase64.encodeToString(options.mimeType), UrlSafeBase64.encodeToString(f.getName()));

        String keyStr = "";
        if (key != null) {
            keyStr = format("/key/%s", UrlSafeBase64.encodeToString(key));
        }

        String paramStr = "";
        if (options.params.size() != 0) {
            String str[] = new String[options.params.size()];
            int j = 0;
            for (Map.Entry<String, String> i : options.params.entrySet()) {
                str[j++] = format(Locale.ENGLISH, "%s/%s", i.getKey(), UrlSafeBase64.encodeToString(i.getValue()));
            }
            paramStr = "/" + StringUtils.join(str, "/");
        }
        String path = format(Locale.ENGLISH, "/mkfile/%d%s%s%s", size, mime, keyStr, paramStr);
        URI address = uri;
        try {
            address = new URI(uri.getScheme(), uri.getHost(), path, null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }


        String bodyStr = StringUtils.join(contexts, ",");
        byte[] data = bodyStr.getBytes();

        post(address, data, 0, data.length, null, _completionHandler, c);
    }

    private void post(URI uri, byte[] data, int offset, int size, ProgressHandler progress,
                      CompletionHandler completion, UpCancellationSignal c) {
        client.asyncPost(uri.toString(), data, offset, size, headers, progress, completion, c);
    }

    private int calcPutSize(int offset) {
        int left = size - offset;
        return left < config.chunkSize ? left : config.chunkSize;
    }

    private int calcBlockSize(int offset) {
        int left = size - offset;
        return left < Configuration.BLOCK_SIZE ? left : Configuration.BLOCK_SIZE;
    }

    private boolean isCancelled() {
        return options.cancellationSignal.isCancelled();
    }

    private void nextTask(final int offset, final int retried, final URI address) {
        if (isCancelled()) {
            ResponseInfo i = ResponseInfo.cancelled();
            completionHandler.complete(key, i, null);
            return;
        }

        if (offset == size) {
            //完成操作,返回的内容不确定,是否真正成功逻辑让用户自己判断
            CompletionHandler complete = new CompletionHandler() {
                @Override
                public void complete(ResponseInfo info, JSONObject response) {
                    if (info.isOK()) {
                        removeRecord();
                        options.progressHandler.progress(key, 1.0);
                        completionHandler.complete(key, info, response);
                        return;
                    }

                    if ((info.isNotQiniu() && !token.hasReturnUrl() || info.needRetry()) && retried < config.retryMax) {
                        nextTask(offset, retried + 1, config.upBackup.address);
                        return;
                    }
                    completionHandler.complete(key, info, response);
                }
            };
            makeFile(address, complete, options.cancellationSignal);
            return;
        }

        final int chunkSize = calcPutSize(offset);
        ProgressHandler progress = new ProgressHandler() {
            @Override
            public void onProgress(int bytesWritten, int totalSize) {
                double percent = (double) (offset + bytesWritten) / size;
                if (percent > 0.95) {
                    percent = 0.95;
                }
                options.progressHandler.progress(key, percent);
            }
        };

        // 分片上传,七牛响应内容固定,若缺少reqId,可通过响应体判断
        CompletionHandler complete = new CompletionHandler() {
            @Override
            public void complete(ResponseInfo info, JSONObject response) {
                if (!isChunkOK(info, response)) {
                    if (info.statusCode == 701 && retried < config.retryMax) {
                        nextTask((offset / Configuration.BLOCK_SIZE) * Configuration.BLOCK_SIZE, retried + 1, address);
                        return;
                    }

                    if ((isNotChunkToQiniu(info, response) || info.needRetry()) && retried < config.retryMax) {
                        nextTask(offset, retried + 1, config.upBackup.address);
                        return;
                    }

                    completionHandler.complete(key, info, response);
                    return;
                }
                String context = null;

                if (response == null && retried < config.retryMax) {
                    nextTask(offset, retried + 1, config.upBackup.address);
                    return;
                }
                long crc = 0;
                try {
                    context = response.getString("ctx");
                    crc = response.getLong("crc32");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if ((context == null || crc != ResumeUploader.this.crc32) && retried < config.retryMax) {
                    nextTask(offset, retried + 1, config.upBackup.address);
                    return;
                }
                contexts[offset / Configuration.BLOCK_SIZE] = context;
                record(offset + chunkSize);
                nextTask(offset + chunkSize, retried, address);
            }
        };
        if (offset % Configuration.BLOCK_SIZE == 0) {
            int blockSize = calcBlockSize(offset);
            makeBlock(address, offset, blockSize, chunkSize, progress, complete, options.cancellationSignal);
            return;
        }
        String context = contexts[offset / Configuration.BLOCK_SIZE];
        putChunk(address, offset, chunkSize, context, progress, complete, options.cancellationSignal);
    }

    private int recoveryFromRecord() {
        if (config.recorder == null) {
            return 0;
        }
        byte[] data = config.recorder.get(recorderKey);
        if (data == null) {
            return 0;
        }
        String jsonStr = new String(data);
        JSONObject obj;
        try {
            obj = new JSONObject(jsonStr);
        } catch (JSONException e) {
            e.printStackTrace();
            return 0;
        }
        int offset = obj.optInt("offset", 0);
        long modify = obj.optLong("modify_time", 0);
        int fSize = obj.optInt("size", 0);
        JSONArray array = obj.optJSONArray("contexts");
        if (offset == 0 || modify != modifyTime || fSize != size || array == null || array.length() == 0) {
            return 0;
        }
        for (int i = 0; i < array.length(); i++) {
            contexts[i] = array.optString(i);
        }

        return offset;
    }

    private void removeRecord() {
        if (config.recorder != null) {
            config.recorder.del(recorderKey);
        }
    }

    // save json value
    //{
    //    "size":filesize,
    //    "offset":lastSuccessOffset,
    //    "modify_time": lastFileModifyTime,
    //    "contexts": contexts
    //}
    private void record(int offset) {
        if (config.recorder == null || offset == 0) {
            return;
        }
        String data = format(Locale.ENGLISH, "{\"size\":%d,\"offset\":%d, \"modify_time\":%d, \"contexts\":[%s]}",
                size, offset, modifyTime, StringUtils.jsonJoin(contexts));
        config.recorder.set(recorderKey, data.getBytes());
    }


    private URI newURI(URI uri, String path) {
        try {
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), path, null, null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return uri;
    }
}
