package com.qiniu.android;

import android.test.AndroidTestCase;

import com.qiniu.android.bigdata.Configuration;

public class BigDataConfigurationTest extends AndroidTestCase {

    public void testCopy(){
        Configuration configuration = new Configuration();
        configuration.connectTimeout = 15;

        Configuration configurationCopy = Configuration.copy(configuration);

        assertTrue(configurationCopy.connectTimeout == configuration.connectTimeout);
    }

    public void testCopyNoValue(){
        Configuration configuration = null;

        Configuration configurationCopy = Configuration.copy(configuration);

        assertTrue(configurationCopy != null);
    }
}
