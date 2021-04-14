package com.qiniu.android;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.Etag;
import com.qiniu.android.utils.LogUtil;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class UploadBaseTest extends BaseTest {

    protected UploadOptions defaultOptions = new UploadOptions(null, null, true, new UpProgressHandler() {
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

        final UploadCompleteInfo completeInfo = new UploadCompleteInfo();
        uploadFile(file, key, configuration, options, new UpCompletionHandler() {
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
        }, 5 * 60);

        LogUtil.d("=== upload response key:" + (key != null ? key : "") + " response:" + completeInfo.responseInfo);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo != null);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo.isOK());
        assertTrue(completeInfo.responseInfo.toString(), verifyUploadKey(key, completeInfo.key));

        // 成功验证 etag
        String etag = null;
        String serverEtag = null;
        try {
            etag = Etag.file(file);
            serverEtag = completeInfo.responseInfo.response.getString("hash");
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogUtil.d("=== upload etag:" + etag + " response etag:" + serverEtag);
        assertEquals("file:" + etag + " server etag:" + serverEtag, etag, serverEtag);
    }

    protected void uploadFileAndAssertResult(int statusCode,
                                             File file,
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

        final UploadCompleteInfo completeInfo = new UploadCompleteInfo();
        uploadFile(file, token, key, configuration, options, new UpCompletionHandler() {
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
        }, 5 * 60);

        LogUtil.d("=== upload response key:" + (key != null ? key : "") + " response:" + completeInfo.responseInfo);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo != null);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo.statusCode == statusCode);
        assertTrue(completeInfo.responseInfo.toString(), verifyUploadKey(key, completeInfo.key));

        // 成功验证 etag
        if (statusCode == 200) {
            String etag = null;
            String serverEtag = null;
            try {
                etag = Etag.file(file);
                serverEtag = completeInfo.responseInfo.response.getString("hash");
            } catch (Exception e) {
                e.printStackTrace();
            }
            LogUtil.d("=== upload etag:" + etag + " response etag:" + serverEtag);
            assertEquals("file:" + etag + " server etag:" + serverEtag, etag, serverEtag);
        }
    }

    protected void uploadFile(File file,
                              String key,
                              Configuration configuration,
                              UploadOptions options,
                              UpCompletionHandler completionHandler) {

        uploadFile(file, TestConfig.token_na0, key, configuration, options, completionHandler);
    }

    protected void uploadFile(File file,
                              String token,
                              String key,
                              Configuration configuration,
                              UploadOptions options,
                              UpCompletionHandler completionHandler) {
        if (options == null) {
            options = defaultOptions;
        }
        UploadManager manager = new UploadManager(configuration);
        manager.put(file, key, token, completionHandler, options);
    }


    protected void uploadDataAndAssertSuccessResult(byte[] data,
                                                    String key,
                                                    Configuration configuration,
                                                    UploadOptions options) {

        final UploadCompleteInfo completeInfo = new UploadCompleteInfo();
        uploadData(data, key, configuration, options, new UpCompletionHandler() {
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
        }, 5 * 60);

        LogUtil.d("=== upload response key:" + (key != null ? key : "") + " response:" + completeInfo.responseInfo);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo != null);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo.isOK());
        assertTrue(completeInfo.responseInfo.toString(), verifyUploadKey(key, completeInfo.key));

        // 成功验证 etag
        String etag = null;
        String serverEtag = null;
        try {
            etag = Etag.data(data);
            serverEtag = completeInfo.responseInfo.response.getString("hash");
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogUtil.d("=== upload etag:" + etag + " response etag:" + serverEtag);
        assertEquals("file:" + etag + " server etag:" + serverEtag, etag, serverEtag);
    }

    protected void uploadDataAndAssertResult(int statusCode,
                                             byte[] data,
                                             String key,
                                             Configuration configuration,
                                             UploadOptions options) {

        uploadDataAndAssertResult(statusCode, data, TestConfig.token_na0, key, configuration, options);
    }

    protected void uploadDataAndAssertResult(int statusCode,
                                             byte[] data,
                                             String token,
                                             String key,
                                             Configuration configuration,
                                             UploadOptions options) {
        final UploadCompleteInfo completeInfo = new UploadCompleteInfo();
        uploadData(data, token, key, configuration, options, new UpCompletionHandler() {
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
        }, 5 * 60);

        LogUtil.d("=== upload response key:" + (key != null ? key : "") + " response:" + completeInfo.responseInfo);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo != null);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo.statusCode == statusCode);
        assertTrue(completeInfo.responseInfo.toString(), verifyUploadKey(key, completeInfo.key));

        // 成功验证 etag
        if (statusCode == 200) {
            String etag = null;
            String serverEtag = null;
            try {
                etag = Etag.data(data);
                serverEtag = completeInfo.responseInfo.response.getString("hash");
            } catch (Exception e) {
                e.printStackTrace();
            }
            LogUtil.d("=== upload etag:" + etag + " response etag:" + serverEtag);
            assertEquals("file:" + etag + " server etag:" + serverEtag, etag, serverEtag);
        }
    }

    protected void uploadData(byte[] data,
                              String key,
                              Configuration configuration,
                              UploadOptions options,
                              UpCompletionHandler completionHandler) {

        uploadData(data, TestConfig.token_na0, key, configuration, options, completionHandler);
    }

    protected void uploadData(byte[] data,
                              String token,
                              String key,
                              Configuration configuration,
                              UploadOptions options,
                              UpCompletionHandler completionHandler) {
        if (options == null) {
            options = defaultOptions;
        }
        UploadManager manager = new UploadManager(configuration);
        manager.put(data, key, token, completionHandler, options);
    }


    protected static class UploadCompleteInfo {
        String key;
        ResponseInfo responseInfo;
    }
}
