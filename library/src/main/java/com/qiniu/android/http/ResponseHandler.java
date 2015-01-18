package com.qiniu.android.http;

import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.qiniu.android.common.Config;
import com.qiniu.android.utils.Dns;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;


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
    /**
     * 服务器IP
     */
    private String ip;

    public ResponseHandler(String url, CompletionHandler completionHandler, ProgressHandler progressHandler) {
        super(Looper.getMainLooper());
        URI uri = null;
        try {
            uri = new URI(url);
            this.host = uri.getHost();
        } catch (URISyntaxException e) {
            this.host = "N/A";
            e.printStackTrace();
        }
        this.completionHandler = completionHandler;
        this.progressHandler = progressHandler;
    }

    private static ResponseInfo buildResponseInfo(int statusCode, Header[] headers, byte[] responseBody, String host, double duration,
                                                  Throwable error) {
        String reqId = null;
        String xlog = null;
        String ip = null;
        String xvia = null;
        if (headers != null) {
            for (Header h : headers) {
                if ("X-Reqid".equals(h.getName())) {
                    reqId = h.getValue();
                } else if ("X-Log".equals(h.getName())) {
                    xlog = h.getValue();
                } else if ("X-Via".equals(h.getName())){
                    xvia = h.getValue();
                } else if ("X-Px".equals(h.getName())){
                    xvia = h.getValue();
                }
            }
        }

        String err = null;
        if (statusCode != 200) {
            if (responseBody != null) {
                try {
                    err = new String(responseBody, Config.CHARSET);
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
        }

        return new ResponseInfo(statusCode, reqId, xlog, xvia, host, ip, duration, err);
    }

    private static JSONObject buildJsonResp(byte[] body) throws Exception {
        String str = new String(body, Config.CHARSET);
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
        ResponseInfo info = buildResponseInfo(statusCode, headers, null, host, duration, exception);
        Log.i("upload----success", info.toString());
        completionHandler.complete(info, obj);
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
        double duration = (System.currentTimeMillis() - reqStartTime) / 1000.0;
        ResponseInfo info = buildResponseInfo(statusCode, headers, responseBody, host, duration, error);
        Log.i("upload----failed", info.toString());
        completionHandler.complete(info, null);
    }

    @Override
    public void onProgress(int bytesWritten, int totalSize) {
        if (progressHandler != null) {
            progressHandler.onProgress(bytesWritten, totalSize);
        }
    }

    @Override
    public void onStart() {
        this.reqStartTime = System.currentTimeMillis();
        super.onStart();
    }

    /**
     * hack the method for dns in background before receive msg in main looper
     * @param msg
     */
    @Override
    protected void sendMessage(Message msg) {
        if (msg.what == AsyncHttpResponseHandler.FAILURE_MESSAGE)
        {
            Object[] response = (Object[]) msg.obj;
            if (response != null && response.length >= 4){
                Throwable e = (Throwable) response[3];
                if (! (e instanceof UnknownHostException)){
                    this.ip = Dns.getAddressesString(host);
                }
            }
        }
        super.sendMessage(msg);
    }
}
