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

//    public void testHttp() {
//        AutoZone zone = new AutoZone();
//        final CountDownLatch countDownLatch = new CountDownLatch(1);
//        zone.preQueryIndex(new AutoZone.ZoneIndex(ak, bkt), new Zone.QueryHandler() {
//            @Override
//            public void onSuccess() {
//                countDownLatch.countDown();
//            }
//
//            @Override
//            public void onFailure(int reason) {
//                countDownLatch.countDown();
//                fail();
//            }
//        });
//        try {
//            countDownLatch.await();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        ZoneInfo zoneInfo = zone.zoneInfo(ak, bkt);
//
////        assertTrue(zoneInfo.upDomainsList.contains("upload.qiniup.com"));
////        assertTrue(zoneInfo.upDomainsList.contains("up.qiniup.com"));
////        assertTrue(zoneInfo.upDomainsList.contains("upload-nb.qiniup.com"));
//    }

//    public void testHttpFail() {
//        AutoZone zone = new AutoZone();
//        ZoneInfo zoneInfo = zone.zoneInfo(ak + "_not_be_ak", bkt);
//        assertNull(zoneInfo);
//    }

//    public void testSplitE() {
//        String s1 = "bkt:key";
//        String s2 = "bkt";
//        Assert.assertEquals(s1.split(":")[0], s2.split(":")[0]);
//    }

    private boolean isTestUCServerComplete = false;
    public void testUCServer(){
        String ucServer = "ucserver.test";
        AutoZone autoZone = new AutoZone();
        autoZone.setUcServer(ucServer);
        assertTrue(autoZone.getUcServerList().get(0) == ucServer);

        UpToken token = UpToken.parse(TestConfig.commonToken);

        autoZone.preQuery(token, new Zone.QueryHandler() {
            @Override
            public void complete(int code, ResponseInfo responseInfo, UploadRegionRequestMetrics metrics) {
                assertTrue(code == 0);
                assertTrue(responseInfo.statusCode != 200);
                isTestUCServerComplete = true;
            }
        });

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                return !isTestUCServerComplete;
            }
        }, 60);

        assertTrue(autoZone.getZonesInfo(null) == null);
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
        final AutoZone zone = new AutoZone();
        final UpToken token = UpToken.parse(TestConfig.commonToken);

        zone.preQuery(token, new Zone.QueryHandler() {
            @Override
            public void complete(int code, ResponseInfo responseInfo, UploadRegionRequestMetrics metrics) {
                if (code == 0){
                    completeHandlder.complete(true);
                } else {
                    completeHandlder.complete(false);
                }

                ZonesInfo zonesInfo = zone.getZonesInfo(token);
                if (zonesInfo != null){
                    LogUtil.i(zonesInfo.toString());
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
