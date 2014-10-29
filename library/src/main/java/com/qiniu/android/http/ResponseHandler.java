package com.qiniu.android.http;

import android.os.Looper;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.qiniu.android.common.Config;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;


public final class ResponseHandler extends AsyncHttpResponseHandler {
    private ProgressHandler progressHandler;
    private CompletionHandler completionHandler;

    public ResponseHandler(CompletionHandler completionHandler, ProgressHandler progressHandler) {
        super(Looper.getMainLooper());
        this.completionHandler = completionHandler;
        this.progressHandler = progressHandler;
    }

    private static ResponseInfo buildResponseInfo(int statusCode, Header[] headers, byte[] responseBody,
                                                  Throwable error) {
        String reqId = null;
        String xlog = null;
        if (headers != null) {
            for (Header h : headers) {
                if ("X-Reqid".equals(h.getName())) {
                    reqId = h.getValue();
                } else if ("X-Log".equals(h.getName())) {
                    xlog = h.getValue();
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
        } else{
            if (reqId==null){
                err = "remote is not qiniu server!";
            }
        }

        if (statusCode == 0) {
            statusCode = ResponseInfo.NetworkError;
        }

        return new ResponseInfo(statusCode, reqId, xlog, err);
    }

    private static JSONObject buildJsonResp(byte[] body) throws Exception {

        String str = new String(body, Config.CHARSET);
        return new JSONObject(str);
    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
        JSONObject obj = null;
        Exception exception = null;
        try {
            obj = buildJsonResp(responseBody);
        } catch (Exception e) {
            exception = e;
        }

        ResponseInfo info = buildResponseInfo(statusCode, headers, null, exception);
        Log.i("qiniu----success", info.toString());

        completionHandler.complete(info, obj);
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
        ResponseInfo info = buildResponseInfo(statusCode, headers, responseBody, error);
        Log.i("qiniu----failed", info.toString());
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
        super.onStart();
    }
}
