package com.qiniu.android.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * Created by bailong on 14/10/22.
 */
public final class AsyncRun {

    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private static int threadPoolSize = 1;
    private static int maxThreadPoolSize = 6;
    private static ExecutorService executorService = new ThreadPoolExecutor(threadPoolSize, maxThreadPoolSize,
            1000L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

    public static void runInMain(Runnable r) {
        if (Looper.getMainLooper() == Looper.myLooper()){
            r.run();
        } else {
            mainThreadHandler.post(r);
        }
    }

    /**
     * delay: delay in milliseconds before task is to be executed.
     */
    public static void runInMain(int delay,
                                 final Runnable r){

        delayTimerTask(delay, new TimerTask() {
            @Override
            public void run() {
                mainThreadHandler.post(r);
                this.cancel();
            }
        });
    }

    public static void runInBack(Runnable r) {
        executorService.submit(r);
    }

    /**
     * delay: delay in milliseconds before task is to be executed.
     */
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

