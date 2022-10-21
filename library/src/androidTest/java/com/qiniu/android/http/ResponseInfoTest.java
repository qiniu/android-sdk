package com.qiniu.android.http;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ResponseInfoTest extends BaseTest {

    @Test
    public void testCreate(){

        ResponseInfo responseInfo = null;

        responseInfo = ResponseInfo.zeroSize("");
        assertTrue(responseInfo.statusCode == ResponseInfo.ZeroSizeFile);

        responseInfo = ResponseInfo.cancelled();
        assertTrue(responseInfo.statusCode == ResponseInfo.Cancelled);

        responseInfo = ResponseInfo.invalidArgument("");
        assertTrue(responseInfo.statusCode == ResponseInfo.InvalidArgument);

        responseInfo = ResponseInfo.invalidToken("");
        assertTrue(responseInfo.statusCode == ResponseInfo.InvalidToken);

        responseInfo = ResponseInfo.fileError(null);
        assertTrue(responseInfo.statusCode == ResponseInfo.InvalidFile);

        responseInfo = ResponseInfo.networkError("");
        assertTrue(responseInfo.statusCode == ResponseInfo.NetworkError);
        assertTrue(responseInfo.isNetworkBroken());
        assertTrue(responseInfo.needRetry());

        responseInfo = ResponseInfo.localIOError("");
        assertTrue(responseInfo.statusCode == ResponseInfo.LocalIOError);
    }
}
