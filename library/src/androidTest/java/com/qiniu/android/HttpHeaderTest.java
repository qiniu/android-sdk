package com.qiniu.android;
import android.test.AndroidTestCase;

import com.qiniu.android.http.Headers;

import java.util.HashMap;

public class HttpHeaderTest extends AndroidTestCase {

    private Headers headers;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        HashMap<String, String> keyVaules = new HashMap<>();
        keyVaules.put("date", "2020-07-15 07:40:01");
        keyVaules.put("host", "127.0.0.1");
//        keyVaules.put("", "illegal key");
        headers = Headers.of(keyVaules);
    }

    public void testValue(){

        assertTrue(headers.getDate("date") != null);
        assertTrue(headers.values("host") != null);
    }

    public void testCount(){

        assertTrue(headers.get("host") != null);
        assertTrue(headers.values("host") != null);
    }

    public void testBuilder(){
        String dateKey = "date";
        String dateValue = "2020-07-15 20:02:30";
        String hostKey = "host";
        String hostValue = "127.0.0.1";

        Headers.Builder builder0 = new Headers.Builder();
        builder0.add(dateKey, dateValue);
        builder0.add(hostKey + ":" + hostValue);
        Headers headers0 = builder0.build();

        String host = headers0.get(hostKey);
        assertTrue(headers0.get(dateKey).equals(dateValue));
        assertTrue(headers0.get(hostKey).equals(hostValue));

        Headers.Builder builder1 = new Headers.Builder();
        builder1.addAll(headers0);
        assertTrue(builder1.get(dateKey).equals(dateValue));

    }
}
