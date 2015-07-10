package com.qiniu.android.http;

import android.os.Looper;
import android.os.Message;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.ResponseHandlerInterface;
import com.qiniu.android.common.Constants;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;


/**
 * 定义请求回复处理方法
 */
public final class ResponseHandler extends AsyncHttpResponseHandler {
    /**
     * 请求的地址
     */
    private String host;
    /**
     * 请求进度处理器
     */
    private ProgressHandler progressHandler;
    /**
     * 请求完成处理器
     */
    private CompletionHandler completionHandler;
    /**
     * 请求开始时间
     */
    private long reqStartTime;

    private String ip = null;

    /**
     * 服务器端口
     */
    private int port = -1;

    private String path = null;

    private volatile long sent = 0;

    public ResponseHandler(String url, CompletionHandler completionHandler, ProgressHandler progressHandler) {
        super(Looper.getMainLooper());
        URI uri = null;
        try {
            uri = new URI(url);
            this.host = uri.getHost();
            this.port = uri.getPort();
            this.path = uri.getPath();
        } catch (URISyntaxException e) {
            this.host = "N/A";
            e.printStackTrace();
        }
        this.completionHandler = completionHandler;
        this.progressHandler = progressHandler;
    }

    private static ResponseInfo buildResponseInfo(int statusCode, Header[] headers, byte[] responseBody,
                                                  String host, String path, String ip, int port, double duration, long sent, Throwable error) {

        if (error != null && error instanceof CancellationHandler.CancellationException) {
            return ResponseInfo.cancelled();
        }

        String reqId = null;
        String xlog = null;
        String xvia = null;
        if (headers != null) {
            for (Header h : headers) {
                if ("X-Reqid".equals(h.getName())) {
                    reqId = h.getValue();
                } else if ("X-Log".equals(h.getName())) {
                    xlog = h.getValue();
                } else if ("X-Via".equals(h.getName())) {
                    xvia = h.getValue();
                } else if ("X-Px".equals(h.getName())) {
                    xvia = h.getValue();
                } else if ("Fw-Via".equals(h.getName())) {
                    xvia = h.getValue();
                }
            }
        }

        String err = null;
        if (statusCode != 200) {
            if (responseBody != null) {
                try {
                    err = new String(responseBody, Constants.UTF_8);
                    JSONObject obj = new JSONObject(err);
                    err = obj.optString("error", err);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else {
                if (error != null) {
                    err = error.getMessage();
                    if (err == null) {
                        err = error.toString();
                    }
                }
            }
        } else {
            if (reqId == null) {
                err = "remote is not qiniu server!";
            }
        }

        if (statusCode == 0) {
            statusCode = ResponseInfo.NetworkError;
            String msg = error.getMessage();
            if (error instanceof IOException) {
                if (msg != null && msg.indexOf("UnknownHostException") == 0) {
                    statusCode = ResponseInfo.UnknownHost;
                } else if (msg != null && msg.indexOf("Broken pipe") == 0) {
                    statusCode = ResponseInfo.NetworkConnectionLost;
                } else if (error instanceof NoHttpResponseException) {
                    statusCode = ResponseInfo.NetworkConnectionLost;
                } else if (error instanceof SocketTimeoutException) {
                    statusCode = ResponseInfo.TimedOut;
                } else if (error instanceof ConnectTimeoutException || error instanceof SocketException) {
                    statusCode = ResponseInfo.CannotConnectToHost;
                }
            }
        }

        return new ResponseInfo(statusCode, reqId, xlog, xvia, host, path, ip, port, duration, sent, err);
    }

    private static JSONObject buildJsonResp(byte[] body) throws Exception {
        String str = new String(body, Constants.UTF_8);
        return new JSONObject(str);
    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
        double duration = (System.currentTimeMillis() - reqStartTime) / 1000.0;
        JSONObject obj = null;
        Exception exception = null;
        try {
            obj = buildJsonResp(responseBody);
        } catch (Exception e) {
            exception = e;
        }
        ResponseInfo info = buildResponseInfo(statusCode, headers, null, host, path, ip, port, duration, sent, exception);
//        Log.i("upload----success", info.toString());
        completionHandler.complete(info, obj);
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
        double duration = (System.currentTimeMillis() - reqStartTime) / 1000.0;
        ResponseInfo info = buildResponseInfo(statusCode, headers, responseBody, host, path, ip, port, duration, sent, error);
//        Log.i("upload----failed", info.toString());
        completionHandler.complete(info, null);
    }

    public void onProgress(int bytesWritten, int totalSize) {
        this.sent += bytesWritten;
        if (progressHandler != null) {
            progressHandler.onProgress(bytesWritten, totalSize);
        }
    }

    public void onProgress(long bytesWritten, long totalSize) {
        onProgress((int) bytesWritten, (int) totalSize);
    }

    @Override
    public void onStart() {
        this.reqStartTime = System.currentTimeMillis();
        super.onStart();
    }

    /**
     * hack the method for dns in background before receive msg in main looper
     *
     * @param msg 发送的状态信息
     */
    @Override
    protected void sendMessage(Message msg) {
        switch (msg.what) {
            case SUCCESS_MESSAGE:
            case FAILURE_MESSAGE:
                this.ip = AsyncHttpClientMod.ip.get();
                AsyncHttpClientMod.ip.remove();
            default:
                break;
        }
        super.sendMessage(msg);
    }

    @Override
    public void onPostProcessResponse(ResponseHandlerInterface instance, HttpResponse response) {
    }
}
