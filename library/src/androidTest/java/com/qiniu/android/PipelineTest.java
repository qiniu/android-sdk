package com.qiniu.android;


import android.test.InstrumentationTestCase;

import com.qiniu.android.bigdata.pipeline.Pipeline;
import com.qiniu.android.bigdata.pipeline.Point;
import com.qiniu.android.http.ResponseInfo;

import junit.framework.Assert;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by long on 2017/7/25.
 */

public class PipelineTest extends InstrumentationTestCase {
    final CountDownLatch signal = new CountDownLatch(1);
    private volatile ResponseInfo info = null;

    public void testPoint() {
        Point p = Point.fromPointObject(new A(3));
        Map<String, Integer> m = new HashMap<>();
        m.put("a", 3);
        Point p2 = Point.fromPointMap(m);
        Assert.assertEquals(p.toString(), p2.toString());
    }

    public void testPump() {
        Map<String, Object> map = new HashMap<>();
        map.put("platform", "android");
        map.put("tl", 1L);
        map.put("tf", 1.0);
        map.put("tb", true);
        map.put("td", new Date());
        Pipeline pipe = new Pipeline(null);
        pipe.pump("testsdk", map, "Pandora le0xKwjp2_9ZGZMkCok7Gko6aG5GnIHValG82deI:w-Ro_Gc1E-S_IX6CnWLw_to9xt0=:eyJyZXNvdXJjZSI6Ii92Mi9yZXBvcy90ZXN0c2RrL2RhdGEiLCJleHBpcmVzIjoxNTAxMDM2MzM2LCJjb250ZW50TUQ1IjoiIiwiY29udGVudFR5cGUiOiJ0ZXh0L3BsYWluIiwiaGVhZGVycyI6IiIsIm1ldGhvZCI6IlBPU1QifQ==", new Pipeline.PumpCompleteHandler() {
            @Override
            public void complete(ResponseInfo inf) {
                info = inf;
                signal.countDown();
            }
        });

        try {
            signal.await(1200, TimeUnit.SECONDS); // wait for callback
            Assert.assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(info);
        Assert.assertTrue(info.isOK());
    }

    public void testPump2() {
        Pipeline pipe = new Pipeline(null);
        pipe.pump("testsdk", new tsdk(), "Pandora le0xKwjp2_9ZGZMkCok7Gko6aG5GnIHValG82deI:meB-yJH6nA27OOt0N3SP4wfpbJs=", new Pipeline.PumpCompleteHandler() {
            @Override
            public void complete(ResponseInfo inf) {
                info = inf;
                signal.countDown();
            }
        });

        try {
            signal.await(1200, TimeUnit.SECONDS); // wait for callback
            Assert.assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(info.isOK());
    }

    static class A {
        public Integer a;

        A(int i) {
            a = i;
        }
    }

    static class tsdk {
        public String platform = "android2";
        public long tl = 2;
        public double tf = 2.0;
        public boolean tb = true;
        public Date td = new Date();

    }

}
