package com.qiniu.android.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by bailong on 14/10/22.
 */
public final class AsyncRun {

    private static final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public static void runInMain(Runnable r) {
        Handler h = new Handler(Looper.getMainLooper());
        h.post(r);
    }

    public static void runInBack(Runnable r) {
       executorService.submit(r);
    }
}

