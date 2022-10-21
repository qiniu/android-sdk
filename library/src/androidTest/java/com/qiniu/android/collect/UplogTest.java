package com.qiniu.android.collect;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.TestConfig;
import com.qiniu.android.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UplogTest extends BaseTest {

    @Test
    public void test() {
    }

    private void no_testUplog() {
        ReportItem item = new ReportItem();
        item.setReport(ReportItem.LogTypeRequest, ReportItem.RequestKeyLogType);
        item.setReport(1634567890, ReportItem.RequestKeyUpTime);
        item.setReport(200, ReportItem.RequestKeyStatusCode);
        item.setReport("reqid", ReportItem.RequestKeyRequestId);
        item.setReport("host", ReportItem.RequestKeyHost);
        item.setReport("remoteAddress", ReportItem.RequestKeyRemoteIp);
        item.setReport(80, ReportItem.RequestKeyPort);
        item.setReport("bucket", ReportItem.RequestKeyTargetBucket);
        item.setReport("key", ReportItem.RequestKeyTargetKey);
        item.setReport(123, ReportItem.RequestKeyTotalElapsedTime);
        item.setReport(123, ReportItem.RequestKeyDnsElapsedTime);
        item.setReport(123, ReportItem.RequestKeyConnectElapsedTime);
        item.setReport(123, ReportItem.RequestKeyTLSConnectElapsedTime);
        item.setReport(123, ReportItem.RequestKeyRequestElapsedTime);
        item.setReport(123, ReportItem.RequestKeyWaitElapsedTime);
        item.setReport(123, ReportItem.RequestKeyResponseElapsedTime);
        item.setReport(123, ReportItem.RequestKeyFileOffset);
        item.setReport(123, ReportItem.RequestKeyBytesSent);
        item.setReport(123, ReportItem.RequestKeyBytesTotal);
        item.setReport("123", ReportItem.RequestKeyPid);
        item.setReport(123, ReportItem.RequestKeyTid);
        item.setReport("regionid", ReportItem.RequestKeyTargetRegionId);
        item.setReport("regionid", ReportItem.RequestKeyCurrentRegionId);
        item.setReport("errorType", ReportItem.RequestKeyErrorType);

        item.setReport("errorDesc", ReportItem.RequestKeyErrorDescription);
        item.setReport("form", ReportItem.RequestKeyUpType);
        item.setReport("android", ReportItem.RequestKeyOsName);
        item.setReport("10", ReportItem.RequestKeyOsVersion);
        item.setReport("Android", ReportItem.RequestKeySDKName);
        item.setReport("8.3.3", ReportItem.RequestKeySDKVersion);
        item.setReport(1623456789, ReportItem.RequestKeyClientTime);
        item.setReport("wifi", ReportItem.RequestKeyNetworkType);
        item.setReport(-1, ReportItem.RequestKeySignalStrength);

        item.setReport("server", ReportItem.RequestKeyPrefetchedDnsSource);
        item.setReport(10, ReportItem.RequestKeyPrefetchedBefore);
        item.setReport("lastPrefetchErrorMessage", ReportItem.RequestKeyPrefetchedErrorMessage);

        item.setReport("okhttp", ReportItem.RequestKeyHttpClient);
        item.setReport("4.2.2", ReportItem.RequestKeyHttpClientVersion);
        item.setReport("disable", ReportItem.RequestKeyNetworkMeasuring);

        // 劫持标记
        item.setReport("hijacked", ReportItem.RequestKeyHijacking);
        item.setReport("syncDnsSource", ReportItem.RequestKeyDnsSource);
        item.setReport("syncDnsError", ReportItem.RequestKeyDnsErrorMessage);

        // 统计当前请求上传速度 / 总耗时
        item.setReport(123, ReportItem.RequestKeyPerceptiveSpeed);

        item.setReport("http1.1", ReportItem.RequestKeyHttpVersion);

        UploadInfoReporter.getInstance().report(item, TestConfig.commonToken);

//        wait(new WaitConditional() {
//            @Override
//            public boolean shouldWait() {
//                return true;
//            }
//        }, 10 * 60);
    }
}
