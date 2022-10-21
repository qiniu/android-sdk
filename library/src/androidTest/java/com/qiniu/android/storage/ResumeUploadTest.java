package com.qiniu.android.storage;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.TempFile;
import com.qiniu.android.http.ResponseInfo;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class ResumeUploadTest extends UploadFlowTest {

    @Test
    public void testSwitchRegionV1() {
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V1)
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .build();
        int[] sizeArray = {1000, 5000, 8000, 10000, 20000};
        for (int size : sizeArray) {
            String key = "android_resume_switch_region_v1_" + size + "k";
            try {
                File file = TempFile.createFile(size, key);
                switchRegionTestWithFile(file, key, configuration, null);
                TempFile.remove(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testCancelV1() {
        float cancelPercent = (float) 0.5;
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V1)
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .build();
        int[] sizeArray = {10000, 20000};
        for (int size : sizeArray) {
            String key = "android_resume_cancel_v1_" + size + "k";
            try {
                File file = TempFile.createFile(size, key);
                cancelTest((long) (size * cancelPercent), file, key, configuration, null);
                TempFile.remove(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testHttpV1() {
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V1)
                .useConcurrentResumeUpload(false)
                .useHttps(false)
                .build();
        int[] sizeArray = {500, 1000, 3000, 4000, 5000, 8000, 10000, 20000};
        for (int size : sizeArray) {
            String key = "android_resume_http_v1_" + size + "k";
            try {
                File file = TempFile.createFile(size, key);
                uploadFileAndAssertSuccessResult(file, key, configuration, null);
                TempFile.remove(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testHttpsV1() {
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V1)
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .build();
        int[] sizeArray = {500, 3000, 4000, 7000, 10000, 20000};
        for (int size : sizeArray) {
            String key = "android_resume_https_v1_" + size + "k";
            try {
                File file = TempFile.createFile(size, key);
                uploadFileAndAssertSuccessResult(file, key, configuration, null);
                TempFile.remove(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testReuploadV1() {
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V1)
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .chunkSize(1024 * 1024)
                .build();
        int[] sizeArray = {30000};
        for (int size : sizeArray) {
            String key = "android_resume_reupload_v1_" + size + "k";
            try {
                File file = TempFile.createFile(size, key);
                reuploadUploadTest((long) (size * 0.5), file, key, configuration, null);
                TempFile.remove(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testNoKeyV1() {
        int size = 600;
        String key = "android_resume_no_key_v1_" + size + "k";
        File file = null;
        try {
            file = TempFile.createFile(size, key);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Configuration configurationHttp = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V1)
                .useConcurrentResumeUpload(false)
                .useHttps(false)
                .build();
        uploadFileAndAssertSuccessResult(file, null, configurationHttp, null);

        Configuration configurationHttps = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V1)
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .build();
        uploadFileAndAssertSuccessResult(file, null, configurationHttps, null);

        TempFile.remove(file);
    }

    @Test
    public void test0kV1() {
        int size = 0;
        String key = "android_resume_0k_v1_" + size + "k";
        File file = null;
        try {
            file = TempFile.createFile(size, key);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Configuration configurationHttp = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V1)
                .useConcurrentResumeUpload(false)
                .useHttps(false)
                .build();
        uploadFileAndAssertResult(ResponseInfo.ZeroSizeFile, file, key, configurationHttp, null);

        Configuration configurationHttps = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V1)
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .build();
        uploadFileAndAssertResult(ResponseInfo.ZeroSizeFile, file, key, configurationHttps, null);

        TempFile.remove(file);
    }

    @Test
    public void testCustomParamV1() {

        Map<String, String> userParam = new HashMap<>();
        userParam.put("foo", "foo_value");
        userParam.put("bar", "bar_value");

        Map<String, String> metaParam = new HashMap<>();
        metaParam.put("0000", "meta_value_0");
        metaParam.put("x-qn-meta-aaa", "meta_value_1");
        metaParam.put("x-qn-meta-key-2", "meta_value_2");

        UploadOptions options = new UploadOptions(userParam, metaParam, null, true, null, null, null);

        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V1)
                .useConcurrentResumeUpload(false)
                .useHttps(false)
                .build();

        String key = "android_resume_custom_param_v1";
        File file = null;
        try {
            file = TempFile.createFile(5 * 1024, key);
        } catch (IOException e) {
            e.printStackTrace();
        }

        uploadFileAndAssertSuccessResult(file, key, configuration, options);
    }

    @Test
    public void testSwitchRegionV2() {
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .useConcurrentResumeUpload(false)
                .chunkSize(4 * 1024 * 1024)
                .useHttps(true)
                .build();
        int[] sizeArray = {5000, 8000, 10000, 20000};
        for (int size : sizeArray) {
            String key = "android_resume_switch_region_v2_" + size + "k";
            try {
                File file = TempFile.createFile(size, key);
                switchRegionTestWithFile(file, key, configuration, null);
                TempFile.remove(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testCancelV2() {
        float cancelPercent = (float) 0.5;
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .useConcurrentResumeUpload(false)
                .chunkSize(4 * 1024 * 1024)
                .useHttps(true)
                .build();
        int[] sizeArray = {10000, 20000};
        for (int size : sizeArray) {
            String key = "android_resume_cancel_v2_" + size + "k";
            try {
                File file = TempFile.createFile(size, key);
                cancelTest((long) (size * cancelPercent), file, key, configuration, null);
                TempFile.remove(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testHttpV2() {
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .useConcurrentResumeUpload(false)
                .chunkSize(4 * 1024 * 1024)
                .useHttps(false)
                .build();
        int[] sizeArray = {500, 1000, 3000, 4000, 5000, 8000, 10000, 20000};
        for (int size : sizeArray) {
            String key = "android_resume_http_v2_" + size + "k";
            try {
                File file = TempFile.createFile(size, key);
                uploadFileAndAssertSuccessResult(file, key, configuration, null);
                TempFile.remove(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testHttpsV2() {
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .useConcurrentResumeUpload(false)
                .chunkSize(4 * 1024 * 1024)
                .useHttps(true)
                .build();
        int[] sizeArray = {500, 3000, 4000, 7000, 10000, 20000};
        for (int size : sizeArray) {
            String key = "android_resume_https_v2_" + size + "k";
            try {
                File file = TempFile.createFile(size, key);
                uploadFileAndAssertSuccessResult(file, key, configuration, null);
                TempFile.remove(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testReuploadV2() {
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .chunkSize(4 * 1024 * 1024)
                .build();
        int[] sizeArray = {30000};
        for (int size : sizeArray) {
            String key = "android_resume_reupload_v2_" + size + "k";
            try {
                File file = TempFile.createFile(size, key);
                reuploadUploadTest((long) (size * 0.7), file, key, configuration, null);
                TempFile.remove(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testNoKeyV2() {
        int size = 600;
        String key = "android_resume_reupload_v2_" + size + "k";
        File file = null;
        try {
            file = TempFile.createFile(size, key);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Configuration configurationHttp = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .useConcurrentResumeUpload(false)
                .useHttps(false)
                .build();
        uploadFileAndAssertSuccessResult(file, null, configurationHttp, null);

        Configuration configurationHttps = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .build();
        uploadFileAndAssertSuccessResult(file, null, configurationHttps, null);

        TempFile.remove(file);
    }

    @Test
    public void test0kV2() {
        int size = 0;
        String key = "android_resume_0k_v2_" + size + "k";
        File file = null;
        try {
            file = TempFile.createFile(size, key);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Configuration configurationHttp = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .useConcurrentResumeUpload(false)
                .useHttps(false)
                .build();
        uploadFileAndAssertResult(ResponseInfo.ZeroSizeFile, file, key, configurationHttp, null);

        Configuration configurationHttps = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .build();
        uploadFileAndAssertResult(ResponseInfo.ZeroSizeFile, file, key, configurationHttps, null);

        TempFile.remove(file);
    }

    @Test
    public void testCustomParamV2() {

        Map<String, String> userParam = new HashMap<>();
        userParam.put("foo", "foo_value");
        userParam.put("bar", "bar_value");

        Map<String, String> metaParam = new HashMap<>();
        metaParam.put("0000", "meta_value_0");
        metaParam.put("x-qn-meta-aaa", "meta_value_1");
        metaParam.put("x-qn-meta-key-2", "meta_value_2");

        UploadOptions options = new UploadOptions(userParam, metaParam, null, true, null, null, null);

        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .useConcurrentResumeUpload(false)
                .chunkSize(4 * 1024 * 1024)
                .useHttps(false)
                .build();

        String key = "android_resume_custom_param_v2";
        File file = null;
        try {
            file = TempFile.createFile(5 * 1024, key);
        } catch (IOException e) {
            e.printStackTrace();
        }

        uploadFileAndAssertSuccessResult(file, key, configuration, options);
    }
}