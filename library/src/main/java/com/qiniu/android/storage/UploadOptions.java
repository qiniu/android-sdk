package com.qiniu.android.storage;

import android.os.Looper;
import android.util.Log;

import com.qiniu.android.utils.AndroidNetwork;

import java.util.HashMap;
import java.util.Map;

/**
 * 定义数据或文件上传时的可选项
 */
public final class UploadOptions {

    /**
     * 扩展参数，以<code>x:</code>开头的用户自定义参数
     */
    final Map<String, String> params;

    /**
     * 指定上传文件的MimeType
     */
    final String mimeType;

    /**
     * 启用上传内容crc32校验
     */
    final boolean checkCrc;

    /**
     * 上传内容进度处理
     */
    final UpProgressHandler progressHandler;

    /**
     * 取消上传信号
     */
    final UpCancellationSignal cancellationSignal;

    /**
     * 当网络暂时无法使用时，由用户决定是否继续处理
     */
    final NetReadyHandler netReadyHandler;

    public UploadOptions(Map<String, String> params, String mimeType, boolean checkCrc,
                         UpProgressHandler progressHandler, UpCancellationSignal cancellationSignal) {
        this(params, mimeType, checkCrc, progressHandler, cancellationSignal, null);
    }

    public UploadOptions(Map<String, String> params, String mimeType, boolean checkCrc,
                         UpProgressHandler progressHandler, UpCancellationSignal cancellationSignal, NetReadyHandler netReadyHandler) {
        this.params = filterParam(params);
        this.mimeType = mime(mimeType);
        this.checkCrc = checkCrc;
        this.progressHandler = progressHandler != null ? progressHandler : new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                Log.d("Qiniu.UploadProgress", "" + percent);
            }
        };
        this.cancellationSignal = cancellationSignal != null ? cancellationSignal : new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {
                return false;
            }
        };
        this.netReadyHandler = netReadyHandler != null ? netReadyHandler : new NetReadyHandler() {
            @Override
            public void waitReady() {
                if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                    return;
                }
                for (int i = 0; i < 6; i++) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (AndroidNetwork.isNetWorkReady()) {
                        return;
                    }
                }
            }
        };
    }

    /**
     * 过滤用户自定义参数，只有参数名以<code>x:</code>开头的参数才会被使用
     *
     * @param params 待过滤的用户自定义参数
     * @return 过滤后的用户自定义参数
     */
    private static Map<String, String> filterParam(Map<String, String> params) {
        Map<String, String> ret = new HashMap<String, String>();
        if (params == null) {
            return ret;
        }

        for (Map.Entry<String, String> i : params.entrySet()) {
            if (i.getKey().startsWith("x:") && i.getValue() != null && !i.getValue().equals("")) {
                ret.put(i.getKey(), i.getValue());
            }
        }
        return ret;
    }

    static UploadOptions defaultOptions() {
        return new UploadOptions(null, null, false, null, null);
    }

    private static String mime(String mimeType) {
        if (mimeType == null || mimeType.equals("")) {
            return "application/octet-stream";
        }
        return mimeType;
    }
}
