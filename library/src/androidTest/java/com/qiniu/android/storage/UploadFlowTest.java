package com.qiniu.android.storage;


import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.common.ZonesInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.Utils;

import org.json.JSONObject;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class UploadFlowTest extends UploadBaseTest {

    public void test() {
    }

    protected void cancelTest(long cancelPosition,
                              File file,
                              String key,
                              Configuration configuration,
                              UploadOptions options) {
        cancelPosition *= 1024;

        LogUtil.d("======== progress File cancel ============================================");
        UploadInfo<File> fileInfo = new UploadInfo<>(file);
        fileInfo.configWithFile(file);
        cancelTest(cancelPosition, fileInfo, key, configuration, options);

        Uri uri = Uri.fromFile(file);
        UploadInfo<Uri> uriInfo = new UploadInfo<>(uri);
        uriInfo.configWithFile(file);
        cancelTest(cancelPosition, uriInfo, key, configuration, options);

        LogUtil.d("======== progress InputStream with size cancel ===========================");
        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
        }
        UploadInfo<InputStream> streamInfo = new UploadInfo<>(stream);
        streamInfo.configWithFile(file);
        cancelTest(cancelPosition, streamInfo, key, configuration, options);

        LogUtil.d("======== progress InputStream without size cancel =========================");
        stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
        }
        streamInfo = new UploadInfo<>(stream);
        streamInfo.configWithFile(file);
        streamInfo.size = -1;
        cancelTest(cancelPosition, streamInfo, key, configuration, options);

        LogUtil.d("======== progress data cancel =============================================");
        if (file.length() < 4 * 1024 * 1024) {
            byte[] data = getDataFromFile(file);
            UploadInfo<byte[]> dataInfo = new UploadInfo<>(data);
            dataInfo.configWithFile(file);
            cancelTest(cancelPosition, dataInfo, key, configuration, options);
        }
    }

    private void cancelTest(final long cancelPosition,
                            UploadInfo file,
                            String key,
                            Configuration configuration,
                            UploadOptions options) {

        if (options == null) {
            options = defaultOptions;
        }

        final UploadOptions optionsReal = options;
        final Flags flags = new Flags();
        UploadOptions cancelOptions = new UploadOptions(optionsReal.params, optionsReal.mimeType, optionsReal.checkCrc, new UpProgressBytesHandler() {
            @Override
            public void progress(String key, long uploadBytes, long totalBytes) {
                if (cancelPosition < uploadBytes) {
                    flags.shouldCancel = true;
                }
                if (optionsReal.progressHandler instanceof UpProgressBytesHandler) {
                    ((UpProgressBytesHandler)optionsReal.progressHandler).progress(key, uploadBytes, totalBytes);
                }
            }

            @Override
            public void progress(String key, double percent) {
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

    protected void reuploadUploadTest(long resumePosition,
                                      File file,
                                      String key,
                                      Configuration configuration,
                                      UploadOptions options) {

        resumePosition *= 1024;

        LogUtil.d("======== progress File ReUpload ==============================================");
        UploadInfo<File> fileInfo = new UploadInfo<>(file);
        fileInfo.configWithFile(file);
        reuploadUploadTest(resumePosition, fileInfo, fileInfo, key, configuration, options);

        LogUtil.d("======== progress Uri ReUpload ==============================================");
        Uri uri = Uri.fromFile(file);
        UploadInfo<Uri> uriInfo = new UploadInfo<>(uri);
        uriInfo.configWithFile(file);
        reuploadUploadTest(resumePosition, uriInfo, uriInfo, key, configuration, options);

        LogUtil.d("======== progress InputStream with size ReUpload =============================");
        InputStream firstStream = null;
        try {
            firstStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
        }
        UploadInfo<InputStream> firstStreamInfo = new UploadInfo<>(firstStream);
        firstStreamInfo.configWithFile(file);

        InputStream secondStream = null;
        try {
            secondStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
        }
        UploadInfo<InputStream> secondStreamInfo = new UploadInfo<>(secondStream);
        secondStreamInfo.configWithFile(file);

        reuploadUploadTest(resumePosition, firstStreamInfo, secondStreamInfo, key, configuration, options);
        try {
            firstStream.close();
            secondStream.close();
        } catch (IOException e) {
        }

        LogUtil.d("======== progress InputStream without size ReUpload ==========================");
        firstStream = null;
        try {
            firstStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
        }
        firstStreamInfo = new UploadInfo<>(firstStream);
        firstStreamInfo.configWithFile(file);
        firstStreamInfo.size = -1;

        secondStream = null;
        try {
            secondStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
        }
        secondStreamInfo = new UploadInfo<>(secondStream);
        secondStreamInfo.configWithFile(file);
        secondStreamInfo.size = -1;

        reuploadUploadTest(resumePosition, firstStreamInfo, secondStreamInfo, key, configuration, options);
        try {
            firstStream.close();
            secondStream.close();
        } catch (IOException e) {
        }
    }

    private void reuploadUploadTest(final long resumePosition,
                                    final UploadInfo firstFile,
                                    final UploadInfo secondFile,
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

        cancelTest(resumePosition, firstFile, key, reuploadConfiguration, optionsReal);

        LogUtil.d("progress ReUpload ====================================================");

        final Flags flags = new Flags();
        UploadOptions reuploadOptions = new UploadOptions(optionsReal.params, optionsReal.mimeType, optionsReal.checkCrc, new UpProgressBytesHandler() {
            @Override
            public void progress(String key, double percent) {
            }

            @Override
            public void progress(String key, long uploadBytes, long totalBytes) {
                if (!flags.flags) {
                    flags.flags = true;
                    if (uploadBytes <= resumePosition && uploadBytes > 0) {
                        flags.isSuccess = true;
                    }
                }

                if (optionsReal.progressHandler != null && optionsReal.progressHandler instanceof UpProgressBytesHandler) {
                    ((UpProgressBytesHandler)optionsReal.progressHandler).progress(key, uploadBytes, totalBytes);
                }
            }
        }, null, options.netReadyHandler);

        final UploadCompleteInfo completeInfo = new UploadCompleteInfo();
        upload(secondFile, key, reuploadConfiguration, reuploadOptions, new UpCompletionHandler() {
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
        }, 10* 60);
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

        if (file.length() < 4 * 1024 * 1024) {
            byte[] data = getDataFromFile(file);
            UploadInfo<byte[]> dataInfo = new UploadInfo<>(data);
            dataInfo.configWithFile(file);
            switchRegionTestWithFile(dataInfo, key, configuration, options);
        }
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
        hostArray1.add("upload.qiniup.com");
        hostArray1.add("up.qiniup.com");
        ZoneInfo zoneInfo1 = ZoneInfo.buildInfo(hostArray1, null, null);

        ArrayList<String> hostArray2 = new ArrayList<>();
        hostArray2.add("upload-na0.qiniup.com");
        hostArray2.add("up-na0.qiniup.com");
        ZoneInfo zoneInfo2 = ZoneInfo.buildInfo(hostArray2, null, null);

        ArrayList<ZoneInfo> zoneInfoArray = new ArrayList<>();
        zoneInfoArray.add(zoneInfo0);
        zoneInfoArray.add(zoneInfo1);
        zoneInfoArray.add(zoneInfo2);
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
