package com.qiniu.android;
import android.test.AndroidTestCase;

import com.qiniu.android.http.Headers;
import com.qiniu.android.http.HttpDate;
import com.qiniu.android.utils.LogUtil;

import java.util.Date;
import java.util.HashMap;

public class HttpHeaderTest extends AndroidTestCase {

    private Headers headers;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        HashMap<String, String> keyVaules = new HashMap<>();
        keyVaules.put("date", "2020-07-15 07:40:01");
        keyVaules.put("host", "127.0.0.1");
        headers = Headers.of(keyVaules);
        LogUtil.i(headers.names().toString());
    }

    public void testValue(){

        assertTrue(headers.values("date") != null);
        assertTrue(headers.values("host") != null);
    }

    public void testCount(){

        assertTrue(headers.byteCount() > 0);
    }

    public void testBuilder(){
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


    public void testBuilderError(){

        Headers headers = null;

        String headersString = null;
        try {
            headers = Headers.of(headersString);
        } catch (Exception e){
            assertTrue(true);
        }

        try {
            headers = Headers.of("key");
        } catch (Exception e){
            assertTrue(true);
        }

        try {
            headers = Headers.of("key", null);
        } catch (Exception e){
            assertTrue(true);
        }

        try {
            headers = Headers.of("key", "");
        } catch (Exception e){
            assertTrue(true);
        }

    }
}
