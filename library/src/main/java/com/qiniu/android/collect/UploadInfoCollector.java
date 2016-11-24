package com.qiniu.android.collect;

import android.content.Context;

import com.qiniu.android.utils.ContextGetter;

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
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Simon on 11/22/16.
 */
public class UploadInfoCollector {
    /**
     * 单线程任务队列
     */
    private static ExecutorService singleServer = null;
    private static final String recordFileName = "_qiniu_record_file_hu3z9lo7anx03";
    private static File recordFile = null;
    private static long lastUpload;// milliseconds
    private static OkHttpClient httpClient = null;


    private UploadInfoCollector() throws IOException {

    }

    public static void setHttpClient(OkHttpClient client) {
        httpClient = client;
    }

    public static void clean() {
        try {
            if (recordFile != null) {
                recordFile.delete();
            } else {
                new File(getRecordDir(Config.recordDir), recordFileName).delete();
            }
        } catch (Exception e) {
        }

        try {
            if (singleServer != null) {
                singleServer.shutdown();
            }
        } catch (Exception e) {
        }
    }

    /**
     * 修改记录"是否记录上传信息: isRecord","记录信息所在文件夹: recordDir"配置后,调用此方法重置.
     * 上传方式, 时间间隔,文件最大大小,上传阀值等参数修改不用调用此方法.
     *
     * @throws java.io.IOException
     */
    public static void reset() throws IOException {
        if (Config.isRecord) {
            setAndInitRecordDir(getRecordDir(Config.recordDir));
        }
        if (!Config.isRecord && singleServer != null) {
            singleServer.shutdown();
        }
        if (Config.isRecord && (singleServer == null || singleServer.isShutdown())) {
            singleServer = Executors.newSingleThreadExecutor();
        }
    }

    private static File getRecordDir(String recordDir) {
        if (recordDir != null) {
            return new File(recordDir);
        } else {
            Context c = ContextGetter.applicationContext();
            if (c != null) {
                return c.getCacheDir();
            } else {
                return null;
            }
        }
    }


    private static void setAndInitRecordDir(File recordDir) throws IOException {
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


    public static void handle(final RecordMsg record) {
        if (Config.isRecord && (singleServer != null && !singleServer.isShutdown())) {
            Runnable taskRecord = new Runnable() {
                @Override
                public void run() {
                    if (Config.isRecord) {
                        tryRecode(record.toRecordMsg());
                    }
                }
            };
            singleServer.submit(taskRecord);

            if (Config.isUpload) {
                Runnable taskUpload = new Runnable() {
                    @Override
                    public void run() {
                        if (Config.isRecord && Config.isUpload) {
                            tryUploadThenClean();
                        }
                    }
                };
                singleServer.submit(taskUpload);
            }
        }

    }


    private static boolean tryRecode(String msg) {
        if (recordFile.length() < Config.maxRecordFileSize) {
            // 追加到文件尾部
            writeToFile(recordFile, msg + "\n", true);
        }
        return true;
    }

    private static void tryUploadThenClean() {
        if (recordFile.length() > Config.uploadThreshold) {
            long now = new Date().getTime();
            // milliseconds
            if (now > (lastUpload + Config.minInteval * 60 * 1000)) {
                lastUpload = now;
                boolean success = upload();
                if (success) {
                    // 记录文件重置为空
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {

                }
            }
        }
    }

    //同步上传
    private static boolean upload() {
        try {
            String serverURL = Config.serverURL;
            OkHttpClient client = getHttpClient();
            RequestBody formBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", recordFile.getName(),
                            RequestBody.create(MediaType.parse("text/plain"), recordFile))
                    .build();
            Request request = new Request.Builder().url(serverURL).post(formBody).build();

            Response res = client.newCall(request).execute();
            if (isOk(res)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean isOk(Response res) {
        return res.isSuccessful() && res.header("X-Reqid") != null;
    }


    private static OkHttpClient getHttpClient() {
        if (httpClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(10, TimeUnit.SECONDS);
            builder.readTimeout(10, TimeUnit.SECONDS);
            builder.writeTimeout((Config.minInteval / 2 + 1) * 60, TimeUnit.SECONDS);
            httpClient = builder.build();
        }
        return httpClient;
    }

    public static abstract class RecordMsg {
        public abstract String  toRecordMsg();

        public String replace(String o) {
            if (o == null) {
                return "";
            }
            return o.substring(0, Math.min(150, o.length())).replaceAll(",", ".")
                    .replaceAll("(\r\n)+", "\\\\r\\\\n").replaceAll("\r+", "\\\\r")
                    .replaceAll("\n+", "\\\\n").replaceAll("\t+", "\\\\t").replaceAll(" +", " ");
        }
    }

}
