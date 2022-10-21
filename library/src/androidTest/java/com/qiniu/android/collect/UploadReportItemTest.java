package com.qiniu.android.collect;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.Request;
import com.qiniu.android.BaseTest;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
public class UploadReportItemTest extends BaseTest {

    @Test
    public void testSetAndRemoveValue(){

        ReportItem reportItem = new ReportItem();
        reportItem.setReport(ReportItem.LogTypeRequest, ReportItem.RequestKeyLogType);

        String json = reportItem.toJson();
        assertTrue(! json.equals("{}"));

        reportItem.removeReportValue(ReportItem.RequestKeyLogType);

        json = reportItem.toJson();
        assertTrue(json.equals("{}"));
    }

    @Test
    public void testReportStatusCode(){
        ResponseInfo responseInfo = createResponseInfo(ResponseInfo.Cancelled);
        assertTrue(ReportItem.requestReportStatusCode(responseInfo) != null);

        responseInfo = createResponseInfo(200);
        assertTrue(ReportItem.requestReportStatusCode(responseInfo).equals("200"));
    }

    @Test
    public void testReportErrorType(){
        ResponseInfo responseInfo = createResponseInfo(200);
        assertTrue(ReportItem.qualityResult(responseInfo).equals("ok"));

        responseInfo = createResponseInfo(400);
        assertTrue(ReportItem.qualityResult(responseInfo).equals("bad_request"));

        responseInfo = createResponseInfo(ResponseInfo.ZeroSizeFile);
        assertTrue(ReportItem.qualityResult(responseInfo).equals("zero_size_file"));

        responseInfo = createResponseInfo(ResponseInfo.InvalidFile);
        assertTrue(ReportItem.qualityResult(responseInfo).equals("invalid_file"));

        responseInfo = createResponseInfo(ResponseInfo.InvalidToken);
        assertTrue(ReportItem.qualityResult(responseInfo).equals("invalid_args"));

        responseInfo = createResponseInfo(ResponseInfo.NetworkError);
        assertTrue(ReportItem.qualityResult(responseInfo).equals("network_error"));

        responseInfo = createResponseInfo(ResponseInfo.TimedOut);
        assertTrue(ReportItem.qualityResult(responseInfo).equals("timeout"));

        responseInfo = createResponseInfo(ResponseInfo.CannotConnectToHost);
        assertTrue(ReportItem.qualityResult(responseInfo).equals("cannot_connect_to_host"));

        responseInfo = createResponseInfo(ResponseInfo.NetworkConnectionLost);
        assertTrue(ReportItem.qualityResult(responseInfo).equals("transmission_error"));

        responseInfo = createResponseInfo(ResponseInfo.NetworkSSLError);
        assertTrue(ReportItem.qualityResult(responseInfo).equals("ssl_error"));

        responseInfo = createResponseInfo(ResponseInfo.ParseError);
        assertTrue(ReportItem.qualityResult(responseInfo).equals("parse_error"));

        responseInfo = createResponseInfo(ResponseInfo.MaliciousResponseError);
        assertTrue(ReportItem.qualityResult(responseInfo).equals("malicious_response"));

        responseInfo = createResponseInfo(ResponseInfo.Cancelled);
        assertTrue(ReportItem.qualityResult(responseInfo).equals("user_canceled"));

        responseInfo = createResponseInfo(ResponseInfo.LocalIOError);
        assertTrue(ReportItem.qualityResult(responseInfo).equals("local_io_error"));

        responseInfo = createResponseInfo(ResponseInfo.NetworkProtocolError);
        assertTrue(ReportItem.qualityResult(responseInfo).equals("protocol_error"));

        responseInfo = createResponseInfo(ResponseInfo.NetworkSlow);
        assertTrue(ReportItem.qualityResult(responseInfo).equals("network_slow"));

        responseInfo = createResponseInfo(614);
        assertTrue(ReportItem.qualityResult(responseInfo).equals("bad_request"));
    }


    private ResponseInfo createResponseInfo(int statusCode){

        HashMap<String, String>requestHeader = new HashMap<>();

        Request request = new Request("url", "GET", requestHeader, null, 10);

        HashMap<String, String>responseHeader = new HashMap<>();
        responseHeader.put("x-reqid", "req-id");
        responseHeader.put("x-log", "log");
        return ResponseInfo.create(request, statusCode, responseHeader, new JSONObject(), "");
    }
}
