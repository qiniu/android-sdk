package com.qiniu.android;

import com.qiniu.android.http.serverRegion.UploadServerFreezeManager;

public class UploadServerFreezeManagerTest extends BaseTest {

    public void testFreeze() {

        String host = "baidu.com";
        String type = host;
        UploadServerFreezeManager.getInstance().freezeHost(host, type, 10);

        boolean isFrozen = UploadServerFreezeManager.getInstance().isFreezeHost(host, type);
        assertTrue(isFrozen);
    }

    public void testUnfreeze() {

        String host = "baidu.com";
        String type = host;
        UploadServerFreezeManager.getInstance().freezeHost(host, type, 10);

        boolean isFrozen = UploadServerFreezeManager.getInstance().isFreezeHost(host, type);
        assertTrue(isFrozen);

        UploadServerFreezeManager.getInstance().unfreezeHost(host, type);
        isFrozen = UploadServerFreezeManager.getInstance().isFreezeHost(host, type);
        assertTrue(isFrozen == false);

    }
}
