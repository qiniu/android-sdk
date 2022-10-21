package com.qiniu.android.http.serverRegion;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.http.request.IUploadServer;
import com.qiniu.android.http.request.UploadRequestState;
import com.qiniu.android.BaseTest;
import com.qiniu.android.utils.Utils;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UploadDomainRegionTest extends BaseTest {

    @Test
    public void testGetOneServer(){

        String host = "baidu.com";
        String type = Utils.getIpType(null, host);
        FixedZone zone = new FixedZone(new String[]{host});

        UploadDomainRegion region = new UploadDomainRegion();
        region.setupRegionData(zone.getZonesInfo(null).zonesInfo.get(0));

        UploadServerFreezeManager.getInstance().freezeType(type, 100);

        UploadRequestState state = new UploadRequestState();
        state.setUseOldServer(false);
        IUploadServer server = region.getNextServer(state, null, null);

        assertNotNull(server);
    }

}
