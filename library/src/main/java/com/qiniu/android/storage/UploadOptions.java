package com.qiniu.android.storage;

import android.os.Looper;

import com.qiniu.android.utils.AndroidNetwork;
import com.qiniu.android.utils.LogUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 定义数据或文件上传时的可选项
 */
public final class UploadOptions {

    /**
     * 用于服务器上传回调通知的自定义参数，参数的key必须以x: 开头  eg: x:foo
     */
    public final Map<String, String> params;

    /**
     * 用于设置meta数据，参数的key必须以x-qn-meta- 开头  eg: x-qn-meta-key
     */
    public final Map<String, String> metaDataParam;

    /**
     * 指定上传文件的MimeType
     */
    public final String mimeType;

    /**
     * 启用上传内容crc32校验
     */
    public final boolean checkCrc;

    /**
     * 上传内容进度处理
     */
    public final UpProgressHandler progressHandler;

    /**
     * 取消上传信号
     */
    public final UpCancellationSignal cancellationSignal;

    /**
     * 当网络暂时无法使用时，由用户决定是否继续处理
     */
    public final NetReadyHandler netReadyHandler;

    public UploadOptions(Map<String, String> params,
                         String mimeType,
                         boolean checkCrc,
                         UpProgressHandler progressHandler,
                         UpCancellationSignal cancellationSignal) {
        this(params, mimeType, checkCrc, progressHandler, cancellationSignal, null);
    }

    public UploadOptions(final Map<String, String> params,
                         String mimeType,
                         boolean checkCrc,
                         UpProgressHandler progressHandler,
                         UpCancellationSignal cancellationSignal,
                         NetReadyHandler netReadyHandler) {
            this(params, null, mimeType, checkCrc, progressHandler, cancellationSignal, netReadyHandler);
    }

    public UploadOptions(final Map<String, String> params,
                         final Map<String, String> metaDataParam,
                         String mimeType,
                         boolean checkCrc,
                         UpProgressHandler progressHandler,
                         UpCancellationSignal cancellationSignal,
                         NetReadyHandler netReadyHandler) {

        int netReadyCheckTime = 6;
        try {
            String netCheckTime = params.get("netCheckTime");
            if (netCheckTime != null) {
                netReadyCheckTime = Integer.parseInt(netCheckTime);
            }
        } catch (Exception e) {}
        this.params = filterParam(params);
        this.metaDataParam = filterMetaDataParam(metaDataParam);
        this.mimeType = mime(mimeType);
        this.checkCrc = checkCrc;
        this.progressHandler = progressHandler != null ? progressHandler : new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                LogUtil.d("" + percent);
            }
        };
        this.cancellationSignal = cancellationSignal != null ? cancellationSignal : new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {
                return false;
            }
        };
        final int finalNetReadyCheckTime = netReadyCheckTime;
        this.netReadyHandler = netReadyHandler != null ? netReadyHandler : new NetReadyHandler() {
            @Override
            public void waitReady() {
                if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                    return;
                }
                for (int i = 0; i < finalNetReadyCheckTime; i++) {
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

    /**
     * 过滤meta data参数，只有参数名以<code>x-qn-meta-</code>开头的参数才会被使用
     *
     * @param params 待过滤的用户自定义参数
     * @return 过滤后的参数
     */
    private static Map<String, String> filterMetaDataParam(Map<String, String> params) {
        Map<String, String> ret = new HashMap<String, String>();
        if (params == null) {
            return ret;
        }

        for (Map.Entry<String, String> i : params.entrySet()) {
            if (i.getKey().startsWith("x-qn-meta-") && i.getValue() != null && !i.getValue().equals("")) {
                ret.put(i.getKey(), i.getValue());
            }
        }
        return ret;
    }


    public static UploadOptions defaultOptions() {
        return new UploadOptions(null, null, false, null, null);
    }

    private static String mime(String mimeType) {
        if (mimeType == null || mimeType.equals("")) {
            return "application/octet-stream";
        }
        return mimeType;
    }
}
