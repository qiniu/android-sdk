package com.qiniu.android.storage;

import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.LogUtil;

import kotlin.jvm.Synchronized;

class UpProgress {

    private long maxProgressUploadBytes = -1;
    private long previousUploadBytes = 0;
    private final UpProgressHandler handler;

    UpProgress(UpProgressHandler handler) {
        this.handler = handler;
    }

    public void progress(final String key, long uploadBytes, final long totalBytes) {
        if (handler == null || uploadBytes < 0 || (totalBytes > 0 && uploadBytes > totalBytes)) {
            return;
        }

        if (totalBytes > 0) {
            synchronized (this) {
                if (this.maxProgressUploadBytes < 0) {
                    this.maxProgressUploadBytes = (long) (totalBytes * 0.95);
                }
            }

            if (uploadBytes > this.maxProgressUploadBytes) {
                return;
            }
        }

        synchronized (this) {
            if (uploadBytes > this.previousUploadBytes) {
                this.previousUploadBytes = uploadBytes;
            } else {
                // 不大于之前回调百分比，不再回调
                return;
            }
        }

        notify(key, uploadBytes, totalBytes);
    }

    public void notifyDone(final String key, final long totalBytes) {
        notify(key, totalBytes, totalBytes);
    }

    private void notify(final String key, long uploadBytes, final long totalBytes) {
        if (handler == null) {
            return;
        }

        // 不管什么类型 source, 在资源 EOF 时间，均可以获取到文件的大小
        if (handler instanceof UpProgressBytesHandler) {
            AsyncRun.runInMain(new Runnable() {
                @Override
                public void run() {
                    LogUtil.i("key:" + key + " progress uploadBytes:" + uploadBytes + " totalBytes:" + totalBytes);
                    ((UpProgressBytesHandler) handler).progress(key, uploadBytes, totalBytes);
                }
            });
            return;
        }

        if (totalBytes <= 0) {
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
}
