package com.qiniu.android;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.UrlConverter;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UploadOptions;

import junit.framework.Assert;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FormUploadTest extends UploadFlowTest {

    public void testSwitchRegion() {
        Configuration configuration = new Configuration.Builder()
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .build();
        int[] sizeArray = {5, 50, 200, 500, 800, 1000, 2000, 3000, 4000};
        for (int size : sizeArray) {
            String key = "android_form_switch_region_" + size + "k";
            File file = null;
            try {
                file = TempFile.createFile(size, key);
            } catch (IOException e) {
                Assert.assertTrue(e.getMessage(), false);
            }
            switchRegionTestWithFile(file, key, configuration, null);
        }
    }

    public void testCancel() {
        float cancelPercent = (float) 0.2;
        Configuration configuration = new Configuration.Builder()
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .build();
        int[] sizeArray = {2000, 3000, 4000};
        for (int size : sizeArray) {
            String key = "android_form_cancel_" + size + "k";
            File file = null;
            try {
                file = TempFile.createFile(size, key);
            } catch (IOException e) {
                Assert.assertTrue(e.getMessage(), false);
            }
            cancelTest(cancelPercent, file, key, configuration, null);
        }
    }

    public void testHttpV1() {
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V1)
                .useConcurrentResumeUpload(true)
                .useHttps(false)
                .build();
        int[] sizeArray = {500, 1000, 3000, 4000, 5000, 8000, 10000, 20000};
        for (int size : sizeArray) {
            String key = "android_form_http_v1_" + size + "k";
            File file = null;
            try {
                file = TempFile.createFile(size, key);
            } catch (IOException e) {
                Assert.assertTrue(e.getMessage(), false);
            }
            uploadFileAndAssertSuccessResult(file, key, configuration, null);
        }
    }

    public void testHttpsV1() {
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V1)
                .useConcurrentResumeUpload(true)
                .useHttps(true)
                .build();
        int[] sizeArray = {500, 1000, 3000, 4000, 5000, 8000, 10000, 20000};
        for (int size : sizeArray) {
            String key = "android_form_https_v1_" + size + "k";
            File file = null;
            try {
                file = TempFile.createFile(size, key);
            } catch (IOException e) {
                Assert.assertTrue(e.getMessage(), false);
            }
            uploadFileAndAssertSuccessResult(file, key, configuration, null);
        }
    }

    public void testSmall() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "foo");
        params.put("x:bar", "bar");
        //mime type
        final String mimeType = "text/plain";
        final UploadOptions options = new UploadOptions(params, mimeType, true, null, null);

        String key = "android_small";
        File file = null;
        try {
            file = TempFile.createFile(10, key);
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage(), false);
        }
        uploadFileAndAssertSuccessResult(file, key, null, options);
    }


    public void test100up() {
        int count = 100;
        for (int i = 1; i < count; i++) {
            String key = "android_form_100_up_" + i + "k";
            File file = null;
            try {
                file = TempFile.createFile(10, key);
            } catch (IOException e) {
                Assert.assertTrue(e.getMessage(), false);
            }
            uploadFileAndAssertSuccessResult(file, key, null, null);
        }
    }

    public void testUpUnAuth() {
        String key = "android_unAuth";
        File file = null;
        try {
            file = TempFile.createFile(10, key);
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage(), false);
        }
        uploadFileAndAssertResult(ResponseInfo.InvalidToken, file, "noAuth", "android_form_no_auth", null, null);
    }

    public void testNoData() {
        String key = "android_noData";
        File file = null;
        try {
            file = TempFile.createFile(0, key);
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage(), false);
        }
        uploadFileAndAssertResult(ResponseInfo.ZeroSizeFile, file, key, null, null);
    }

    public void testNoToken() {
        String key = "android_form_no_token";
        File file = null;
        try {
            file = TempFile.createFile(5 * 1024, key);
        } catch (IOException e) {
            e.printStackTrace();
        }

        uploadFileAndAssertResult(ResponseInfo.InvalidToken, file, null, key, null, null);

        uploadFileAndAssertResult(ResponseInfo.InvalidToken, file, "", key, null, null);

        uploadFileAndAssertResult(ResponseInfo.InvalidToken, file, "ABC", key, null, null);

        TempFile.remove(file);
    }

    public void testNoKey() {
        String key = "android_form_no_key";
        File file = null;
        try {
            file = TempFile.createFile(5 * 1024, key);
        } catch (IOException e) {
            e.printStackTrace();
        }

        uploadFileAndAssertSuccessResult(file, null, null, null);

        TempFile.remove(file);
    }

    public void testUrlConvert() {
        String fileKey = "android_form_url_convert_file_new";
        File file = null;
        try {
            file = TempFile.createFile(5, fileKey);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Configuration configuration = new Configuration.Builder()
                .useHttps(false)
                .zone(new FixedZone(new String[]{"upnono-na0.qiniu.com", "upnono-na0.qiniu.com"}))
                .urlConverter(new UrlConverter() {
                    @Override
                    public String convert(String url) {
                        return url.replace("upnono", "up");
                    }
                })
                .build();
        uploadFileAndAssertSuccessResult(file, fileKey, configuration, null);
    }

    public void testCustomParam() {

        Map<String, String> userParam = new HashMap<>();
        userParam.put("foo", "foo_value");
        userParam.put("bar", "bar_value");

        Map<String, String> metaParam = new HashMap<>();
        metaParam.put("0000", "meta_value_0");
        metaParam.put("x-qn-meta-aaa", "meta_value_1");
        metaParam.put("x-qn-meta-key-2", "meta_value_2");

        UploadOptions options = new UploadOptions(userParam, metaParam, null, true, null, null, null);

        String fileKey = "android_form_custom_param_file";
        File file = null;
        try {
            file = TempFile.createFile(5, fileKey);
        } catch (IOException e) {
            e.printStackTrace();
        }
        uploadFileAndAssertSuccessResult(file, fileKey, null, options);
    }

}
