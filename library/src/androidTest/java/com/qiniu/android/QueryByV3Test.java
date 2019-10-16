package com.qiniu.android;

import android.test.InstrumentationTestCase;
import android.util.Log;

import com.qiniu.android.common.AutoZone;

import com.qiniu.android.common.ZoneInfo;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by jemy on 2019/9/16.
 */

public class QueryByV3Test extends InstrumentationTestCase {

    private String token = TestConfig.uptoken_v3_query;
    private String ak = "bjtWBQXrcxgo7HWwlC_bgHg81j352_GhgBGZPeOW";
    private String bucket = "shuanghuo1";

    public void testAutoZone() {
        AutoZone autoZone = new AutoZone();
        autoZone.preQuery(token);
        ZoneInfo zoneInfo = autoZone.zoneInfo(ak, bucket);
        Log.e("qiniutest", "info:" + zoneInfo.toString());
        List<String> upDomainsList = zoneInfo.upDomainsList;
        Log.e("qiniutest", "size:" + upDomainsList.size());
        for (int i = 0; i < upDomainsList.size(); i++) {
            Log.e("qiniutest", "upDomainsList:" + upDomainsList.get(i));
        }
    }

}
