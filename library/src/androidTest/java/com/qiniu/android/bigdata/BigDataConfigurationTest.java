package com.qiniu.android.bigdata;


import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BigDataConfigurationTest {

    @Test
    public void testCopy() {
        Configuration configuration = new Configuration();
        configuration.connectTimeout = 15;

        Configuration configurationCopy = Configuration.copy(configuration);

        assertTrue(configurationCopy.connectTimeout == configuration.connectTimeout);
    }

    @Test
    public void testCopyNoValue() {
        Configuration configuration = null;

        Configuration configurationCopy = Configuration.copy(configuration);

        assertTrue(configurationCopy != null);
    }
}
