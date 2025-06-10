package com.qiniu.android.storage;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ConfigurationTest extends BaseTest {

    @Test
    public void testDefaultValue() {
        Configuration cfg = new Configuration.Builder().build();
        assertEquals("Configuration build: resumeUploadVersion default value error", cfg.resumeUploadVersion, Configuration.RESUME_UPLOAD_VERSION_V1);

        cfg = new Configuration.Builder().buildV2();
        assertEquals("Configuration buildV2: resumeUploadVersion default value error", cfg.resumeUploadVersion, Configuration.RESUME_UPLOAD_VERSION_V2);
    }
}
