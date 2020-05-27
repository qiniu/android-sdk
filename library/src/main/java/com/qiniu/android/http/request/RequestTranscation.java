package com.qiniu.android.http.request;

import android.util.Log;

import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.UserAgent;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.http.request.handler.RequestShouldRetryHandler;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.serverRegion.UploadDomainRegion;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.Crc32;
import com.qiniu.android.utils.StringUtils;
import com.qiniu.android.utils.UrlSafeBase64;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class RequestTranscation {

    private final Configuration config;
    private final UploadOptions uploadOption;
    private final String key;
    private final UpToken token;
    private final String userAgent;

    private UploadRequestInfo requestInfo;
    private UploadRequstState requstState;
    private HttpRegionRequest regionRequest;


    public RequestTranscation(ArrayList<String> hosts,
                              UpToken token){
        this(new Configuration.Builder().build(), UploadOptions.defaultOptions(), hosts, null, token);
    }

    public RequestTranscation(Configuration config,
                              UploadOptions uploadOption,
                              ArrayList<String> hosts,
                              String key,
                              UpToken token){
        this(config, uploadOption, key, token);
        UploadRegion region = new UploadDomainRegion();
        region.setupRegionData(ZoneInfo.buildInfo(hosts, null));
        this.initData(region, region);
    }

    public RequestTranscation(Configuration config,
                              UploadOptions uploadOption,
                              UploadRegion targetRegion,
                              UploadRegion currentRegion,
                              String key,
                              UpToken token) {
        this(config, uploadOption, key, token);
        this.initData(targetRegion, currentRegion);
    }

    private RequestTranscation(Configuration config,
                               UploadOptions uploadOption,
                               String key,
                               UpToken token) {
        this.config = config;
        this.uploadOption = uploadOption;
        this.key = key;
        this.token = token;
        this.userAgent = UserAgent.instance().getUa((token.accessKey != null ? token.accessKey : "pandora"));
    }

    private void initData(UploadRegion targetRegion,
                          UploadRegion currentRegion){

        this.requstState = new UploadRequstState();
        this.requestInfo = new UploadRequestInfo();
        this.requestInfo.targetRegionId = targetRegion.getZoneInfo().getRegionId();
        this.requestInfo.currentRegionId = currentRegion.getZoneInfo().getRegionId();
        this.requestInfo.bucket = token.bucket;
        this.regionRequest = new HttpRegionRequest(config, uploadOption, token, currentRegion, this.requestInfo, this.requstState);
    }


    public void quertUploadHosts(boolean isAsyn,
                                 final RequestCompleteHandler completeHandler){
        requestInfo.requestType = UploadRequestInfo.RequestTypeUCQuery;

        RequestShouldRetryHandler shouldRetryHandler = new RequestShouldRetryHandler() {
            @Override
            public boolean shouldRetry(ResponseInfo responseInfo, JSONObject response) {
                if (!responseInfo.isOK()){
                    return true;
                } else {
                    return false;
                }
            }
        };

        HashMap<String, String> header = new HashMap<>();
        header.put("User-Agent", userAgent);
        String action = "/v3/query?ak=" + (token.accessKey != null ? token.accessKey : "") + "&bucket=" + (token.bucket != null ? token.bucket : "") ;
        regionRequest.get(action, isAsyn, header, shouldRetryHandler, new HttpRegionRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                completeHandler.complete(responseInfo, requestMetrics, response);
            }
        });
    }

    public void uploadFormData(byte[] data,
                               String fileName,
                               boolean isAsyn,
                               final RequestProgressHandler progressHandler,
                               final RequestCompleteHandler completeHandler){

        requestInfo.requestType = UploadRequestInfo.RequestTypeForm;

        HashMap<String, String> param = new HashMap<String, String>();
        if (uploadOption.params != null){
            param.putAll(uploadOption.params);
        }

        if (key != null && key.length() > 0){
            param.put("key", key);
        }

        param.put("token", token.token != null ? token.token : "");

        if (uploadOption.checkCrc){
            param.put("crc32", String.valueOf(Crc32.bytes(data)));
        }

        String boundary = "werghnvt54wef654rjuhgb56trtg34tweuyrgf";
        String disposition = "Content-Disposition: form-data";

        StringBuilder paramPairString = new StringBuilder();
        Set<String> paramKeySet = param.keySet();
        for (String key : paramKeySet) {
            String value = param.get(key);
            paramPairString.append(String.format("--%s\r\n%s; name=\"%s\"\r\n\r\n", boundary, disposition, key));
            paramPairString.append(String.format("%s\r\n", value));
        }

        String filePairFrontString = String.format("--%s\r\n%s; name=\"%s\"; filename=\"%s\"\nContent-Type:%s\r\n\r\n", boundary, disposition, "file", fileName, uploadOption.mimeType);
        String filePairBehindString = String.format("\r\n--%s--\r\n", boundary);

        byte[] paramPair = paramPairString.toString().getBytes();
        byte[] filePairFront = filePairFrontString.toString().getBytes();
        byte[] filePairBehind = filePairBehindString.toString().getBytes();
        byte[] body = new byte[paramPair.length + filePairFront.length + data.length + filePairBehind.length];
        System.arraycopy(paramPair, 0, body, 0, paramPair.length);
        System.arraycopy(filePairFront, 0, body, paramPair.length, filePairFront.length);
        System.arraycopy(data, 0, body, (paramPair.length + filePairFront.length), data.length);
        System.arraycopy(filePairBehind, 0, body, (paramPair.length + filePairFront.length + data.length), filePairBehind.length);

        HashMap <String, String> header = new HashMap<String, String>();
        header.put("Content-Type", ("multipart/form-data; boundary=" + boundary));
        header.put("Content-Length", String.valueOf(body.length));
        header.put("User-Agent", userAgent);

        RequestShouldRetryHandler shouldRetryHandler = new RequestShouldRetryHandler() {
            @Override
            public boolean shouldRetry(ResponseInfo responseInfo, JSONObject response) {
            if (!responseInfo.isOK()){
                return true;
            } else {
                return false;
            }
            }
        };
        regionRequest.post(null, isAsyn, body, header, shouldRetryHandler, progressHandler, new HttpRegionRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
            completeHandler.complete(responseInfo, requestMetrics, response);
            }
        });
    }

    public void makeBlock(long blockOffset,
                          long blockSize,
                          byte[] firstChunkData,
                          boolean isAsyn,
                          final RequestProgressHandler progressHandler,
                          final RequestCompleteHandler completeHandler){

        requestInfo.requestType = UploadRequestInfo.RequestTypeMkblk;
        requestInfo.fileOffset = new Long(blockOffset);

        String token = String.format("UpToken %s", (this.token.token != null ? this.token.token : ""));
        HashMap <String, String> header = new HashMap<String, String>();
        header.put("Authorization", token);
        header.put("Content-Type", "application/octet-stream");
        header.put("User-Agent", userAgent);

        String action = String.format("/mkblk/%d", blockSize);
        final String chunkCrc = String.format("%d", Crc32.bytes(firstChunkData));
        RequestShouldRetryHandler shouldRetryHandler = new RequestShouldRetryHandler() {
            @Override
            public boolean shouldRetry(ResponseInfo responseInfo, JSONObject response) {
            if (response == null) {
                return true;
            }

            String ctx = null;
            String crcServer = null;
            try {
                ctx = response.getString("ctx");
                crcServer = String.valueOf(response.getLong("crc32"));
            } catch (JSONException e) {}

            if (!responseInfo.isOK()
                || ctx == null || crcServer == null || (!chunkCrc.equals(crcServer))){
                return true;
            } else {
                return false;
            }
            }
        };
        regionRequest.post(action, isAsyn, firstChunkData, header, shouldRetryHandler, progressHandler, new HttpRegionRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
            completeHandler.complete(responseInfo, requestMetrics, response);
            }
        });
    }

    public void uploadChunk(String blockContext,
                            long blockOffset,
                            byte[] chunkData,
                            long chunkOffest,
                            boolean isAsyn,
                            final RequestProgressHandler progressHandler,
                            final RequestCompleteHandler completeHandler){

        requestInfo.requestType = UploadRequestInfo.RequestTypeBput;
        requestInfo.fileOffset = new Long((blockOffset + chunkOffest));

        String token = String.format("UpToken %s", (this.token.token != null ? this.token.token : ""));
        HashMap <String, String> header = new HashMap<String, String>();
        header.put("Authorization", token);
        header.put("Content-Type", "application/octet-stream");
        header.put("User-Agent", userAgent);

        String action = String.format("/bput/%s/%d", blockContext, chunkOffest);
        final String chunkCrc = String.format("%d", Crc32.bytes(chunkData));

        Log.i("== request transcation", String.format("blockOffset:%d chunkOffest:%d chunkSize:%d", blockOffset, chunkOffest, chunkData.length));

        RequestShouldRetryHandler shouldRetryHandler = new RequestShouldRetryHandler() {
            @Override
            public boolean shouldRetry(ResponseInfo responseInfo, JSONObject response) {
            if (response == null) {
                return true;
            }

            String ctx = null;
            String crcServer = null;
            try {
                ctx = response.getString("ctx");
                crcServer = String.valueOf(response.getLong("crc32"));
            } catch (JSONException e) {}

            if (!responseInfo.isOK()
                || ctx == null || crcServer == null || (!chunkCrc.equals(crcServer))){
                return true;
            } else {
                return false;
            }
            }
        };
        regionRequest.post(action, isAsyn, chunkData, header, shouldRetryHandler, progressHandler, new HttpRegionRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
            completeHandler.complete(responseInfo, requestMetrics, response);
            }
        });
    }

    public void makeFile(long fileSize,
                         String fileName,
                         String[] blockContexts,
                         boolean isAsyn,
                         final RequestCompleteHandler completeHandler){

        requestInfo.requestType = UploadRequestInfo.RequestTypeMkfile;

        if (blockContexts == null){
            ResponseInfo responseInfo = ResponseInfo.invalidArgument("invalid blockcontexts");
            completeHandler.complete(responseInfo, null, responseInfo.response);
            return;
        }

        String token = String.format("UpToken %s", (this.token.token != null ? this.token.token : ""));
        HashMap <String, String> header = new HashMap<String, String>();
        header.put("Authorization", token);
        header.put("Content-Type", "application/octet-stream");
        header.put("User-Agent", userAgent);

        String mimeType = String.format("/mimeType/%s", UrlSafeBase64.encodeToString(uploadOption.mimeType));
        String action =  String.format("/mkfile/%d%s", fileSize, mimeType);

        if (key != null){
            String keyStr = String.format("/key/%s", UrlSafeBase64.encodeToString(key));
            action = action + keyStr;
        }

        if (uploadOption.params != null){
            Set <String> paramKeySet = uploadOption.params.keySet();
            for (String paramKey : paramKeySet) {
                String value = uploadOption.params.get(paramKey);
                if (value != null){
                    String param = "/" + paramKey + "/" + value;
                    action = action + param;
                }
            }
        }

        String fname = String.format("/fname/%s", UrlSafeBase64.encodeToString(fileName));
        action = action + fname;

        byte[] body = StringUtils.join(blockContexts, ",").getBytes();
        RequestShouldRetryHandler shouldRetryHandler = new RequestShouldRetryHandler() {
            @Override
            public boolean shouldRetry(ResponseInfo responseInfo, JSONObject response) {
            if (!responseInfo.isOK()){
                return true;
            } else {
                return false;
            }
            }
        };
        regionRequest.post(action, isAsyn, body, header, shouldRetryHandler, null, new HttpRegionRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
            completeHandler.complete(responseInfo, requestMetrics, response);
            }
        });
    }

    public void reportLog(byte[] logData,
                          String logClientId,
                          boolean isAsyn,
                          final RequestCompleteHandler completeHandler){

        requestInfo.requestType = UploadRequestInfo.RequestTypeUpLog;

        String token = String.format("UpToken %s", (this.token.token != null ? this.token.token : ""));
        HashMap <String, String> header = new HashMap<String, String>();
        header.put("Authorization", token);
        header.put("Content-Type", "text/plain");
        header.put("User-Agent", userAgent);

        if (logClientId != null){
            header.put("X-Log-Client-Id", logClientId);
        }

        RequestShouldRetryHandler shouldRetryHandler = new RequestShouldRetryHandler() {
            @Override
            public boolean shouldRetry(ResponseInfo responseInfo, JSONObject response) {
                if (!responseInfo.isOK()){
                    return true;
                } else {
                    return false;
                }
            }
        };
        regionRequest.post(null, isAsyn, logData, header, shouldRetryHandler, null, new HttpRegionRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                completeHandler.complete(responseInfo, requestMetrics, response);
            }
        });
    }

    public interface RequestCompleteHandler {
        public void complete(ResponseInfo responseInfo,
                             UploadRegionRequestMetrics requestMetrics,
                             JSONObject response);
    }

}
