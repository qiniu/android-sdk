package com.qiniu.android.http.dns;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class BaseDns {
    int timeout = 10;
    static final ExecutorService executor = new ThreadPoolExecutor(0, 4,
            60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
}
