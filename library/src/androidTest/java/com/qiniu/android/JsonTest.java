package com.qiniu.android;

import android.test.AndroidTestCase;

import com.qiniu.android.utils.Json;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Simon on 3/3/16.
 */
public class JsonTest extends AndroidTestCase {

    private boolean showContent = false;

    public void testEmpty() {
        JSONObject json = new JSONObject();
        Assert.assertNotNull(json);
        Assert.assertEquals("{}", json.toString());
    }

    // e: org.json.JSONException: End of input at character 0 of
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

    public void testB() throws JSONException {
        String str = "{}";
        JSONObject json = new JSONObject(str);
        Assert.assertNotNull(json);
        if (showContent) {
            Assert.assertEquals(str, json.toString());
        }
    }

    // e: org.json.JSONException: Value [] of type org.json.JSONArray cannot be converted to JSONObject
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

    public void testEncodeMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("a", 1);
        String s = Json.encodeMap(m);
        Assert.assertEquals("{\"a\":1}", s);
    }

    public void testEncodeList() {
        List<String> l = new ArrayList<>();
        l.add("a");
        String s = Json.encodeList(l);
        Assert.assertEquals("[\"a\"]", s);
    }
}
