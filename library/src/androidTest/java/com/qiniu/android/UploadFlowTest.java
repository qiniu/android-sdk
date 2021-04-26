package com.qiniu.android;

import android.net.Uri;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.common.ZonesInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.FileRecorder;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.Utils;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class UploadFlowTest extends UploadBaseTest {

    protected void cancelTest(final float cancelPercent,
                              File file,
                              String key,
                              Configuration configuration,
                              UploadOptions options) {

        UploadInfo<File> fileInfo = new UploadInfo<>(file);
        fileInfo.configWithFile(file);
        cancelTest(cancelPercent, fileInfo, key, configuration, options);

        Uri uri = Uri.fromFile(file);
        UploadInfo<Uri> uriInfo = new UploadInfo<>(uri);
        uriInfo.configWithFile(file);
        cancelTest(cancelPercent, uriInfo, key, configuration, options);

        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
        }
        UploadInfo<InputStream> streamInfo = new UploadInfo<>(stream);
        streamInfo.configWithFile(file);
        cancelTest(cancelPercent, streamInfo, key, configuration, options);

        if (file.length() < 10 * 1024 * 1024) {
            byte[] data = getDataFromFile(file);
            UploadInfo<byte[]> dataInfo = new UploadInfo<>(data);
            dataInfo.configWithFile(file);
            cancelTest(cancelPercent, dataInfo, key, configuration, options);
        }
    }

    private void cancelTest(final float cancelPercent,
                            UploadInfo file,
                            String key,
                            Configuration configuration,
                            UploadOptions options) {

        if (options == null) {
            options = defaultOptions;
        }

        final UploadOptions optionsReal = options;
        final Flags flags = new Flags();
        UploadOptions cancelOptions = new UploadOptions(optionsReal.params, optionsReal.mimeType, optionsReal.checkCrc, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                if (cancelPercent <= percent) {
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
        upload(file, key, configuration, cancelOptions, new UpCompletionHandler() {
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

        UploadInfo<File> fileInfo = new UploadInfo<>(file);
        fileInfo.configWithFile(file);
        reuploadUploadTest(resumePercent, fileInfo, key, configuration, options);

        Uri uri = Uri.fromFile(file);
        UploadInfo<Uri> uriInfo = new UploadInfo<>(uri);
        uriInfo.configWithFile(file);
        reuploadUploadTest(resumePercent, uriInfo, key, configuration, options);

        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
        }
        UploadInfo<InputStream> streamInfo = new UploadInfo<>(stream);
        streamInfo.configWithFile(file);
        reuploadUploadTest(resumePercent, streamInfo, key, configuration, options);

        byte[] data = getDataFromFile(file);
        UploadInfo<byte[]> dataInfo = new UploadInfo<>(data);
        dataInfo.configWithFile(file);
        reuploadUploadTest(resumePercent, dataInfo, key, configuration, options);
    }

    private void reuploadUploadTest(final float resumePercent,
                                    final UploadInfo file,
                                    String key,
                                    final Configuration configuration,
                                    UploadOptions options) {

        FileRecorder fileRecorder = null;
        try {
            fileRecorder = new FileRecorder(Utils.sdkDirectory() + "/Test");
        } catch (IOException e) {
            e.printStackTrace();
        }

        final Configuration reuploadConfiguration = new Configuration.Builder()
                .chunkSize(configuration.chunkSize)
                .putThreshold(configuration.putThreshold)
                .retryMax(configuration.retryMax)
                .connectTimeout(configuration.connectTimeout)
                .responseTimeout(configuration.responseTimeout)
                .retryInterval(configuration.retryInterval)
                .recorder(fileRecorder, null)
                .proxy(configuration.proxy)
                .urlConverter(configuration.urlConverter)
                .useHttps(configuration.useHttps)
                .allowBackupHost(configuration.allowBackupHost)
                .useConcurrentResumeUpload(configuration.useConcurrentResumeUpload)
                .resumeUploadVersion(configuration.resumeUploadVersion)
                .concurrentTaskCount(configuration.concurrentTaskCount)
                .zone(configuration.zone)
                .build();


        if (options == null) {
            options = defaultOptions;
        }
        final UploadOptions optionsReal = options;

        cancelTest(resumePercent, file, key, reuploadConfiguration, optionsReal);


        final Flags flags = new Flags();
        UploadOptions reuploadOptions = new UploadOptions(optionsReal.params, optionsReal.mimeType, optionsReal.checkCrc, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                if (!flags.flags) {
                    flags.flags = true;
                    double minPercent = 0;
                    double currentChunkCount = 0;
                    double chunkSize = 0;
                    if (!reuploadConfiguration.useConcurrentResumeUpload) {
                        currentChunkCount = 1;
                        chunkSize = reuploadConfiguration.chunkSize;
                    } else if (reuploadConfiguration.resumeUploadVersion == Configuration.RESUME_UPLOAD_VERSION_V1) {
                        currentChunkCount = reuploadConfiguration.concurrentTaskCount;
                        chunkSize = Configuration.BLOCK_SIZE;
                    } else {
                        currentChunkCount = reuploadConfiguration.concurrentTaskCount;
                        chunkSize = reuploadConfiguration.chunkSize;
                    }
                    minPercent = percent + currentChunkCount * chunkSize / (double) file.size;
                    if (resumePercent <= minPercent) {
                        flags.isSuccess = true;
                    }
                    LogUtil.d("== upload reupload percent:" + percent + " minPercent:" + minPercent);
                }

                if (optionsReal.progressHandler != null) {
                    optionsReal.progressHandler.progress(key, percent);
                }
            }
        }, null, options.netReadyHandler);

        final UploadCompleteInfo completeInfo = new UploadCompleteInfo();
        upload(file, key, reuploadConfiguration, reuploadOptions, new UpCompletionHandler() {
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
        assertTrue(completeInfo.responseInfo.toString(), flags.isSuccess);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo != null);
        assertTrue(completeInfo.responseInfo.toString(), completeInfo.responseInfo.isOK());
        assertTrue(completeInfo.responseInfo.toString(), verifyUploadKey(key, completeInfo.key));
    }

    protected void switchRegionTestWithFile(File file,
                                            String key,
                                            Configuration configuration,
                                            UploadOptions options) {

        UploadInfo<File> fileInfo = new UploadInfo<>(file);
        fileInfo.configWithFile(file);
        switchRegionTestWithFile(fileInfo, key, configuration, options);

        Uri uri = Uri.fromFile(file);
        UploadInfo<Uri> uriInfo = new UploadInfo<>(uri);
        uriInfo.configWithFile(file);
        switchRegionTestWithFile(uriInfo, key, configuration, options);

        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
        }
        UploadInfo<InputStream> streamInfo = new UploadInfo<>(stream);
        streamInfo.configWithFile(file);
        switchRegionTestWithFile(streamInfo, key, configuration, options);

        byte[] data = getDataFromFile(file);
        UploadInfo<byte[]> dataInfo = new UploadInfo<>(data);
        dataInfo.configWithFile(file);
        switchRegionTestWithFile(dataInfo, key, configuration, options);
    }

    private void switchRegionTestWithFile(UploadInfo file,
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
                .zone(zone)
                .build();
        uploadFileAndAssertSuccessResult(file, key, switchConfiguration, options);
    }

    protected static class Flags {
        boolean flags;
        boolean shouldCancel;
        boolean isSuccess;
    }
}
