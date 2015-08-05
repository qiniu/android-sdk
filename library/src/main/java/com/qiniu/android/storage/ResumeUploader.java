package com.qiniu.android.storage;

import com.qiniu.android.http.CompletionHandler;
import com.qiniu.android.http.HttpManager;
import com.qiniu.android.http.ProgressHandler;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.utils.Crc32;
import com.qiniu.android.utils.StringUtils;
import com.qiniu.android.utils.UrlSafeBase64;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
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
    private final HttpManager httpManager;
    private final Configuration config;
    private final byte[] chunkBuffer;
    private final String[] contexts;
    private final Header[] headers;
    private final long modifyTime;
    private final String recorderKey;
    private volatile RandomAccessFile file = null;
    private File f;
    private long crc32;
    private UpToken token;
    private boolean forceIp = false;

    ResumeUploader(HttpManager httpManager, Configuration config, final File f, String key, UpToken token,
                   final UpCompletionHandler completionHandler, UploadOptions options, String recorderKey) {
        this.httpManager = httpManager;
        this.config = config;
        this.f = f;
        this.recorderKey = recorderKey;
        this.size = (int) f.length();
        this.key = key;
        this.headers = new Header[1];
        headers[0] = new BasicHeader("Authorization", "UpToken " + token.token);
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

    public void run() {
        int offset = recoveryFromRecord();
        try {
            file = new RandomAccessFile(f, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            completionHandler.complete(key, ResponseInfo.fileError(e), null);
            return;
        }
        nextTask(offset, 0, config.upHost);
    }

    /**
     * 创建块，并上传第一个分片内容
     *
     * @param host               上传主机
     * @param offset             本地文件偏移量
     * @param blockSize          分块的块大小
     * @param chunkSize          分片的片大小
     * @param progress           上传进度
     * @param _completionHandler 上传完成处理动作
     */
    private void makeBlock(String host, int offset, int blockSize, int chunkSize, ProgressHandler progress,
                           CompletionHandler _completionHandler, UpCancellationSignal c) {
        String url = format(Locale.ENGLISH, "http://%s/mkblk/%d", host, blockSize);
        try {
            file.seek(offset);
            file.read(chunkBuffer, 0, chunkSize);
        } catch (IOException e) {
            completionHandler.complete(key, ResponseInfo.fileError(e), null);
            return;
        }
        this.crc32 = Crc32.bytes(chunkBuffer, 0, chunkSize);

        post(url, chunkBuffer, 0, chunkSize, progress, _completionHandler, c);
    }

    private void putChunk(String host, int offset, int chunkSize, String context, ProgressHandler progress,
                          CompletionHandler _completionHandler, UpCancellationSignal c) {
        int chunkOffset = offset % Configuration.BLOCK_SIZE;
        String url = format(Locale.ENGLISH, "http://%s:%d/bput/%s/%d", host, config.upPort, context, chunkOffset);
        try {
            file.seek(offset);
            file.read(chunkBuffer, 0, chunkSize);
        } catch (IOException e) {
            completionHandler.complete(key, ResponseInfo.fileError(e), null);
            return;
        }
        this.crc32 = Crc32.bytes(chunkBuffer, 0, chunkSize);
        post(url, chunkBuffer, 0, chunkSize, progress, _completionHandler, c);
    }

    private void makeFile(String host, CompletionHandler _completionHandler, UpCancellationSignal c) {
        String mime = format(Locale.ENGLISH, "/mimeType/%s", UrlSafeBase64.encodeToString(options.mimeType));

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
        String url = format(Locale.ENGLISH, "http://%s/mkfile/%d%s%s%s", host, size, mime, keyStr, paramStr);
        String bodyStr = StringUtils.join(contexts, ",");
        byte[] data = bodyStr.getBytes();

        post(url, data, 0, data.length, null, _completionHandler, c);
    }

    private void post(String url, byte[] data, int offset, int size, ProgressHandler progress,
                      CompletionHandler completion, UpCancellationSignal c) {
        httpManager.postData(url, data, offset, size, headers, progress, completion, c, forceIp);
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

    private void nextTask(final int offset, final int retried, final String host) {
        if (offset == size) {
            CompletionHandler complete = new CompletionHandler() {
                @Override
                public void complete(ResponseInfo info, JSONObject response) {
                    if (info.isOK()) {
                        removeRecord();
                        options.progressHandler.progress(key, 1.0);
                        completionHandler.complete(key, info, response);
                        return;
                    }

                    if (isCancelled()) {
                        ResponseInfo i = ResponseInfo.cancelled();
                        completionHandler.complete(key, i, null);
                        return;
                    }

                    if (isNotQiniu(info)) {
                        forceIp = true;
                    }

                    if (isNotQiniu(info) || (info.needRetry() && retried < config.retryMax)) {
                        nextTask(offset, retried + 1, host);
                        return;
                    }
                    completionHandler.complete(key, info, response);
                }
            };
            makeFile(host, complete, options.cancellationSignal);
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

        CompletionHandler complete = new CompletionHandler() {
            @Override
            public void complete(ResponseInfo info, JSONObject response) {
                if (!info.isOK()) {
                    if (isCancelled()) {
                        ResponseInfo i = ResponseInfo.cancelled();
                        completionHandler.complete(key, i, null);
                        return;
                    }
                    if (info.statusCode == 701) {
                        nextTask((offset / Configuration.BLOCK_SIZE) * Configuration.BLOCK_SIZE, retried, host);
                        return;
                    }
                    if (isNotQiniu(info)) {
                        forceIp = true;
                    }
                    if (!isNotQiniu(info) && (retried >= config.retryMax || !info.needRetry())) {
                        completionHandler.complete(key, info, null);
                        return;
                    }
                    String host2 = host;
                    if (info.needSwitchServer()) {
                        host2 = config.upHostBackup;
                    }
                    nextTask(offset, retried + 1, host2);
                    return;
                }
                String context = null;

                if (response == null) {
                    nextTask(offset, retried + 1, host);
                    return;
                }
                long crc = 0;
                try {
                    context = response.getString("ctx");
                    crc = response.getLong("crc32");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (context == null || crc != ResumeUploader.this.crc32) {
                    nextTask(offset, retried + 1, host);
                    return;
                }
                contexts[offset / Configuration.BLOCK_SIZE] = context;
                record(offset + chunkSize);
                nextTask(offset + chunkSize, retried, host);
            }
        };
        if (offset % Configuration.BLOCK_SIZE == 0) {
            int blockSize = calcBlockSize(offset);
            makeBlock(host, offset, blockSize, chunkSize, progress, complete, options.cancellationSignal);
            return;
        }
        String context = contexts[offset / Configuration.BLOCK_SIZE];
        putChunk(host, offset, chunkSize, context, progress, complete, options.cancellationSignal);
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

    private boolean isNotQiniu(ResponseInfo info) {
        return info.isNotQiniu() && !token.hasReturnUrl();
    }
}
