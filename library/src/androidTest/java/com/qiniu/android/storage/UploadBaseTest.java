package com.qiniu.android.storage;

import android.net.Uri;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;
import com.qiniu.android.TestConfig;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.utils.Etag;
import com.qiniu.android.utils.LogUtil;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class UploadBaseTest extends BaseTest {

    @Override
    @Before
    public void setUp() throws Exception {
        LogUtil.enableLog(true);
        LogUtil.setLogLevel(Log.VERBOSE);
    }

    protected UploadOptions defaultOptions = new UploadOptions(null, null, true, new UpProgressBytesHandler() {
        @Override
        public void progress(String key, long uploadBytes, long totalBytes) {
            double percent = 0;
            if (totalBytes > 0) {
                percent = (double) uploadBytes / (double) totalBytes;
            }
            LogUtil.d("== upload key:" + (key == null ? "" : key) + " uploadBytes:" + uploadBytes + " totalBytes:" + totalBytes + " percent:" + percent);
        }

        @Override
        public void progress(String key, double percent) {
            LogUtil.d("== upload key:" + (key == null ? "" : key) + " progress:" + percent);
        }
    }, null);

    protected boolean verifyUploadKey(String upKey, String responseKey) {
        LogUtil.d("== upKey:" + (upKey != null ? upKey : "")
                + " responseKey:" + (responseKey != null ? responseKey : ""));
        if (upKey == null) {
            return responseKey == null;
        }
        if (responseKey == null) {
            return false;
        }
        return upKey.equals(responseKey);
    }

    protected void uploadFileAndAssertSuccessResult(File file,
                                                    String key,
                                                    Configuration configuration,
                                                    UploadOptions options) {
        uploadFileAndAssertResult(ResponseInfo.RequestSuccess, file, key, configuration, options);
    }

    protected void uploadFileAndAssertSuccessResult(UploadInfo file,
                                                    String key,
                                                    Configuration configuration,
                                                    UploadOptions options) {
        uploadFileAndAssertResult(ResponseInfo.RequestSuccess, file, key, configuration, options);
    }

    protected void uploadFileAndAssertResult(int statusCode,
                                             File file,
                                             String key,
                                             Configuration configuration,
                                             UploadOptions options) {

        uploadFileAndAssertResult(statusCode, file, TestConfig.token_na0, key, configuration, options);
    }

    protected void uploadFileAndAssertResult(int statusCode,
                                             UploadInfo file,
                                             String key,
                                             Configuration configuration,
                                             UploadOptions options) {

        uploadFileAndAssertResult(statusCode, file, TestConfig.token_na0, key, configuration, options);
    }

    protected void uploadFileAndAssertResult(int statusCode,
                                             File file,
                                             String token,
                                             String key,
                                             Configuration configuration,
                                             UploadOptions options) {

        UploadInfo<File> fileInfo = new UploadInfo<>(file);
        fileInfo.configWithFile(file);
        uploadFileAndAssertResult(statusCode, fileInfo, token, key, configuration, options);

        Uri uri = Uri.fromFile(file);
        UploadInfo<Uri> uriInfo = new UploadInfo<>(uri);
        uriInfo.configWithFile(file);
        uploadFileAndAssertResult(statusCode, uriInfo, token, key, configuration, options);

        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
        }
        UploadInfo<InputStream> streamInfo = new UploadInfo<>(stream);
        streamInfo.configWithFile(file);
        uploadFileAndAssertResult(statusCode, streamInfo, token, key, configuration, options);
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }

        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
        }
        streamInfo = new UploadInfo<>(stream);
        streamInfo.configWithFile(file);
        streamInfo.size = -1;
        uploadFileAndAssertResult(statusCode, streamInfo, token, key, configuration, options);
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }

        if (file.length() < 4 * 1024 * 1024) {
            byte[] data = getDataFromFile(file);
            UploadInfo<byte[]> dataInfo = new UploadInfo<>(data);
            dataInfo.configWithFile(file);
            uploadFileAndAssertResult(statusCode, dataInfo, token, key, configuration, options);
        }
    }

    protected void uploadFileAndAssertResult(int statusCode,
                                             UploadInfo file,
                                             String token,
                                             String key,
                                             Configuration configuration,
                                             UploadOptions options) {

        final UploadCompleteInfo completeInfo = new UploadCompleteInfo();
        upload(file, token, key, configuration, options, new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                completeInfo.responseInfo = info;
                completeInfo.key = key;
            }
        });

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                return completeInfo.responseInfo == null;
            }
        }, 10 * 60);

        LogUtil.d("=== upload file type:" + file.type() + " response key:" + (key != null ? key : "") + " response:" + completeInfo.responseInfo);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo != null);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo.statusCode == statusCode);
        assertTrue(completeInfo.responseInfo.toString(), verifyUploadKey(key, completeInfo.key));

        // 成功验证 etag
        if (statusCode == ResponseInfo.RequestSuccess) {
            String etag = file.etag;
            String serverEtag = null;
            try {
                serverEtag = completeInfo.responseInfo.response.getString("hash");
            } catch (Exception e) {
                e.printStackTrace();
            }
            LogUtil.d("=== upload etag:" + etag + " response etag:" + serverEtag);
            assertEquals("file:" + etag + " server etag:" + serverEtag, etag, serverEtag);
        }
    }

    protected void upload(UploadInfo file,
                          String key,
                          Configuration configuration,
                          UploadOptions options,
                          UpCompletionHandler completionHandler) {

        upload(file, TestConfig.token_na0, key, configuration, options, completionHandler);
    }

    protected void upload(UploadInfo file,
                          String token,
                          String key,
                          Configuration configuration,
                          UploadOptions options,
                          UpCompletionHandler completionHandler) {
        if (options == null) {
            options = defaultOptions;
        }
        UploadManager manager = new UploadManager(configuration);
        if (file.info instanceof File) {
            manager.put((File) file.info, key, token, completionHandler, options);
        } else if (file.info instanceof Uri) {
            manager.put((Uri) file.info, getContext().getContentResolver(), key, token, completionHandler, options);
        } else if (file.info instanceof InputStream) {
            manager.put((InputStream) file.info, null, file.size, file.fileName, key, token, completionHandler, options);
        } else if (file.info instanceof byte[]) {
            manager.put((byte[]) file.info, key, token, completionHandler, options);
        } else {
            completionHandler.complete(key, ResponseInfo.fileError(new Exception("test case file type error")), null);
        }
    }

    protected byte[] getDataFromFile(File file) {
        byte[] bytes = new byte[(int) file.length()];
        try {
            RandomAccessFile accessFile = new RandomAccessFile(file, "r");
            accessFile.readFully(bytes);
        } catch (Exception e) {
            bytes = null;
        }
        return bytes;
    }

    protected static class UploadInfo<T> {
        protected final T info;
        protected String fileName;
        protected long size = -1;
        protected String etag;
        protected String md5;

        public UploadInfo(T info) {
            this.info = info;
        }

        public void configWithFile(File file) {
            fileName = file.getName();
            size = file.length();
            try {
                etag = Etag.file(file);
            } catch (Exception ignore) {
            }

            try {
                etag = Etag.file(file);
            } catch (Exception ignore) {
            }
        }

        public String type() {
            if (info instanceof File) {
                return "file";
            } else if (info instanceof Uri) {
                return "uri";
            } else if (info instanceof InputStream) {
                return "stream";
            } else if (info instanceof byte[]) {
                return "byte_array";
            } else {
                return "none";
            }
        }
    }

    protected static class UploadCompleteInfo {
        String key;
        ResponseInfo responseInfo;
    }
}
