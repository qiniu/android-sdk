package com.qiniu.android;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.utils.ContextGetter;
import com.qiniu.android.utils.LogUtil;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UriTest extends BaseTest {

    private static boolean[][] testConfigList = {
            {true, true, true},
            {true, true, false},
            {true, false, true},
            {true, false, false},

            {false, true, true},
            {false, true, false},
            {false, false, true},
            {false, false, false},
    };

    public void testUpload() {
        int MB = 1024;
        int[] sizeList = {512, MB, 4*MB, 5*MB, 8*MB, 10*MB, 20*MB};
        for (int size : sizeList) {
            String fileName = size + "KB" + ".txt";
            Uri uri = writeFileToDownload(size, fileName);
            for (boolean[] config : testConfigList) {
                testUpload(uri, fileName, config[0], config[1], config[2]);
            }
            removeUri(uri);
        }
    }

    private void testUpload(Uri uri, String fileName, boolean isHttps, boolean isResumableV1, boolean isConcurrent) {

        assertNotNull("Uri write file error", uri);

        Configuration configuration = new Configuration.Builder()
                .resumeUploadVersion(isResumableV1 ? Configuration.RESUME_UPLOAD_VERSION_V1 : Configuration.RESUME_UPLOAD_VERSION_V2)
                .useConcurrentResumeUpload(isConcurrent)
                .useHttps(isHttps)
                .build();

        UploadManager uploadManager = new UploadManager(configuration);

        String key = "Uri_Upload_";
        key += isHttps ? "https_" : "http_";
        key += isResumableV1 ? "v1_" : "v2_";
        key += isConcurrent ? "serial_" : "concurrent_";
        key += fileName;
        final UploadCompleteInfo completeInfo = new UploadCompleteInfo();
        uploadManager.put(uri, null, key, TestConfig.token_na0, new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                completeInfo.key = key;
                completeInfo.responseInfo = info;
            }
        }, null);

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                return completeInfo.responseInfo == null;
            }
        }, 10 * 60);

        LogUtil.d("=== upload response key:" + (key != null ? key : "") + " response:" + completeInfo.responseInfo);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo != null);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo.statusCode == ResponseInfo.RequestSuccess);
        assertTrue(completeInfo.responseInfo.toString(), key.equals(completeInfo.key));
    }

    private Uri writeFileToDownload(int size, String fileName) {
        File file = null;
        try {
            file = TempFile.createFile(size);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        ContentResolver resolver = ContextGetter.applicationContext().getContentResolver();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        Uri imageUri = null;
        try {
            imageUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (imageUri != null) {
            // 若生成了uri，则表示该文件添加成功
            // 使用流将内容写入该uri中即可
            OutputStream outputStream = null;
            try {
                outputStream = resolver.openOutputStream(imageUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (outputStream != null) {
                try {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buf)) > 0) {
                        outputStream.write(buf, 0, len);
                    }
                    outputStream.close();
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return imageUri;
    }

    private void removeUri(Uri uri) {
        ContentResolver resolver = ContextGetter.applicationContext().getContentResolver();
        resolver.delete(uri, null, null);
    }

    protected static class UploadCompleteInfo {
        String key;
        ResponseInfo responseInfo;
    }
}
