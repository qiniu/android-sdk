package com.qiniu.test;

import java.io.IOException;

import junit.framework.Assert;

import com.qiniu.conf.Conf;
import com.qiniu.utils.QiniuException;
import com.qiniu.utils.RetryRet;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

public class RetryTest extends AndroidTestCase{
    @SmallTest
    public void testRetryCheck()  {
        QiniuException ioe = new QiniuException(QiniuException.IO, "io test", new IOException("test"));
        Assert.assertFalse(RetryRet.noRetry(ioe));
    }
}
