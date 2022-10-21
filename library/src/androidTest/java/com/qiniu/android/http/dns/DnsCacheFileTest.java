package com.qiniu.android.http.dns;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;
import com.qiniu.android.storage.GlobalConfiguration;
import com.qiniu.android.utils.LogUtil;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class DnsCacheFileTest extends BaseTest {

    @Test
    public void testCreate(){
        try {
            DnsCacheFile file = new DnsCacheFile(null);
            if (file != null){
                assertTrue(false);
            }
        } catch (IOException e) {
            assertTrue(true);
        }


        DnsCacheFile file = null;
        try {
            file = new DnsCacheFile(GlobalConfiguration.getInstance().dnsCacheDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        LogUtil.i("DnsCacheFile name:" + file.getFileName());
        assertTrue(file != null);

    }

    @Test
    public void testValue(){

        String dataStringBefore = "123";
        byte[] dataBefore = dataStringBefore.getBytes();

        DnsCacheFile file = null;
        try {
            file = new DnsCacheFile(GlobalConfiguration.getInstance().dnsCacheDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        file.set("key", dataBefore);

        byte[] dataAfter = file.get("key");
        String dataStringAfter = new String(dataAfter);

        assertTrue(dataStringBefore.equals(dataStringAfter));


        file.del("key");
        dataAfter = file.get("key");
        assertTrue(dataAfter == null);

    }
}
