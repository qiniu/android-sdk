package com.qiniu.android;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;

import java.io.File;
import java.io.IOException;


public class ResumeUploadTest extends UploadFlowTest {


    public void testSwitchRegionV1() {
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V1)
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .build();
        int[] sizeArray = {5000, 8000, 10000, 20000};
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

    public void testCancelV1() {
        float cancelPercent = (float) 0.5;
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V1)
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .build();
        int[] sizeArray = {5000, 8000, 10000, 20000};
        for (int size : sizeArray) {
            String key = "android_resume_cancel_v1_" + size + "k";
            try {
                File file = TempFile.createFile(size, key);
                cancelTest(cancelPercent, file, key, configuration, null);
                TempFile.remove(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void testHttpV1(){
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

    public void testHttpsV1(){
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V1)
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .build();
        int[] sizeArray = {500, 1000, 3000, 4000, 5000, 8000, 10000, 20000};
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

    public void testReuploadV1(){
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V1)
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .chunkSize(1024*1024)
                .build();
        int[] sizeArray = {1000, 3000, 4000, 5000, 8000, 10000, 20000};
        for (int size : sizeArray) {
            String key = "android_resume_reupload_v1_" + size + "k";
            try {
                File file = TempFile.createFile(size, key);
                reuploadUploadTest((float)0.7, file, key, configuration, null);
                TempFile.remove(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void testNoKeyV1(){
        int size = 600;
        String key = "android_resume_reupload_v1_" + size + "k";
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

    public void test0kV1(){
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


    public void testSwitchRegionV2() {
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .useConcurrentResumeUpload(false)
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

    public void testCancelV2() {
        float cancelPercent = (float) 0.5;
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .build();
        int[] sizeArray = {5000, 8000, 10000, 20000};
        for (int size : sizeArray) {
            String key = "android_resume_cancel_v2_" + size + "k";
            try {
                File file = TempFile.createFile(size, key);
                cancelTest(cancelPercent, file, key, configuration, null);
                TempFile.remove(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void testHttpV2(){
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .useConcurrentResumeUpload(false)
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

    public void testHttpsV2(){
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .build();
        int[] sizeArray = {500, 1000, 3000, 4000, 5000, 8000, 10000, 20000};
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

    public void testReuploadV2(){
        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .useConcurrentResumeUpload(false)
                .useHttps(true)
                .chunkSize(1024*1024)
                .build();
        int[] sizeArray = {1000, 3000, 4000, 5000, 8000, 10000, 20000};
        for (int size : sizeArray) {
            String key = "android_resume_reupload_v2_" + size + "k";
            try {
                File file = TempFile.createFile(size, key);
                reuploadUploadTest((float)0.7, file, key, configuration, null);
                TempFile.remove(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void testNoKeyV2(){
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

    public void test0kV2(){
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
}