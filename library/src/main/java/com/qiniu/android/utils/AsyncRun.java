package com.qiniu.android.utils;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by bailong on 14/10/22.
 */
public final class AsyncRun {
    public static void run(final Runnable r) {
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                try {
                    r.run();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
}
