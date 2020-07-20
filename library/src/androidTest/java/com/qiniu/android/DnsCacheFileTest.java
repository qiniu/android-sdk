package com.qiniu.android;

import com.qiniu.android.http.dns.DnsCacheFile;
import com.qiniu.android.storage.GlobalConfiguration;

import java.io.IOException;

public class DnsCacheFileTest extends BaseTest {

    public void testCreate(){
        try {
            DnsCacheFile file = new DnsCacheFile("");
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

        assertTrue(file != null);

    }

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
