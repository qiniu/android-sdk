package com.qiniu.android.http.serverRegion;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UploadServerFreezeManagerTest extends BaseTest {

    @Test
    public void testFreeze() {

        String host = "baidu.com";
        String type = host;
        UploadServerFreezeManager.getInstance().freezeType(type, 10);

        boolean isFrozen = UploadServerFreezeManager.getInstance().isTypeFrozen(type);
        assertTrue(isFrozen);
    }

    @Test
    public void testUnfreeze() {

        String host = "baidu.com";
        String type = host;
        UploadServerFreezeManager.getInstance().freezeType(type, 10);

        boolean isFrozen = UploadServerFreezeManager.getInstance().isTypeFrozen(type);
        assertTrue(isFrozen);

        UploadServerFreezeManager.getInstance().unfreezeType(type);
        isFrozen = UploadServerFreezeManager.getInstance().isTypeFrozen(type);
        assertTrue(isFrozen == false);
    }
}
