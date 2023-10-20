package com.qiniu.android.common;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FixZoneTest extends BaseTest {

    @Test
    public void testCreateByRegionId() {
        FixedZone zone = FixedZone.createWithRegionId("na0");
        ZoneInfo zoneInfo = zone.getZonesInfo(null).zonesInfo.get(0);

        assertTrue(zoneInfo.regionId.equals("na0"));
        assertTrue(zoneInfo.domains.get(0).equals("upload-na0.qiniup.com"));
        assertTrue(zoneInfo.domains.get(1).equals("up-na0.qiniup.com"));
    }
}
