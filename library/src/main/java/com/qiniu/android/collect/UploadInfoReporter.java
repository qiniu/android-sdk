package com.qiniu.android.collect;


import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.transaction.TransactionManager;
import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.StringUtils;
import com.qiniu.android.utils.Utils;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class UploadInfoReporter {
    private static final String DelayReportTransactionName = "com.qiniu.uplog";
    private ReportConfig config = ReportConfig.getInstance();
    private long lastReportTime = 0;
    private File recordDirectory = new File(config.recordDirectory);
    private File recorderFile = new File(config.recordDirectory + "/qiniu.log");
    private File recorderTempFile = new File(config.recordDirectory + "/qiniuTemp.log");
    private String X_Log_Client_Id;
    private RequestTransaction transaction;

    private final ExecutorService executorService = new ThreadPoolExecutor(1, 2,
            120L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());
    // 是否正在向服务上报中
    private boolean isReporting = false;

    private static UploadInfoReporter instance = new UploadInfoReporter();

    private UploadInfoReporter() {
    }

    public static UploadInfoReporter getInstance() {
        return instance;
    }

    public synchronized void report(final ReportItem reportItem, final String tokenString) {
        if (!checkReportAvailable() || reportItem == null || tokenString == null || tokenString.length() == 0) {
            return;
        }

        final String jsonString = reportItem.toJson();
        if (jsonString == null) {
            return;
        }

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                LogUtil.i("up log:" + StringUtils.toNonnullString(jsonString));
                synchronized (this) {
                    saveReportJsonString(jsonString);
                    reportToServerIfNeeded(tokenString);
                }
            }
        });
    }

    public void clean() {
        cleanRecorderFile();
        cleanTempLogFile();
    }

    private void cleanRecorderFile() {
        if (recorderFile.exists()) {
            recorderFile.delete();
        }
    }

    private void cleanTempLogFile() {
        if (recorderTempFile.exists()) {
            recorderTempFile.delete();
        }
    }

    private boolean checkReportAvailable() {
        if (!config.isReportEnable) {
            return false;
        }
        if (config.maxRecordFileSize <= config.uploadThreshold) {
            LogUtil.e("maxRecordFileSize must be larger than uploadThreshold");
            return false;
        }
        return true;
    }

    private void saveReportJsonString(final String jsonString) {

        if (!recordDirectory.exists() && !recordDirectory.mkdirs()) {
            return;
        }

        if (!recordDirectory.isDirectory()) {
            LogUtil.e("recordDirectory is not a directory");
            return;
        }

        if (!recorderFile.exists()) {
            try {
                boolean isSuccess = recorderFile.createNewFile();
                if (!isSuccess) {
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        if (recorderFile.length() > config.maxRecordFileSize) {
            return;
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(recorderFile, true);
            fos.write((jsonString + "\n").getBytes());
            fos.flush();
        } catch (FileNotFoundException ignored) {
        } catch (IOException ignored) {
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void reportToServerIfNeeded(final String tokenString) {
        boolean needToReport = false;
        long currentTime = Utils.currentSecondTimestamp();
        final long interval = (long)(config.interval * 60);
        if (recorderTempFile.exists()) {
            needToReport = true;
        } else if ((lastReportTime == 0 || (currentTime - lastReportTime) >= interval || recorderFile.length() > config.uploadThreshold) &&
                recorderFile.renameTo(recorderTempFile)) {
            needToReport = true;
        }

        if (needToReport && !this.isReporting) {
            reportToServer(tokenString);
        } else {
            // 有未上传日志存在，则 interval 时间后再次重试一次
            if (!recorderFile.exists() || recorderFile.length() == 0) {
                return;
            }

            TransactionManager manager = TransactionManager.getInstance();
            List<TransactionManager.Transaction> transactionList = manager.transactionsForName(DelayReportTransactionName);
            if (transactionList != null && transactionList.size() > 1) {
                return;
            }

            if (transactionList != null && transactionList.size() == 1) {
                TransactionManager.Transaction transaction = transactionList.get(0);
                if (transaction != null && !transaction.isExecuting()) {
                    return;
                }
            }

            TransactionManager.Transaction transaction = new TransactionManager.Transaction(DelayReportTransactionName, (int) interval, new Runnable() {
                @Override
                public void run() {
                    reportToServerIfNeeded(tokenString);
                }
            });
            TransactionManager.getInstance().addTransaction(transaction);
        }
    }

    private void reportToServer(String tokenString) {

        RequestTransaction transaction = createUploadRequestTransaction(tokenString);
        if (transaction == null) {
            return;
        }

        byte[] logData = getLogData();
        if (logData == null || logData.length == 0) {
            return;
        }

        isReporting = true;
        transaction.reportLog(logData, X_Log_Client_Id, true, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                if (responseInfo.isOK()) {
                    lastReportTime = new Date().getTime();
                    if (X_Log_Client_Id == null
                            && responseInfo.responseHeader != null
                            && responseInfo.responseHeader.get("x-log-client-id") != null) {
                        X_Log_Client_Id = responseInfo.responseHeader.get("x-log-client-id");
                    }
                    cleanTempLogFile();
                }

                isReporting = false;
                destroyTransactionResource();
            }
        });

    }

    private byte[] getLogData() {
        if (recorderTempFile == null || recorderTempFile.length() == 0) {
            return null;
        }

        int fileSize = (int) recorderTempFile.length();
        RandomAccessFile randomAccessFile = null;
        byte[] data = null;
        try {
            randomAccessFile = new RandomAccessFile(recorderTempFile, "r");
            ByteArrayOutputStream out = new ByteArrayOutputStream(fileSize);
            int len = 0;
            byte[] buff = new byte[fileSize];
            while ((len = randomAccessFile.read(buff)) >= 0) {
                out.write(buff, 0, len);
            }
            data = out.toByteArray();
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            data = null;
        } finally {
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                }
            }
        }

        return data;
    }

    private synchronized RequestTransaction createUploadRequestTransaction(String tokenString) {
        if (transaction != null) {
            return null;
        }

        if (config == null) {
            return null;
        }
        UpToken token = UpToken.parse(tokenString);
        if (token == null) {
            return null;
        }
        ArrayList<String> hosts = new ArrayList<>();
        hosts.add(config.serverURL);

        transaction = new RequestTransaction(hosts, ZoneInfo.EmptyRegionId, token);
        return transaction;
    }

    private synchronized void destroyTransactionResource() {
        transaction = null;
    }
}
