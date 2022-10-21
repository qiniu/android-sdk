package com.qiniu.android.utils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Simon on 3/3/16.
 */
@RunWith(AndroidJUnit4.class)
public class JsonTest {

    private boolean showContent = false;

    @Test
    public void testEmpty() {
        JSONObject json = new JSONObject();
        Assert.assertNotNull(json);
        Assert.assertEquals("{}", json.toString());
    }

    // e: org.json.JSONException: End of input at character 0 of
    @Test
    public void testEmpty1() throws JSONException {
        String str = "";
        Exception ex = null;
        try {
            JSONObject json = new JSONObject(str);
        } catch (JSONException e) {
            ex = e;
        }
        Assert.assertNotNull(ex);
        if (showContent) {
            Assert.assertEquals(str, ex.getMessage());
        }
    }

    //e: org.json.JSONException: End of input at character 2 of
    @Test
    public void testEmpty2() throws JSONException {
        String str = "  ";
        Exception ex = null;
        try {
            JSONObject json = new JSONObject(str);
        } catch (JSONException e) {
            ex = e;
        }
        Assert.assertNotNull(ex);
        if (showContent) {
            Assert.assertEquals(str, ex.getMessage());
        }
    }

    @Test
    public void testB() throws JSONException {
        String str = "{}";
        JSONObject json = new JSONObject(str);
        Assert.assertNotNull(json);
        if (showContent) {
            Assert.assertEquals(str, json.toString());
        }
    }

    // e: org.json.JSONException: Value [] of type org.json.JSONArray cannot be converted to JSONObject
    @Test
    public void testArray() throws JSONException {
        String str = "[]";
        Exception ex = null;
        try {
            JSONObject json = new JSONObject(str);// should JSONArray
        } catch (JSONException e) {
            ex = e;
        }
        Assert.assertNotNull(ex);
        if (showContent) {
            Assert.assertEquals(str, ex.getMessage());
        }
    }

    //e: org.json.JSONException: Value null of type org.json.JSONObject$1 cannot be converted to JSONObject
    @Test
    public void testNull() throws JSONException {
        String str = "null";
        Exception ex = null;
        try {
            JSONObject json = new JSONObject(str);
        } catch (JSONException e) {
            ex = e;
        }
        Assert.assertNotNull(ex);
        if (showContent) {
            Assert.assertEquals(str, ex.getMessage());
        }
    }

    @Test
    public void testEncodeMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("a", 1);
        String s = Json.encodeMap(m);
        Assert.assertEquals("{\"a\":1}", s);
    }

    @Test
    public void testEncodeList() {
        List<String> l = new ArrayList<>();
        l.add("a");
        String s = Json.encodeList(l);
        Assert.assertEquals("[\"a\"]", s);
    }
}
