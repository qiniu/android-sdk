package com.qiniu.android.collect;

import com.qiniu.android.http.UserAgent;
import com.qiniu.android.storage.UpToken;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 收集上传信息，发送到后端
 */
public class UploadInfoCollector {
    /**
     * 单线程任务队列
     */
    private ExecutorService singleServer = null;
    private final String recordFileName = "_qiniu_record_file_hu3z9lo7anx03";
    private File recordFile = null;
    private long lastUpload;// milliseconds
    private OkHttpClient httpClient = null;

    private static UploadInfoCollector collector;


    private UploadInfoCollector() {
        try {
            reset0();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static UploadInfoCollector getCollector() {
        if (collector == null) {
            collector = new UploadInfoCollector();
        }
        return collector;
    }


    /**
     * 清理操作。
     * 若需更改 isRecord、isUpload 的值，请在此方法调用前修改。
     */
    public static void clean() {
        try {
            getCollector().clean0();
        } catch (Exception e) {
            e.printStackTrace();
        }
        collector = null;
    }


    private void clean0() {
        try {
            if (singleServer != null) {
                singleServer.shutdown();
            }
        } catch (Exception e) {
            // do nothing
        }
        singleServer = null;
        httpClient = null;

        try {
            if (recordFile != null) {
                recordFile.delete();
            } else {
                new File(getRecordDir(Config.recordDir), recordFileName).delete();
            }
        } catch (Exception e) {
            // do nothing
        }
        recordFile = null;

    }

    /**
     * 修改记录"是否记录上传信息: isRecord","记录信息所在文件夹: recordDir"配置后,调用此方法重置.
     * 上传方式, 时间间隔,文件最大大小,上传阀值等参数修改不用调用此方法.
     *
     * @throws java.io.IOException
     */
    public static void reset() {
        try {
            getCollector().reset0();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reset0() throws IOException {
        if (Config.isRecord) {
            initRecordFile(getRecordDir(Config.recordDir));
        }
        if (!Config.isRecord && singleServer != null) {
            singleServer.shutdown();
        }
        if (Config.isRecord && (singleServer == null || singleServer.isShutdown())) {
            singleServer = Executors.newSingleThreadExecutor();
        }
    }

    private File getRecordDir(String recordDir) {
        return new File(recordDir);
    }


    private void initRecordFile(File recordDir) throws IOException {
        if (recordDir == null) {
            throw new IOException("record's dir is not setted");
        }
        if (!recordDir.exists()) {
            boolean r = recordDir.mkdirs();
            if (!r) {
                throw new IOException("mkdir failed: " + recordDir.getAbsolutePath());
            }
            return;
        }
        if (!recordDir.isDirectory()) {
            throw new IOException(recordDir.getAbsolutePath() + " is not a dir");
        }

        recordFile = new File(recordDir, recordFileName);
    }


    public static void handle(final UpToken upToken, final RecordMsg record) {
        try {
            if (Config.isRecord) {
                getCollector().handle0(upToken, record);
            }
        } catch (Throwable t) {
            // do nothing
        }
    }

    private void handle0(final UpToken upToken, final RecordMsg record) {
        if (singleServer != null && !singleServer.isShutdown()) {
            Runnable taskRecord = new Runnable() {
                @Override
                public void run() {
                    if (Config.isRecord) {
                        try {
                            tryRecode(record.toRecordMsg());
                        } catch (Throwable t) {
                            // do nothing
                        }
                    }
                }
            };
            singleServer.submit(taskRecord);

            // 少几次上传没有影响
            if (Config.isUpload && upToken != UpToken.NULL) {
                Runnable taskUpload = new Runnable() {
                    @Override
                    public void run() {
                        if (Config.isRecord && Config.isUpload) {
                            try {
                                tryUploadAndClean(upToken);
                            } catch (Throwable t) {
                                // do nothing
                            }
                        }
                    }
                };
                singleServer.submit(taskUpload);
            }
        }
    }


    private void tryRecode(String msg) {
        if (Config.isRecord && recordFile.length() < Config.maxRecordFileSize) {
            // 追加到文件尾部并换行
            writeToFile(recordFile, msg + "\n", true);
        }
    }

    private void tryUploadAndClean(final UpToken upToken) {
        if (Config.isUpload && recordFile.length() > Config.uploadThreshold) {
            long now = new Date().getTime();
            // Config.interval 单位为：分钟
            if (now > (lastUpload + Config.interval * 60 * 1000)) {
                lastUpload = now;
                //同步上传
                boolean success = upload(upToken);
                if (success) {
                    // 记录文件重置为空
                    writeToFile(recordFile, "", false);
                    writeToFile(recordFile, "", false);
                }
            }
        }
    }

    private static void writeToFile(File file, String msg, boolean isAppend) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, isAppend);
            fos.write(msg.getBytes(Charset.forName("UTF-8")));
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
    }

    //同步上传
    private boolean upload(final UpToken upToken) {
        try {
            String serverURL = Config.serverURL;
            OkHttpClient client = getHttpClient();
            RequestBody reqBody = RequestBody.create(MediaType.parse("text/plain"), recordFile);
            Request request = new Request.Builder().url(serverURL).
                    addHeader("Authorization", "UpToken " + upToken.token).
                    addHeader("User-Agent", UserAgent.instance().getUa(upToken.accessKey)).
                    post(reqBody).build();
            Response res = client.newCall(request).execute();
            return isOk(res);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private boolean isOk(Response res) {
        return res.isSuccessful() && res.header("X-Reqid") != null;
    }


    private OkHttpClient getHttpClient() {
        if (httpClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(10, TimeUnit.SECONDS);
            builder.readTimeout(15, TimeUnit.SECONDS);
            builder.writeTimeout((Config.interval / 2 + 1) * 60 - 10, TimeUnit.SECONDS);
            httpClient = builder.build();
        }
        return httpClient;
    }

    public static abstract class RecordMsg {
        public abstract String toRecordMsg();
    }

}
