package com.qiniu.android.collect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Simon on 11/21/16.
 */
public class CollectManager {
    /**单线程任务队列*/
    private static ExecutorService singleServer = null;
    private static File recordFile = null;
    private long lastestUpload;// milliseconds

    private static CollectManager handler = null;

    private CollectManager() throws IOException {
        reset();
    }

    public static CollectManager getInstance() {
        if (handler == null) {
            try {
                handler = new CollectManager();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return handler;
    }

    /**
     * 修改记录"是否记录上传信息: isRecord","记录信息所在文件夹: recordDir"配置后,调用此方法重置.
     * 上传方式, 时间间隔,文件最大大小,上传阀值等参数修改不用调用此方法.
     *
     * @throws java.io.IOException
     */
    public void reset() throws IOException {
        if (Constants.isRecord) {
            setAndInitRecordDir(Constants.recordDir);
        }
        if (!Constants.isRecord && singleServer != null) {
            singleServer.shutdown();
        }
        if (Constants.isRecord && (singleServer == null || singleServer.isShutdown())) {
            singleServer = Executors.newSingleThreadExecutor();
        }
    }


    private void setAndInitRecordDir(String path) throws IOException {
        File recordDir = new File(path);
        if (!recordDir.exists()) {
            boolean r = recordDir.mkdirs();
            if (!r) {
                throw new IOException("mkdir failed");
            }
            return;
        }
        if (!recordDir.isDirectory()) {
            throw new IOException("does not mkdir");
        }
        String rememberName = "_qiniu_record_file_hu39lo7anx03";
        recordFile = new File(recordDir, rememberName);
    }

    public void handle(final String msg) {
        if (Constants.isRecord) {
            Runnable taskRecord = new Runnable() {
                @Override
                public void run() {
                    if (Constants.isRecord) {
                        tryRecode(msg);
                    }
                }
            };
            singleServer.submit(taskRecord);

            if (Constants.isUpload) {
                Runnable taskUpload = new Runnable() {
                    @Override
                    public void run() {
                        if (Constants.isRecord && Constants.isUpload) {
                            tryUploadThenClean();
                        }
                    }
                };
                singleServer.submit(taskUpload);
            }
        }

    }


    private boolean tryRecode(String msg) {
        if (recordFile.length() < Constants.maxRecordFileSize) {
            // 追加到文件尾部
            writeToFile(recordFile, msg + "\n", true);
        }
        return true;
    }

    private void tryUploadThenClean() {
        if (recordFile.length() > Constants.uploadThreshold) {
            long now = new Date().getTime();
            // milliseconds
            if (now > lastestUpload + Constants.minInteval * 60 * 1000) {
                lastestUpload = now;
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


    private boolean upload() {
        //TODO 同步上传
        return false;
    }
}
