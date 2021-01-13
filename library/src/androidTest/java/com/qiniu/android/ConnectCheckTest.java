package com.qiniu.android;

import com.qiniu.android.http.connectCheck.ConnectChecker;

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
}
