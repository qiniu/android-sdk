package com.qiniu.android.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.Timer;
import java.util.TimerTask;
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

    public static void runInMain(int delay,
                                 final Runnable r){

        delayTimerTask(delay, new TimerTask() {
            @Override
            public void run() {
                Handler h = new Handler(Looper.getMainLooper());
                h.post(r);
                this.cancel();
            }
        });
    }

    public static void runInBack(Runnable r) {
       executorService.submit(r);
    }

    public static void runInBack(int delay,
                                 final Runnable r) {

        delayTimerTask(delay, new TimerTask() {
            @Override
            public void run() {
                executorService.submit(r);
                this.cancel();
            }
        });
    }

    private static void delayTimerTask(int delay, TimerTask timerTask){
        Timer timer = new Timer();
        timer.schedule(timerTask, delay);
    }

}

