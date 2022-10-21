package com.qiniu.android.utils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StringUtilsTest extends BaseTest {

    @Test
    public void testJoin(){

        String string = "";

        string = StringUtils.join(new String[]{"1", "2"}, "-");
        assertTrue(string.equals("1-2"));

        string = StringUtils.jsonJoin(new String[]{"1", "2"});
        assertTrue(string.equals("\"1\",\"2\""));

    }

    @Test
    public void testTransform(){

        String string = "";

        string = StringUtils.jsonJoin(new Long[]{1L, 2L});
        assertTrue(string.equals("\"1\",\"2\""));

        String[] stringArray = StringUtils.longToString(new Long[]{1L, 2L});
        assertTrue(stringArray.length == 2);

        string = "1234";
        byte[] data = StringUtils.toByteArray(string);
        byte[] dataR = new byte[]{-84, -19, 0, 5, 116, 0, 4, 49, 50, 51, 52};
        assertTrue(new String(dataR).equals(new String(data)));

        Object stringR = StringUtils.toObject(dataR);
        assertTrue(stringR.equals(string));


        string = "tom";
        string = StringUtils.upperCase(string);
        assertTrue(string.equals("Tom"));
    }

    @Test
    public void testToken(){
        String token = "jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu:mWEhowqz4sG301DXU6CB3IO7Zss=:eyJzY29wZSI6InpvbmUxLXNwYWNlIiwiZGVhZGxpbmUiOjE1OTczOTMzOTYsICJyZXR1cm5Cb2R5Ijoie1wiZm9vXCI6JCh4OmZvbyksIFwiYmFyXCI6JCh4OmJhciksIFwibWltZVR5cGVcIjokKG1pbWVUeXBlKSwgXCJoYXNoXCI6JChldGFnKSwgXCJrZXlcIjokKGtleSksIFwiZm5hbWVcIjokKGZuYW1lKX0ifQ==";
        String ak = StringUtils.getAkAndScope(token);
        assertTrue(ak.equals("jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22euzone1-space"));

        String bucket = StringUtils.getBucket(token);
        assertTrue(bucket.equals("zone1-space"));
    }
}
