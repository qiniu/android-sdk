package com.qiniu.android.bigdata.pipeline;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;
import com.qiniu.android.http.ResponseInfo;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by long on 2017/7/25.
 */

@RunWith(AndroidJUnit4.class)
public class PipelineTest extends BaseTest {
    final CountDownLatch signal = new CountDownLatch(1);
    private volatile ResponseInfo info = null;

    @Test
    public void testPoint() {
        StringBuilder b1 = new StringBuilder();
        Points.formatPoint(new A(3), b1);
        StringBuilder b2 = new StringBuilder();
        Map<String, Integer> m = new HashMap<>();
        m.put("a", 3);
        Points.formatPoint(m, b2);
        assertEquals(b1.toString(), b2.toString());
    }

    @Test
    public void testPump() {
        Map<String, Object> map = new HashMap<>();
        map.put("platform", "android");
        map.put("tl", 1L);
        map.put("tf", 1.0);
        map.put("tb", true);
        map.put("td", new Date());
        Pipeline pipe = new Pipeline(null);
        pipe.pump("testsdk", map, "Pandora le0xKwjp2_9ZGZMkCok7Gko6aG5GnIHValG82deI:yIl-J0zNjJCUii_7jag6-U79DPY=:eyJyZXNvdXJjZSI6Ii92Mi9yZXBvcy90ZXN0c2RrL2RhdGEiLCJleHBpcmVzIjo1MTAxMDQ1Njg0LCJjb250ZW50TUQ1IjoiIiwiY29udGVudFR5cGUiOiJ0ZXh0L3BsYWluIiwiaGVhZGVycyI6IiIsIm1ldGhvZCI6IlBPU1QifQ==", new Pipeline.PumpCompleteHandler() {
            @Override
            public void complete(ResponseInfo inf) {
                info = inf;
                signal.countDown();
            }
        });

        try {
            signal.await(1200, TimeUnit.SECONDS); // wait for callback
            assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(info);
//        assertTrue(info.isOK());
    }

    @Test
    public void testPump2() {
        Pipeline pipe = new Pipeline(null);
        pipe.pump("testsdk", new tsdk(), "Pandora le0xKwjp2_9ZGZMkCok7Gko6aG5GnIHValG82deI:yIl-J0zNjJCUii_7jag6-U79DPY=:eyJyZXNvdXJjZSI6Ii92Mi9yZXBvcy90ZXN0c2RrL2RhdGEiLCJleHBpcmVzIjo1MTAxMDQ1Njg0LCJjb250ZW50TUQ1IjoiIiwiY29udGVudFR5cGUiOiJ0ZXh0L3BsYWluIiwiaGVhZGVycyI6IiIsIm1ldGhvZCI6IlBPU1QifQ==", new Pipeline.PumpCompleteHandler() {
            @Override
            public void complete(ResponseInfo inf) {
                info = inf;
                signal.countDown();
            }
        });

        try {
            signal.await(1200, TimeUnit.SECONDS); // wait for callback
            assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        assertTrue(info.isOK());
    }

    @Test
    public void testPump3() {
        Map<String, Object> map = new HashMap<>();
        map.put("platform", "android");
        map.put("tl", 1L);
        map.put("tf", 1.0);
        map.put("tb", true);
        map.put("td", new Date());
        Pipeline pipe = new Pipeline(null);
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(map);
        list.add(map);
        pipe.pumpMulti("testsdk", list, "Pandora le0xKwjp2_9ZGZMkCok7Gko6aG5GnIHValG82deI:yIl-J0zNjJCUii_7jag6-U79DPY=:eyJyZXNvdXJjZSI6Ii92Mi9yZXBvcy90ZXN0c2RrL2RhdGEiLCJleHBpcmVzIjo1MTAxMDQ1Njg0LCJjb250ZW50TUQ1IjoiIiwiY29udGVudFR5cGUiOiJ0ZXh0L3BsYWluIiwiaGVhZGVycyI6IiIsIm1ldGhvZCI6IlBPU1QifQ==", new Pipeline.PumpCompleteHandler() {
            @Override
            public void complete(ResponseInfo inf) {
                info = inf;
                signal.countDown();
            }
        });

        try {
            signal.await(1200, TimeUnit.SECONDS); // wait for callback
            assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(info);
//        assertTrue(info.isOK());
    }

    @Test
    public void testPump4() {
        Pipeline pipe = new Pipeline(null);
        List<Object> list = new ArrayList<>();
        list.add(new tsdk());
        list.add(new tsdk());
        pipe.pumpMultiObjects("testsdk", list, "Pandora le0xKwjp2_9ZGZMkCok7Gko6aG5GnIHValG82deI:yIl-J0zNjJCUii_7jag6-U79DPY=:eyJyZXNvdXJjZSI6Ii92Mi9yZXBvcy90ZXN0c2RrL2RhdGEiLCJleHBpcmVzIjo1MTAxMDQ1Njg0LCJjb250ZW50TUQ1IjoiIiwiY29udGVudFR5cGUiOiJ0ZXh0L3BsYWluIiwiaGVhZGVycyI6IiIsIm1ldGhvZCI6IlBPU1QifQ==", new Pipeline.PumpCompleteHandler() {
            @Override
            public void complete(ResponseInfo inf) {
                info = inf;
                signal.countDown();
            }
        });

        try {
            signal.await(1200, TimeUnit.SECONDS); // wait for callback
            assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        assertTrue(info.isOK());
    }

    @Test
    public void testPump5() {
        Pipeline pipe = new Pipeline(null);
        tsdk[] t = new tsdk[2];
        t[0] = new tsdk();
        t[1] = new tsdk();
        pipe.pumpMultiObjects("testsdk", t, "Pandora le0xKwjp2_9ZGZMkCok7Gko6aG5GnIHValG82deI:yIl-J0zNjJCUii_7jag6-U79DPY=:eyJyZXNvdXJjZSI6Ii92Mi9yZXBvcy90ZXN0c2RrL2RhdGEiLCJleHBpcmVzIjo1MTAxMDQ1Njg0LCJjb250ZW50TUQ1IjoiIiwiY29udGVudFR5cGUiOiJ0ZXh0L3BsYWluIiwiaGVhZGVycyI6IiIsIm1ldGhvZCI6IlBPU1QifQ==", new Pipeline.PumpCompleteHandler() {
            @Override
            public void complete(ResponseInfo inf) {
                info = inf;
                signal.countDown();
            }
        });

        try {
            signal.await(1200, TimeUnit.SECONDS); // wait for callback
            assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        assertTrue(info.isOK());
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
