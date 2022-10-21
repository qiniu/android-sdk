package com.qiniu.android.http;

import static org.junit.Assert.assertTrue;

import com.qiniu.android.utils.LogUtil;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
public class HttpHeaderTest {

    private Headers headers;

    @Before
    public void setUp() throws Exception {

        HashMap<String, String> keyVaules = new HashMap<>();
        keyVaules.put("date", "2020-07-15 07:40:01");
        keyVaules.put("host", "127.0.0.1");
        headers = Headers.of(keyVaules);
        LogUtil.i(headers.names().toString());
    }

    @Test
    public void testValue() {

        assertTrue(headers.values("date") != null);
        assertTrue(headers.values("host") != null);
    }

    @Test
    public void testCount() {

        assertTrue(headers.byteCount() > 0);
    }

    @Test
    public void testBuilder() {
        String dateKey = "date";
        String dateValue = HttpDate.format(new Date());
        String hostKey = "host";
        String hostValue = "127.0.0.1";

        Headers.Builder builder0 = new Headers.Builder();
        builder0.add(dateKey, dateValue);
        builder0.add(hostKey + ":" + hostValue);
        Headers headers0 = builder0.build();

        assertTrue(headers0.get(dateKey).equals(dateValue));
        assertTrue(headers0.getDate(dateKey) != null);
        assertTrue(headers0.get(hostKey).equals(hostValue));


        Headers.Builder builder1 = new Headers.Builder();
        builder1.addAll(headers0);
        assertTrue(builder1.get(dateKey).equals(dateValue));


        Headers headers1 = headers0.newBuilder().build();
        assertTrue(headers1.equals(headers0));
        assertTrue(headers1.toString().equals(headers0.toString()));
        assertTrue(headers1.hashCode() == headers0.hashCode());


        Headers headers = Headers.of(hostKey, hostValue, dateKey, dateValue);
        assertTrue(headers.get(dateKey).equals(dateValue));


        headers1.toMultimap();


        builder1.set("time", "2020-07-10 10:20:14");
        headers1 = builder1.build();
        assertTrue(headers1.get("time").equals("2020-07-10 10:20:14"));

        builder1.removeAll("time");
        headers1 = builder1.build();
        assertTrue(headers1.get("time") == null);
    }

    @Test
    public void testBuilderError() {

        Headers headers = null;

        String headersString = null;
        try {
            headers = Headers.of(headersString);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            headers = Headers.of("key");
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            headers = Headers.of("key", null);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            headers = Headers.of("key", "");
        } catch (Exception e) {
            assertTrue(true);
        }

    }
}
