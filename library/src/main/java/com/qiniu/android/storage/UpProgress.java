package com.qiniu.android.storage;

import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.LogUtil;

class UpProgress {

    private volatile long maxProgressUploadBytes = -1;
    private volatile long previousUploadBytes = 0;
    private final UpProgressHandler handler;

    UpProgress(UpProgressHandler handler) {
        this.handler = handler;
    }

    public void progress(final String key, long uploadBytes, final long totalBytes) {
        if (handler == null || uploadBytes < 0 || (totalBytes > 0 && uploadBytes > totalBytes)) {
            return;
        }

        if (totalBytes > 0) {
            if (this.maxProgressUploadBytes < 0) {
                this.maxProgressUploadBytes = (long)(totalBytes * 0.95);
            }

            if (uploadBytes > maxProgressUploadBytes) {
                return;
            }
        }

        if (uploadBytes > previousUploadBytes) {
            previousUploadBytes = uploadBytes;
        } else {
            // 不大于之前回调百分比，不再回调
            return;
        }

        if (handler instanceof UpProgressBytesHandler) {
            final long uploadBytesFinal = uploadBytes;
            AsyncRun.runInMain(new Runnable() {
                @Override
                public void run() {
                    LogUtil.i("key:" + key + " progress uploadBytes:" + uploadBytesFinal + " totalBytes:" + totalBytes);
                    ((UpProgressBytesHandler) handler).progress(key, uploadBytesFinal, totalBytes);
                }
            });
            return;
        }

        if (totalBytes < 0) {
            return;
        }

        final double notifyPercent = (double) uploadBytes / (double) totalBytes;
        AsyncRun.runInMain(new Runnable() {
            @Override
            public void run() {
                LogUtil.i("key:" + key + " progress:" + notifyPercent);
                handler.progress(key, notifyPercent);
            }
        });
    }

    public void notifyDone(final String key, final long totalBytes) {
        if (handler == null) {
            return;
        }

        // 不管什么类型 source, 在资源 EOF 时间，均可以获取到文件的大小
        if (handler instanceof UpProgressBytesHandler) {
            AsyncRun.runInMain(new Runnable() {
                @Override
                public void run() {
                    LogUtil.i("key:" + key + " progress uploadBytes:" + totalBytes + " totalBytes:" + totalBytes);
                    ((UpProgressBytesHandler) handler).progress(key, totalBytes, totalBytes);
                }
            });
            return;
        }

        AsyncRun.runInMain(new Runnable() {
            @Override
            public void run() {
                LogUtil.i("key:" + key + " progress:1");
                handler.progress(key, 1);
            }
        });
    }
}
