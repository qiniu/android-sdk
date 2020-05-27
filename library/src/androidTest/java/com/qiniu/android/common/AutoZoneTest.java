package com.qiniu.android.common;

import com.qiniu.android.BaseTest;
import com.qiniu.android.TestConfig;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpToken;

import junit.framework.Assert;


/**
 * Created by long on 2016/9/30.
 */
public class AutoZoneTest extends BaseTest {
    private String ak = TestConfig.ak;
    private String bkt = "javasdk";

    public void testHttp() {
        AutoZone zone = new AutoZone();
        UpToken token = UpToken.parse(TestConfig.token_z0);

        final WaitCondition waitCondition = new WaitCondition();
        waitCondition.shouldWait = true;

        final int[] c = {-1};
        zone.preQuery(token, new Zone.QueryHandler() {
            @Override
            public void complete(int code, ResponseInfo responseInfo) {
                c[0] = code;
                waitCondition.shouldWait = false;
            }
        });

        wait(waitCondition, 100);

        assertEquals(0, c[0]);
    }

    public void testMutiHttp() {
        for (int i = 0; i < 5; i++) {
            testHttp();
        }
    }

}
