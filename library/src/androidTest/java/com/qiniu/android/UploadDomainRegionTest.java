package com.qiniu.android;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.http.request.IUploadServer;
import com.qiniu.android.http.serverRegion.UploadDomainRegion;
import com.qiniu.android.http.serverRegion.UploadServerFreezeManager;
import com.qiniu.android.utils.Utils;

public class UploadDomainRegionTest extends BaseTest {


    public void testGetOneServer(){

        String host = "baidu.com";
        String type = Utils.getIpType(null, host);
        FixedZone zone = new FixedZone(new String[]{host});

        UploadDomainRegion region = new UploadDomainRegion();
        region.setupRegionData(zone.getZonesInfo(null).zonesInfo.get(0));

        UploadServerFreezeManager.getInstance().freezeHost(host, type, 100);

        IUploadServer server = region.getNextServer(false, null, null);

        assertNotNull(server);
    }

}
