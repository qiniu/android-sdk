package com.qiniu.android.storage;

import android.util.Log;

import com.qiniu.android.http.Client;
import com.qiniu.android.http.CompletionHandler;
import com.qiniu.android.http.ProgressHandler;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.utils.AndroidNetwork;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;

/**
 * Created by jemy on 2019/7/9.
 */

public class ResumeUploaderFast implements Runnable {

    /**
     * 文件总大小
     */
    private final long totalSize;

    private final String key;

    private final UpCompletionHandler completionHandler;

    private final UploadOptions options;

    private final Client client;

    private final Configuration config;
    /**
     * 保存每一块上传返回的ctx
     */
    private final String[] contexts;
    /**
     * headers
     */
    private final StringMap headers;
    /**
     * 修改时间
     */
    private final long modifyTime;
    /**
     * 断点续传记录文件
     */
    private final String recorderKey;
    /**
     * 文件对象
     */
    private RandomAccessFile file;
    /**
     * 待上传文件
     */
    private File f;
    /**
     * 上传token
     */
    private UpToken token;
    /**
     * 分块数据
     */
    private Map<Long, Integer> blockInfo;
    /**
     * 上传域名
     */
    AtomicReference upHost = new AtomicReference();
    /**
     * 总块数
     */
    AtomicInteger tblock;
    /**
     * 重传域名数
     */
    AtomicInteger retried = new AtomicInteger(0);
    /**
     * 单域名检测次数
     */
    AtomicInteger singleDomainRetry = new AtomicInteger(0);

    /**
     * 单域名重试次数
     */
    int retryMax = 0;
    /**
     * 线程数量
     */
    private int multithread;

    /**
     * 每块偏移位子
     * use 断点续传
     */
    private Long[] offsets;
    /**
     * 已上传块
     */
    private int upBlock = 0;
    private int domainRetry = 3;
    /**
     * 避免多个任务同时回调
     */
    private boolean isInterrupted = false;

    ResumeUploaderFast(Client client, Configuration config, File f, String key, UpToken token,
                       final UpCompletionHandler completionHandler, UploadOptions options, String recorderKey, int multithread) {
        this.client = client;
        this.config = config;
        this.f = f;
        this.recorderKey = recorderKey;
        this.totalSize = f.length();
        this.key = key;
        this.headers = new StringMap().put("Authorization", "UpToken " + token.token);
        this.file = null;
        this.multithread = multithread;
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
                //第一个回调出来之后通知其他线程停止这个事件的回调
                synchronized (this) {
                    if (!isInterrupted) {
                        isInterrupted = true;
                        completionHandler.complete(key, info, response);
                    } else {
                        return;
                    }
                }
            }
        };
        this.options = options != null ? options : UploadOptions.defaultOptions();
        tblock = new AtomicInteger((int) (totalSize + Configuration.BLOCK_SIZE - 1) / Configuration.BLOCK_SIZE);
        this.offsets = new Long[tblock.get()];
        contexts = new String[tblock.get()];
        modifyTime = f.lastModified();
        this.token = token;
        this.blockInfo = new LinkedHashMap<>();
        domainRetry = config.zone.getZoneInfo(token.token).upDomainsList.size();
        retryMax = multithread > config.retryMax ? multithread : config.retryMax;
    }

    @Override
    public void run() {
        try {
            file = new RandomAccessFile(f, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            completionHandler.complete(key, ResponseInfo.fileError(e, token), null);
            return;
        }
        putBlockInfo();

        upHost.set(config.zone.upHost(token.token, config.useHttps, null));
        if (blockInfo.size() < multithread) {
            multithread = blockInfo.size();
        }
        for (int i = 0; i < multithread; i++) {
            BlockElement mblock = getBlockInfo();
            new UploadThread(mblock.getOffset(), mblock.getBlocksize(), upHost.get().toString()).start();
        }
    }


    /**
     * cut file to blockInfo
     */
    private void putBlockInfo() {
        Long[] offs = recoveryFromRecord();
        int lastBlock = tblock.get() - 1;
        if (offs == null) {
            for (int i = 0; i < lastBlock; i++) {
                blockInfo.put((long) i * Configuration.BLOCK_SIZE, Configuration.BLOCK_SIZE);
            }
            blockInfo.put((long) lastBlock * Configuration.BLOCK_SIZE, (int) (totalSize - lastBlock * Configuration.BLOCK_SIZE));
        } else {
            HashSet<Long> set = new HashSet<Long>(Arrays.asList(offs));
            for (int i = 0; i < lastBlock; i++) {
                Long offset = (long) i * Configuration.BLOCK_SIZE;
                if (!set.contains(offset)) {
                    blockInfo.put(offset, Configuration.BLOCK_SIZE);
                } else {
                    offsets[i] = offset;
                    upBlock += 1;
                }
            }
            Long offset = (long) lastBlock * Configuration.BLOCK_SIZE;
            if (!set.contains(offset)) {
                blockInfo.put(offset, (int) (totalSize - lastBlock * Configuration.BLOCK_SIZE));
            } else {
                offsets[lastBlock] = offset;
                upBlock += 1;
            }
        }
    }

    /**
     * get next block use to upload
     *
     * @return BlockElement
     */
    private synchronized BlockElement getBlockInfo() {
        Iterator<Map.Entry<Long, Integer>> it = blockInfo.entrySet().iterator();
        long offset = 0;
        int blockSize = 0;
        if (it.hasNext()) {
            Map.Entry<Long, Integer> entry = it.next();
            offset = entry.getKey();
            blockSize = entry.getValue();
            blockInfo.remove(offset);
        }
        return new BlockElement(offset, blockSize);
    }

    class UploadThread extends Thread {
        private long offset;
        private int blockSize;
        private String upHost;

        UploadThread(long offset, int blockSize, String upHost) {
            this.offset = offset;
            this.blockSize = blockSize;
            this.upHost = upHost;
        }

        @Override
        public void run() {
            super.run();
            mkblk(offset, blockSize, upHost);
        }
    }

    /**
     * 创建块，并上传内容
     *
     * @param upHost    上传主机
     * @param offset    本地文件偏移量
     * @param blockSize 分块的块大小
     */
    private void mkblk(long offset, int blockSize, String upHost) {
        String path = format(Locale.ENGLISH, "/mkblk/%d", blockSize);
        byte[] chunkBuffer = new byte[blockSize];
        synchronized (this) {
            try {
                //多线程时file.read可能会在seek时被篡改，导致上传buffer紊乱
                file.seek(offset);
                file.read(chunkBuffer, 0, blockSize);
            } catch (IOException e) {
                completionHandler.complete(key, ResponseInfo.fileError(e, token), null);
                return;
            }
        }

        long crc32 = Crc32.bytes(chunkBuffer, 0, blockSize);
        String postUrl = String.format("%s%s", upHost, path);

        post(postUrl, chunkBuffer, 0, blockSize, getProgressHandler(),
                getCompletionHandler(offset, blockSize, crc32), options.cancellationSignal);
    }

    private void makeFile(String upHost, CompletionHandler _completionHandler, UpCancellationSignal c) {
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
        String path = format(Locale.ENGLISH, "/mkfile/%d%s%s%s", totalSize, mime, keyStr, paramStr);
        String bodyStr = StringUtils.join(contexts, ",");
        byte[] data = bodyStr.getBytes();
        String postUrl = String.format("%s%s", upHost, path);
        post(postUrl, data, 0, data.length, null, _completionHandler, c);
    }

    private void post(String upHost, byte[] data, int offset, int dataSize, ProgressHandler progress,
                      CompletionHandler completion, UpCancellationSignal c) {
        client.asyncPost(upHost, data, offset, dataSize, headers, token, totalSize, progress, completion, c);
    }

    private ProgressHandler getProgressHandler() {
        return new ProgressHandler() {
            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                long size = 0;
                for (Long offset : offsets) {
                    if (offset != null && offset > 0) {
                        size += 1;
                    }
                }
                double percent = (double) size * Configuration.BLOCK_SIZE / totalSize;
                if (percent > 0.95) {
                    percent = 0.95;
                }
                options.progressHandler.progress(key, percent);
            }
        };

    }

    private CompletionHandler getMkfileCompletionHandler() {
        return new CompletionHandler() {
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
                    removeRecord();
                    options.progressHandler.progress(key, 1.0);
                    completionHandler.complete(key, info, response);
                    return;
                }

                // mkfile  ，允许多重试一次，这里不需要重试时，成功与否都complete回调给客户端
                if (info.needRetry() && retried.get() < config.retryMax + 1) {
                    makeFile(upHost.get().toString(), getMkfileCompletionHandler(), options.cancellationSignal);
                    retried.addAndGet(1);
                    return;
                }
                completionHandler.complete(key, info, response);
            }
        };
    }


    private CompletionHandler getCompletionHandler(final long offset, final int blockSize, final long crc32) {
        return new CompletionHandler() {
            @Override
            public void complete(ResponseInfo info, JSONObject response) {
                //网络断开或者无状态，检查3s，直接返回
                if (info.isNetworkBroken() && !AndroidNetwork.isNetWorkReady()) {
                    options.netReadyHandler.waitReady();
                    if (!AndroidNetwork.isNetWorkReady()) {
                        completionHandler.complete(key, info, response);
                        return;
                    }
                }

                //取消
                if (info.isCancelled()) {
                    completionHandler.complete(key, info, response);
                    return;
                }

                //上传失败：重试
                //701: ctx不正确或者已经过期
                //checkRetried之后应该立即updateRetried，否则每个线程进来checkRetried判定都是跟第一个一样
                if (!isChunkOK(info, response)) {
                    if (info.statusCode == 701 && checkRetried()) {
                        updateRetried();
                        mkblk(offset, blockSize, upHost.get().toString());
                        return;
                    }
                    if (upHost != null && ((isNotChunkToQiniu(info, response) || info.needRetry())
                            && checkRetried())) {
                        updateRetried();
                        mkblk(offset, blockSize, upHost.get().toString());
                        return;
                    }

                    completionHandler.complete(key, info, response);
                    return;
                }

                //上传成功(伪成功)：检查response
                String context = null;
                if (response == null && checkRetried()) {
                    updateRetried();
                    mkblk(offset, blockSize, upHost.get().toString());
                    return;
                }
                long crc = 0;
                Exception tempE = null;
                try {
                    context = response.getString("ctx");
                    crc = response.getLong("crc32");
                } catch (Exception e) {
                    tempE = e;
                    e.printStackTrace();
                }
                if ((context == null || crc != crc32) && checkRetried()) {
                    updateRetried();
                    mkblk(offset, blockSize, upHost.get().toString());
                    return;
                }
                if (context == null) {
                    String error = "get context failed.";
                    if (tempE != null) {
                        error += "\n";
                        error += tempE.getMessage();
                    }
                    ResponseInfo info2 = ResponseInfo.errorInfo(info, ResponseInfo.UnknownError, error);
                    completionHandler.complete(key, info2, response);
                    return;
                }
                if (crc != crc32) {
                    String error = "block's crc32 is not match. local: " + crc32 + ", remote: " + crc;
                    ResponseInfo info2 = ResponseInfo.errorInfo(info, ResponseInfo.Crc32NotMatch, error);
                    completionHandler.complete(key, info2, response);
                    return;
                }

                synchronized (this) {
                    contexts[(int) (offset / Configuration.BLOCK_SIZE)] = context;
                    offsets[(int) (offset / Configuration.BLOCK_SIZE)] = offset;
                    record(offsets);
                    upBlock += 1;
                    if (upBlock == tblock.get()) {
                        makeFile(upHost.get().toString(), getMkfileCompletionHandler(), options.cancellationSignal);
                        return;
                    }
                }

                if (blockInfo.size() > 0) {
                    BlockElement mblock = getBlockInfo();
                    if (mblock.getOffset() != 0 && mblock.getBlocksize() != 0)
                        new UploadThread(mblock.getOffset(), mblock.getBlocksize(), upHost.get().toString()).start();
                }
            }
        };
    }

    private synchronized void updateRetried() {
        if (singleDomainRetry.get() < config.retryMax) {
            singleDomainRetry.getAndAdd(1);
        } else if (retried.get() < domainRetry) {
            singleDomainRetry.getAndSet(1);
            retried.getAndAdd(1);
            upHost.getAndSet(config.zone.upHost(token.token, config.useHttps, upHost.get().toString()));
        }
    }

    private boolean checkRetried() {
        return retried.get() < domainRetry;
    }

    private boolean isChunkOK(ResponseInfo info, JSONObject response) {
        return info.statusCode == 200 && info.error == null && (info.hasReqId() || isChunkResOK(response));
    }

    private boolean isChunkResOK(JSONObject response) {
        try {
            // getXxxx 若获取不到值,会抛出异常
            response.getString("ctx");
            response.getLong("crc32");
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private boolean isNotChunkToQiniu(ResponseInfo info, JSONObject response) {
        return info.statusCode < 500 && info.statusCode >= 200 && (!info.hasReqId() && !isChunkResOK(response));
    }

    private Long[] recoveryFromRecord() {

        if (config.recorder == null) {
            return null;
        }
        byte[] data = config.recorder.get(recorderKey);
        if (data == null) {
            return null;
        }
        String jsonStr = new String(data);
        JSONObject obj;
        try {
            obj = new JSONObject(jsonStr);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        JSONArray offsetsArray = obj.optJSONArray("offsets");
        long modify = obj.optLong("modify_time", 0);
        long fSize = obj.optLong("size", 0);
        JSONArray array = obj.optJSONArray("contexts");
        if (offsetsArray.length() == 0 || modify != modifyTime || fSize != totalSize || array == null || array.length() == 0) {
            return null;
        }
        for (int i = 0; i < array.length(); i++) {
            contexts[i] = array.optString(i);
        }
        for (int i = 0; i < array.length(); i++) {
            String offset = offsetsArray.optString(i);
            if (offset != null && !offset.equals("null")) {
                offsets[i] = Long.parseLong(offset);
            }
        }
        return offsets;
    }

    private void removeRecord() {
        if (config.recorder != null) {
            config.recorder.del(recorderKey);
        }
    }

    private void record(Long[] offsets) {
        if (config.recorder == null || offsets.length == 0) {
            return;
        }
        String data = format(Locale.ENGLISH, "{\"size\":%d,\"offsets\":[%s], \"modify_time\":%d, \"contexts\":[%s]}",
                totalSize, StringUtils.jsonJoin(offsets), modifyTime, StringUtils.jsonJoin(contexts));
        config.recorder.set(recorderKey, data.getBytes());
    }

    class BlockElement {
        private long offset;
        private int blocksize;

        BlockElement(long offset, int blocksize) {
            this.offset = offset;
            this.blocksize = blocksize;
        }

        public long getOffset() {
            return offset;
        }

        public int getBlocksize() {
            return blocksize;
        }
    }

}
