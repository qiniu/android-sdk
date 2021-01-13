package com.qiniu.android;

import com.qiniu.android.http.connectCheck.ConnectChecker;
import com.qiniu.android.storage.GlobalConfiguration;

public class ConnectCheckTest extends BaseTest {

    public void testCheck() {

        int maxCount = 1000;
        int successCount = 0;
        for (int i = 0; i < maxCount; i++) {
            if (ConnectChecker.check()) {
                successCount += 1;
            }
        }

        assertEquals("maxCount:" + maxCount + " successCount:" + successCount, maxCount, successCount);
    }

    public void testCustomCheckHosts() {
        GlobalConfiguration.getInstance().connectCheckURLStrings = new String[]{"https://www.baidu.com"};
        int maxCount = 1000;
        int successCount = 0;
        for (int i = 0; i < maxCount; i++) {
            if (ConnectChecker.check()) {
                successCount += 1;
            }
        }

        assertEquals("maxCount:" + maxCount + " successCount:" + successCount, maxCount, successCount);
    }

    public void testNotConnected() {
        GlobalConfiguration.getInstance().connectCheckURLStrings = new String[]{"https://www.test1.com", "https://www.test2.com"};
        int maxCount = 1000;
        int successCount = 0;
        for (int i = 0; i < maxCount; i++) {
            if (ConnectChecker.check()) {
                successCount += 1;
            }
        }

        assertEquals("maxCount:" + maxCount + " successCount:" + successCount, 0, successCount);
    }
}
