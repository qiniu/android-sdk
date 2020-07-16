package com.qiniu.android.common;

import com.qiniu.android.BaseTest;
import com.qiniu.android.TestConfig;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.LogUtil;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by long on 2016/9/30.
 */
public class AutoZoneTest extends BaseTest {
    private String ak = TestConfig.ak;
    private String bkt = "javasdk";

    public void testHttp() {

        final WaitCondition waitCondition = new WaitCondition();

        zoneRequest(new CompleteHandlder() {
            @Override
            public void complete(boolean isSuccess) {
                waitCondition.shouldWait = false;
                assertTrue(isSuccess);
            }
        });

        wait(waitCondition, 100);
    }

    public void testMufiHttp() {

        final TestParam param = new TestParam();
        final int maxCount = 5;
        for (int i = 0; i < maxCount; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    zoneRequest(new CompleteHandlder() {
                        @Override
                        public void complete(boolean isSuccess) {
                            param.completeCount.incrementAndGet();
                            assertTrue(isSuccess);
                        }
                    });

                }
            }).start();
        }

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (param.completeCount.intValue() >= maxCount){
                    return false;
                } else {
                    return true;
                }
            }
        }, 60);

        LogUtil.i("== muti complete");
    }

    private void zoneRequest(final CompleteHandlder completeHandlder){
        AutoZone zone = new AutoZone();
        UpToken token = UpToken.parse(TestConfig.token_z0);

        zone.preQuery(token, new Zone.QueryHandler() {
            @Override
            public void complete(int code, ResponseInfo responseInfo, UploadRegionRequestMetrics metrics) {
                if (code == 0){
                    completeHandlder.complete(true);
                } else {
                    completeHandlder.complete(false);
                }
            }
        });
    }

    private interface CompleteHandlder {
        void complete(boolean isSuccess);
    }

    private class TestParam{
        AtomicInteger completeCount = new AtomicInteger(0);
    }

}
