package com.qiniu.android;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.UrlConverter;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UploadOptions;

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
            byte[] data = TempFile.getByte(size);
            switchRegionTestWithData(data, key, configuration, null);
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
            byte[] data = TempFile.getByte(size*1024);
            cancelTest(cancelPercent, data, key, configuration, null);
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
            byte[] data = TempFile.getByte(size);
            uploadDataAndAssertSuccessResult(data, key, configuration, null);
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
            byte[] data = TempFile.getByte(size);
            uploadDataAndAssertSuccessResult(data, key, configuration, null);
        }
    }

    public void testSmall() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "foo");
        params.put("x:bar", "bar");
        //mime type
        final String mimeType = "text/plain";
        final UploadOptions options = new UploadOptions(params, mimeType, true, null, null);
        byte[] data = "Hello, World!".getBytes();
        uploadDataAndAssertSuccessResult(data, "android_你好", null, options);
    }


    public void test100up() {
        int count = 100;
        for (int i = 1; i < count; i++) {
            String key = "android_form_100_up_" + i + "k";
            byte[] data = TempFile.getByte(i * 1024);
            uploadDataAndAssertSuccessResult(data, key, null, null);
        }
    }

    public void testUpUnAuth() {
        byte[] data = "Hello, World!".getBytes();
        uploadDataAndAssertResult(ResponseInfo.InvalidToken, data, "noAuth", "android_form_no_auth", null, null);
    }

    public void testNoData() {
        uploadDataAndAssertResult(ResponseInfo.ZeroSizeFile, null, "android_form_no_data", null, null);
    }

    public void testNoFile() {
        uploadFileAndAssertResult(ResponseInfo.ZeroSizeFile, null, "android_form_no_file", null, null);
    }

    public void testNoToken() {
        String key = "android_form_no_token";
        File file = null;
        byte[] data = null;
        try {
            file = TempFile.createFile(5 * 1024, key);
            data = TempFile.getByte(5 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }

        uploadDataAndAssertResult(ResponseInfo.InvalidToken, data, null, key, null, null);
        uploadFileAndAssertResult(ResponseInfo.InvalidToken, file, null, key, null, null);

        uploadDataAndAssertResult(ResponseInfo.InvalidToken, data, "", key, null, null);
        uploadFileAndAssertResult(ResponseInfo.InvalidToken, file, "", key, null, null);

        uploadDataAndAssertResult(ResponseInfo.InvalidToken, data, "ABC", key, null, null);
        uploadFileAndAssertResult(ResponseInfo.InvalidToken, file, "ABC", key, null, null);

        TempFile.remove(file);
    }

    public void testNoKey() {
        String key = "android_form_no_key";
        File file = null;
        byte[] data = null;
        try {
            file = TempFile.createFile(5 * 1024, key);
            data = TempFile.getByte(5 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }

        uploadDataAndAssertSuccessResult(data, null, null, null);
        uploadFileAndAssertSuccessResult(file, null, null, null);

        TempFile.remove(file);
    }

    public void testUrlConvert() {
        String dataKey = "android_form_url_convert_data";
        String fileKey = "android_form_url_convert_file";
        File file = null;
        byte[] data = null;
        try {
            file = TempFile.createFile(5 * 1024, fileKey);
            data = TempFile.getByte(5 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Configuration configuration = new Configuration.Builder()
                .useHttps(false)
                .zone(new FixedZone(new String[]{"upnono-na0.qiniu.com", "upnono-na0.qiniu.com"}))
                .urlConverter(new UrlConverter() {
                    @Override
                    public String convert(String url) {
                        return  url.replace("upnono", "up");
                    }
                })
                .build();
        uploadDataAndAssertSuccessResult(data, dataKey, configuration, null);
        uploadFileAndAssertSuccessResult(file, fileKey, configuration, null);
    }
}
