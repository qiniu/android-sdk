package com.qiniu.android.http.connectCheck;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;
import com.qiniu.android.storage.GlobalConfiguration;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ConnectCheckTest extends BaseTest {

    @Test
    public void testCheck() {

        int maxCount = 100;
        int successCount = 0;
        for (int i = 0; i < maxCount; i++) {
            if (ConnectChecker.isConnected(ConnectChecker.check())) {
                successCount += 1;
            }
        }

        assertEquals("maxCount:" + maxCount + " successCount:" + successCount, maxCount, successCount);
    }

    @Test
    public void testCustomCheckHosts() {
        GlobalConfiguration.getInstance().connectCheckURLStrings = new String[]{"https://www.qiniu.com", "https://www.google.com"};
        int maxCount = 20;
        int successCount = 0;
        for (int i = 0; i < maxCount; i++) {
            if (ConnectChecker.isConnected(ConnectChecker.check())) {
                successCount += 1;
            }
        }

        assertTrue("maxCount:" + maxCount + " successCount:" + successCount, successCount > 12);
    }

    @Test
    public void testNotConnected() {
        GlobalConfiguration.getInstance().connectCheckURLStrings = new String[]{"https://connect.a.com", "https://connect.a.com"};
        int maxCount = 100;
        int successCount = 0;
        for (int i = 0; i < maxCount; i++) {
            if (ConnectChecker.isConnected(ConnectChecker.check())) {
                successCount += 1;
            }
        }

        assertEquals("maxCount:" + maxCount + " successCount:" + successCount, 0, successCount);
    }
}
