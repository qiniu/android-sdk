package com.qiniu.android.storage;

import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.LogUtil;

class UpProgress {

    private double previousPercent;
    private final UpProgressHandler handler;

    UpProgress(UpProgressHandler handler){
        this.handler = handler;
        this.previousPercent = 0;
    }

    public void progress(final String key, final long uploadBytes, final long totalBytes) {
        if (handler != null && handler instanceof UpProgressBytesHandler) {
            AsyncRun.runInMain(new Runnable() {
                @Override
                public void run() {
                    LogUtil.i("key:" + key + " progress uploadBytes:" + uploadBytes + " totalBytes:" + totalBytes);
                    ((UpProgressBytesHandler) handler).progress(key, uploadBytes, totalBytes);
                }
            });
            return;
        }

        if (totalBytes <= 0 || uploadBytes < 0) {
            return;
        }

        double percent = (double) uploadBytes / (double) totalBytes;
        if (percent > 0.95) {
            percent = 0.95;
        }
        if (percent > previousPercent) {
            previousPercent = percent;
        } else {
            percent = previousPercent;
        }

        if (handler != null) {
            final double notifyPercent = percent;
            AsyncRun.runInMain(new Runnable() {
                @Override
                public void run() {
                    LogUtil.i("key:" + key + " progress:" + notifyPercent);
                    handler.progress(key, notifyPercent);
                }
            });
        }
    }

    public void notifyDone(final String key) {
        if (handler != null && handler instanceof UpProgressBytesHandler) {
            return;
        }

        if (handler != null) {
            AsyncRun.runInMain(new Runnable() {
                @Override
                public void run() {
                    LogUtil.i("key:" + key + " progress:1");
                    handler.progress(key, 1);
                }
            });
        }
    }
}
