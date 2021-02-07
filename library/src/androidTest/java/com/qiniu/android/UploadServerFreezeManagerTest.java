package com.qiniu.android;

import com.qiniu.android.http.serverRegion.UploadServerFreezeManager;

public class UploadServerFreezeManagerTest extends BaseTest {

    public void testFreeze() {

        String host = "baidu.com";
        String type = host;
        UploadServerFreezeManager.getInstance().freezeType(type, 10);

        boolean isFrozen = UploadServerFreezeManager.getInstance().isTypeFrozen(type);
        assertTrue(isFrozen);
    }

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
