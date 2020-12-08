package com.qiniu.android;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.common.ZonesInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadOptions;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class UploadFlowTest extends UploadBaseTest {

    protected void cancelTest(final float cancelPercent,
                              File file,
                              String key,
                              Configuration configuration,
                              UploadOptions options) {
        if (options == null) {
            options = UploadOptions.defaultOptions();
        }

        final UploadOptions optionsReal = options;
        final Flags flags = new Flags();
        UploadOptions cancelOptions = new UploadOptions(optionsReal.params, optionsReal.mimeType, optionsReal.checkCrc, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                if (cancelPercent >= percent) {
                    flags.shouldCancel = true;
                }
            }
        }, new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {

                return flags.shouldCancel;
            }
        }, options.netReadyHandler);

        final UploadCompleteInfo completeInfo = new UploadCompleteInfo();
        uploadFile(file, key, configuration, cancelOptions, new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                completeInfo.key = key;
                completeInfo.responseInfo = info;
            }
        });

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                return completeInfo.responseInfo == null;
            }
        }, 5 * 60);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo != null);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo.isCancelled());
        assertTrue(completeInfo.responseInfo.toString(), verifyUploadKey(key, completeInfo.key));
    }

    protected void cancelTest(final float cancelPercent,
                              byte[] data,
                              String key,
                              Configuration configuration,
                              UploadOptions options) {

        if (options == null) {
            options = UploadOptions.defaultOptions();
        }

        final UploadOptions optionsReal = options;
        final Flags flags = new Flags();
        UploadOptions cancelOptions = new UploadOptions(optionsReal.params, optionsReal.mimeType, optionsReal.checkCrc, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                if (cancelPercent >= percent) {
                    flags.shouldCancel = true;
                }
                if (optionsReal.progressHandler != null) {
                    optionsReal.progressHandler.progress(key, percent);
                }
            }
        }, new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {

                return flags.shouldCancel;
            }
        }, options.netReadyHandler);

        final UploadCompleteInfo completeInfo = new UploadCompleteInfo();
        uploadData(data, key, configuration, cancelOptions, new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                completeInfo.key = key;
                completeInfo.responseInfo = info;
            }
        });

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                return completeInfo.responseInfo == null;
            }
        }, 5 * 60);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo != null);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo.isCancelled());
        assertTrue(completeInfo.responseInfo.toString(), verifyUploadKey(key, completeInfo.key));
    }


    protected void reuploadUploadTest(final float resumePercent,
                                      final File file,
                                      String key,
                                      final Configuration configuration,
                                      UploadOptions options) {
        if (options == null) {
            options = UploadOptions.defaultOptions();
        }
        final UploadOptions optionsReal = options;

        cancelTest(resumePercent, file, key, configuration, optionsReal);


        final Flags flags = new Flags();
        UploadOptions cancelOptions = new UploadOptions(optionsReal.params, optionsReal.mimeType, optionsReal.checkCrc, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                double minPercent = 0;
                if (configuration.useConcurrentResumeUpload) {
                    minPercent = percent - (configuration.chunkSize) * configuration.concurrentTaskCount / file.length();
                } else {
                    minPercent = percent - configuration.chunkSize / file.length();
                }
                if (resumePercent >= minPercent) {
                    flags.isSuccess = true;
                }
                if (optionsReal.progressHandler != null) {
                    optionsReal.progressHandler.progress(key, percent);
                }
            }
        }, new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {

                return flags.shouldCancel;
            }
        }, options.netReadyHandler);

        final UploadCompleteInfo completeInfo = new UploadCompleteInfo();
        uploadFile(file, key, configuration, cancelOptions, new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                completeInfo.key = key;
                completeInfo.responseInfo = info;
            }
        });

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                return completeInfo.responseInfo == null;
            }
        }, 5 * 60);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo != null);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo.isOK());
        assertTrue(completeInfo.responseInfo.toString(), verifyUploadKey(key, completeInfo.key));
    }

    protected void reuploadUploadTest(final float resumePercent,
                                      final byte[] data,
                                      String key,
                                      final Configuration configuration,
                                      UploadOptions options) {
        if (options == null) {
            options = UploadOptions.defaultOptions();
        }
        final UploadOptions optionsReal = options;

        cancelTest(resumePercent, data, key, configuration, optionsReal);

        final Flags flags = new Flags();
        UploadOptions cancelOptions = new UploadOptions(optionsReal.params, optionsReal.mimeType, optionsReal.checkCrc, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                double minPercent = 0;
                if (configuration.useConcurrentResumeUpload) {
                    minPercent = percent - (configuration.chunkSize) * configuration.concurrentTaskCount / data.length;
                } else {
                    minPercent = percent - configuration.chunkSize / data.length;
                }
                if (resumePercent >= minPercent) {
                    flags.isSuccess = true;
                }
                if (optionsReal.progressHandler != null) {
                    optionsReal.progressHandler.progress(key, percent);
                }
            }
        }, new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {

                return flags.shouldCancel;
            }
        }, options.netReadyHandler);

        final UploadCompleteInfo completeInfo = new UploadCompleteInfo();
        uploadData(data, key, configuration, cancelOptions, new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                completeInfo.key = key;
                completeInfo.responseInfo = info;
            }
        });

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                return completeInfo.responseInfo == null;
            }
        }, 5 * 60);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo != null);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo.isOK());
        assertTrue(completeInfo.responseInfo.toString(), verifyUploadKey(key, completeInfo.key));
    }

    protected void switchRegionTestWithFile(File file,
                                            String key,
                                            Configuration configuration,
                                            UploadOptions options) {
        if (configuration == null) {
            configuration = new Configuration.Builder().build();
        }
        Configuration configurationReal = configuration;

        ArrayList<String> hostArray0 = new ArrayList<>();
        hostArray0.add("mock1.up.qiniup.com");
        hostArray0.add("mock2.up.qiniup.com");
        ZoneInfo zoneInfo0 = ZoneInfo.buildInfo(hostArray0, null, null);
        ArrayList<String> hostArray1 = new ArrayList<>();
        hostArray1.add("upload-na0.qiniup.com");
        hostArray1.add("up-na0.qiniup.com");
        ZoneInfo zoneInfo1 = ZoneInfo.buildInfo(hostArray1, null, null);

        ArrayList<ZoneInfo> zoneInfoArray = new ArrayList<>();
        zoneInfoArray.add(zoneInfo0);
        zoneInfoArray.add(zoneInfo1);
        ZonesInfo zonesInfo = new ZonesInfo(zoneInfoArray);
        FixedZone zone = new FixedZone(zonesInfo);

        Configuration switchConfiguration = new Configuration.Builder()
                .chunkSize(configurationReal.chunkSize)
                .putThreshold(configurationReal.putThreshold)
                .retryMax(configurationReal.retryMax)
                .connectTimeout(configurationReal.connectTimeout)
                .responseTimeout(configurationReal.responseTimeout)
                .retryInterval(configurationReal.retryInterval)
                .recorder(configurationReal.recorder, configurationReal.keyGen)
                .proxy(configurationReal.proxy)
                .urlConverter(configurationReal.urlConverter)
                .useHttps(configurationReal.useHttps)
                .allowBackupHost(configurationReal.allowBackupHost)
                .useConcurrentResumeUpload(configurationReal.useConcurrentResumeUpload)
                .resumeUploadVersion(configurationReal.resumeUploadVersion)
                .concurrentTaskCount(configurationReal.concurrentTaskCount)
                .build();
        uploadFileAndAssertSuccessResult(file, key, switchConfiguration, options);
    }

    protected void switchRegionTestWithData(byte[] data,
                                            String key,
                                            Configuration configuration,
                                            UploadOptions options) {
        if (configuration == null) {
            configuration = new Configuration.Builder().build();
        }
        Configuration configurationReal = configuration;

        ArrayList<String> hostArray0 = new ArrayList<>();
        hostArray0.add("mock1.up.qiniup.com");
        hostArray0.add("mock2.up.qiniup.com");
        ZoneInfo zoneInfo0 = ZoneInfo.buildInfo(hostArray0, null, null);
        ArrayList<String> hostArray1 = new ArrayList<>();
        hostArray1.add("upload-na0.qiniup.com");
        hostArray1.add("up-na0.qiniup.com");
        ZoneInfo zoneInfo1 = ZoneInfo.buildInfo(hostArray1, null, null);

        ArrayList<ZoneInfo> zoneInfoArray = new ArrayList<>();
        zoneInfoArray.add(zoneInfo0);
        zoneInfoArray.add(zoneInfo1);
        ZonesInfo zonesInfo = new ZonesInfo(zoneInfoArray);
        FixedZone zone = new FixedZone(zonesInfo);

        Configuration switchConfiguration = new Configuration.Builder()
                .chunkSize(configurationReal.chunkSize)
                .putThreshold(configurationReal.putThreshold)
                .retryMax(configurationReal.retryMax)
                .connectTimeout(configurationReal.connectTimeout)
                .responseTimeout(configurationReal.responseTimeout)
                .retryInterval(configurationReal.retryInterval)
                .recorder(configurationReal.recorder, configurationReal.keyGen)
                .proxy(configurationReal.proxy)
                .urlConverter(configurationReal.urlConverter)
                .useHttps(configurationReal.useHttps)
                .allowBackupHost(configurationReal.allowBackupHost)
                .useConcurrentResumeUpload(configurationReal.useConcurrentResumeUpload)
                .resumeUploadVersion(configurationReal.resumeUploadVersion)
                .concurrentTaskCount(configurationReal.concurrentTaskCount)
                .build();
        uploadDataAndAssertSuccessResult(data, key, switchConfiguration, options);
    }


    protected static class Flags {
        boolean shouldCancel;
        boolean isSuccess;
    }
}
