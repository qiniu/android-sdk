package com.qiniu.android.http.request;

import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.UserAgent;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.http.request.handler.RequestShouldRetryHandler;
import com.qiniu.android.http.serverRegion.UploadDomainRegion;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.Crc32;
import com.qiniu.android.utils.GZipUtil;
import com.qiniu.android.utils.MD5;
import com.qiniu.android.utils.StringUtils;
import com.qiniu.android.utils.UrlSafeBase64;
import com.qiniu.android.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 请求事务
 *
 * @hidden
 */
public class RequestTransaction {

    private final Configuration config;
    private final UploadOptions uploadOption;
    private final String key;
    private final UpToken token;
    private final String userAgent;

    private UploadRequestInfo requestInfo;
    private UploadRequestState requestState;
    private HttpRegionRequest regionRequest;


    /**
     * 构造函数
     *
     * @param hosts hosts
     * @param token token
     */
    public RequestTransaction(List<String> hosts,
                              UpToken token) {
        this(new Configuration.Builder().build(), UploadOptions.defaultOptions(), hosts, null, null, token);
    }

    /**
     * 构造函数
     *
     * @param hosts    hosts
     * @param regionId 区域 ID
     * @param token    上传 Token
     */
    public RequestTransaction(List<String> hosts,
                              String regionId,
                              UpToken token) {
        this(new Configuration.Builder().build(), UploadOptions.defaultOptions(), hosts, regionId, null, token);
    }

    /**
     * 构造函数
     *
     * @param config       配置信息
     * @param uploadOption 上传选项
     * @param hosts        hosts
     * @param regionId     区域 ID
     * @param key          上传保存的  key
     * @param token        上传的 Token
     */
    public RequestTransaction(Configuration config,
                              UploadOptions uploadOption,
                              List<String> hosts,
                              String regionId,
                              String key,
                              UpToken token) {
        this(config, uploadOption, key, token);
        IUploadRegion region = new UploadDomainRegion(config);
        region.setupRegionData(ZoneInfo.buildInfo(hosts, regionId));
        this.initData(region, region);
    }

    /**
     * 构造函数
     *
     * @param config        请求事务配置
     * @param uploadOption  请求选项
     * @param targetRegion  请求的区域
     * @param currentRegion 当前的区域
     * @param key           保存的 key
     * @param token         上传的 token
     */
    public RequestTransaction(Configuration config,
                              UploadOptions uploadOption,
                              IUploadRegion targetRegion,
                              IUploadRegion currentRegion,
                              String key,
                              UpToken token) {
        this(config, uploadOption, key, token);
        this.initData(targetRegion, currentRegion);
    }

    private RequestTransaction(Configuration config,
                               UploadOptions uploadOption,
                               String key,
                               UpToken token) {
        this.config = config;
        this.uploadOption = uploadOption;
        this.key = key;
        this.token = token;
        String accessKey = "";
        if (token != null && token.accessKey != null) {
            accessKey = token.accessKey;
        }
        this.userAgent = UserAgent.instance().getUa(accessKey);
    }

    private void initData(IUploadRegion targetRegion,
                          IUploadRegion currentRegion) {

        this.requestState = new UploadRequestState();
        this.requestState.setCouldUseHttp3(config.requestClient != null);
        this.requestInfo = new UploadRequestInfo();
        this.requestInfo.targetRegionId = targetRegion.getZoneInfo().getRegionId();
        this.requestInfo.currentRegionId = currentRegion.getZoneInfo().getRegionId();
        this.requestInfo.bucket = token != null ? token.bucket : "";
        this.requestInfo.key = this.key;
        this.regionRequest = new HttpRegionRequest(config, uploadOption, token, currentRegion, this.requestInfo, this.requestState);
    }


    /**
     * 查询上传域名
     *
     * @param isAsync         是否为同步请求
     * @param completeHandler 请求结束回调
     */
    public void queryUploadHosts(boolean isAsync,
                                 final RequestCompleteHandler completeHandler) {
        requestInfo.requestType = UploadRequestInfo.RequestTypeUCQuery;

        RequestShouldRetryHandler shouldRetryHandler = new RequestShouldRetryHandler() {
            @Override
            public boolean shouldRetry(ResponseInfo responseInfo, JSONObject response) {
                return !responseInfo.isOK();
            }
        };

        HashMap<String, String> header = new HashMap<>();
        header.put("User-Agent", userAgent);
        String action = String.format("/v4/query?ak=%s&bucket=%s&sdk_version=%s&sdk_name=%s", token.accessKey, token.bucket, Utils.sdkVerion(), Utils.sdkLanguage());
        regionRequest.get(action, isAsync, header, shouldRetryHandler, new HttpRegionRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                completeAction(responseInfo, requestMetrics, response, completeHandler);
            }
        });
    }

    /**
     * 上传表单接口
     *
     * @param data            上传数据
     * @param fileName        文件名
     * @param isAsync         是否为同步
     * @param progressHandler 进度回调
     * @param completeHandler 结束回调
     */
    public void uploadFormData(byte[] data,
                               String fileName,
                               boolean isAsync,
                               final RequestProgressHandler progressHandler,
                               final RequestCompleteHandler completeHandler) {

        requestInfo.requestType = UploadRequestInfo.RequestTypeForm;

        HashMap<String, String> param = new HashMap<String, String>();
        if (uploadOption.params != null) {
            param.putAll(uploadOption.params);
        }
        if (uploadOption.metaDataParam != null) {
            param.putAll(uploadOption.metaDataParam);
        }

        if (key != null && key.length() > 0) {
            param.put("key", key);
        }

        param.put("token", token.token != null ? token.token : "");

        if (uploadOption.checkCrc) {
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

        fileName = Utils.formEscape(fileName);

        String filePairFrontString = String.format("--%s\r\n%s; name=\"%s\"; filename=\"%s\"\nContent-Type:%s\r\n\r\n", boundary, disposition, "file", fileName, uploadOption.mimeType);
        String filePairBehindString = String.format("\r\n--%s--\r\n", boundary);

        byte[] paramPair = paramPairString.toString().getBytes();
        byte[] filePairFront = filePairFrontString.getBytes();
        byte[] filePairBehind = filePairBehindString.getBytes();
        byte[] body = new byte[paramPair.length + filePairFront.length + data.length + filePairBehind.length];
        System.arraycopy(paramPair, 0, body, 0, paramPair.length);
        System.arraycopy(filePairFront, 0, body, paramPair.length, filePairFront.length);
        System.arraycopy(data, 0, body, (paramPair.length + filePairFront.length), data.length);
        System.arraycopy(filePairBehind, 0, body, (paramPair.length + filePairFront.length + data.length), filePairBehind.length);

        HashMap<String, String> header = new HashMap<String, String>();
        header.put("Content-Type", ("multipart/form-data; boundary=" + boundary));
        header.put("Content-Length", String.valueOf(body.length));
        header.put("User-Agent", userAgent);

        RequestShouldRetryHandler shouldRetryHandler = new RequestShouldRetryHandler() {
            @Override
            public boolean shouldRetry(ResponseInfo responseInfo, JSONObject response) {
                return !responseInfo.isOK();
            }
        };

        regionRequest.post(null, isAsync, body, header, shouldRetryHandler, progressHandler, new HttpRegionRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                completeAction(responseInfo, requestMetrics, response, completeHandler);
            }
        });
    }

    /**
     * make block api
     *
     * @param blockOffset     block offset
     * @param blockSize       block 大小
     * @param firstChunkData  第一块数据
     * @param isAsync         是否为异步
     * @param progressHandler 进度回调
     * @param completeHandler 结束回调
     */
    public void makeBlock(long blockOffset,
                          long blockSize,
                          byte[] firstChunkData,
                          boolean isAsync,
                          final RequestProgressHandler progressHandler,
                          final RequestCompleteHandler completeHandler) {

        requestInfo.requestType = UploadRequestInfo.RequestTypeMkblk;
        requestInfo.fileOffset = blockOffset;

        String token = String.format("UpToken %s", (this.token.token != null ? this.token.token : ""));
        HashMap<String, String> header = new HashMap<String, String>();
        header.put("Authorization", token);
        header.put("Content-Type", "application/octet-stream");
        header.put("User-Agent", userAgent);

        String action = "/mkblk/" + blockSize;
        final String chunkCrc = "" + Crc32.bytes(firstChunkData);
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
                } catch (JSONException e) {
                }

                return !responseInfo.isOK() || ctx == null || crcServer == null || !chunkCrc.equals(crcServer);
            }
        };

        regionRequest.post(action, isAsync, firstChunkData, header, shouldRetryHandler, progressHandler, new HttpRegionRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                completeAction(responseInfo, requestMetrics, response, completeHandler);
            }
        });
    }

    /**
     * upload chunk api
     *
     * @param blockContext    block ctx
     * @param blockOffset     block offset
     * @param chunkData       chunk data
     * @param chunkOffset     chunk offset
     * @param isAsync         是佛为异步
     * @param progressHandler 进度回调
     * @param completeHandler 结束回调
     */
    public void uploadChunk(String blockContext,
                            long blockOffset,
                            byte[] chunkData,
                            long chunkOffset,
                            boolean isAsync,
                            final RequestProgressHandler progressHandler,
                            final RequestCompleteHandler completeHandler) {

        requestInfo.requestType = UploadRequestInfo.RequestTypeBput;
        requestInfo.fileOffset = blockOffset + chunkOffset;

        String token = String.format("UpToken %s", (this.token.token != null ? this.token.token : ""));
        HashMap<String, String> header = new HashMap<String, String>();
        header.put("Authorization", token);
        header.put("Content-Type", "application/octet-stream");
        header.put("User-Agent", userAgent);

        String action = String.format("/bput/%s/%s", blockContext, chunkOffset + "");
        final String chunkCrc = "" + Crc32.bytes(chunkData);

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
                } catch (JSONException e) {
                }

                return !responseInfo.isOK() || ctx == null || crcServer == null || !chunkCrc.equals(crcServer);
            }
        };

        regionRequest.post(action, isAsync, chunkData, header, shouldRetryHandler, progressHandler, new HttpRegionRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                completeAction(responseInfo, requestMetrics, response, completeHandler);
            }
        });
    }

    /**
     * make file api
     *
     * @param fileSize        文件大小
     * @param fileName        文件名
     * @param blockContexts   block ctxs
     * @param isAsync         是否为异步
     * @param completeHandler 结束回调
     */
    public void makeFile(long fileSize,
                         String fileName,
                         String[] blockContexts,
                         boolean isAsync,
                         final RequestCompleteHandler completeHandler) {

        requestInfo.requestType = UploadRequestInfo.RequestTypeMkfile;

        if (blockContexts == null) {
            ResponseInfo responseInfo = ResponseInfo.invalidArgument("invalid blockContexts");
            completeAction(responseInfo, null, responseInfo.response, completeHandler);
            return;
        }

        String token = String.format("UpToken %s", (this.token.token != null ? this.token.token : ""));
        HashMap<String, String> header = new HashMap<String, String>();
        header.put("Authorization", token);
        header.put("Content-Type", "application/octet-stream");
        header.put("User-Agent", userAgent);

        String mimeType = String.format("/mimeType/%s", UrlSafeBase64.encodeToString(uploadOption.mimeType));
        String action = "/mkfile/" + fileSize + mimeType;

        if (key != null) {
            String keyStr = String.format("/key/%s", UrlSafeBase64.encodeToString(key));
            action = action + keyStr;
        }

        if (uploadOption.params != null) {
            Set<String> paramKeySet = uploadOption.params.keySet();
            for (String paramKey : paramKeySet) {
                String value = uploadOption.params.get(paramKey);
                if (value != null) {
                    String param = "/" + paramKey + "/" + UrlSafeBase64.encodeToString(value);
                    action = action + param;
                }
            }
        }

        if (uploadOption.metaDataParam != null) {
            Set<String> paramKeySet = uploadOption.metaDataParam.keySet();
            for (String paramKey : paramKeySet) {
                String value = uploadOption.metaDataParam.get(paramKey);
                if (value != null) {
                    String param = "/" + paramKey + "/" + UrlSafeBase64.encodeToString(value);
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
                return !responseInfo.isOK();
            }
        };
        regionRequest.post(action, isAsync, body, header, shouldRetryHandler, null, new HttpRegionRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                completeAction(responseInfo, requestMetrics, response, completeHandler);
            }
        });
    }


    /**
     * init part api
     *
     * @param isAsync         是否为异步
     * @param completeHandler 结束回调
     */
    public void initPart(boolean isAsync, final RequestCompleteHandler completeHandler) {

        requestInfo.requestType = UploadRequestInfo.RequestTypeInitParts;

        String token = String.format("UpToken %s", (this.token.token != null ? this.token.token : ""));
        HashMap<String, String> header = new HashMap<String, String>();
        header.put("Authorization", token);
        header.put("Content-Type", "application/octet-stream");
        header.put("User-Agent", userAgent);

        String buckets = "/buckets/" + this.token.bucket;
        String objects = "/objects/" + resumeV2EncodeKey(key);
        String action = buckets + objects + "/uploads";

        RequestShouldRetryHandler shouldRetryHandler = new RequestShouldRetryHandler() {
            @Override
            public boolean shouldRetry(ResponseInfo responseInfo, JSONObject response) {
                return !responseInfo.isOK();
            }
        };

        regionRequest.post(action, isAsync, null, header, shouldRetryHandler, null, new HttpRegionRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                completeHandler.complete(responseInfo, requestMetrics, response);
            }
        });
    }


    /**
     * upload part api
     *
     * @param isAsync         是否为异步
     * @param uploadId        上传 ID
     * @param partIndex       part index
     * @param partData        part data
     * @param progressHandler 进度回调
     * @param completeHandler 完成回调
     */
    public void uploadPart(boolean isAsync,
                           String uploadId,
                           int partIndex,
                           byte[] partData,
                           final RequestProgressHandler progressHandler,
                           final RequestCompleteHandler completeHandler) {

        requestInfo.requestType = UploadRequestInfo.RequestTypeUploadPart;

        String token = String.format("UpToken %s", (this.token.token != null ? this.token.token : ""));
        HashMap<String, String> header = new HashMap<String, String>();
        header.put("Authorization", token);
        header.put("Content-Type", "application/octet-stream");
        header.put("User-Agent", userAgent);
        if (uploadOption.checkCrc) {
            String md5 = MD5.encrypt(partData);
            if (md5 != null) {
                header.put("Content-MD5", md5);
            }
        }

        String buckets = "/buckets/" + this.token.bucket;
        String objects = "/objects/" + resumeV2EncodeKey(key);
        String uploads = "/uploads/" + uploadId;
        String partNumber = "/" + partIndex;
        String action = buckets + objects + uploads + partNumber;

        RequestShouldRetryHandler shouldRetryHandler = new RequestShouldRetryHandler() {
            @Override
            public boolean shouldRetry(ResponseInfo responseInfo, JSONObject response) {
                if (response == null) {
                    return true;
                }

                String etag = null;
                String serverMd5 = null;
                try {
                    etag = response.getString("etag");
                    serverMd5 = response.getString("md5");
                } catch (JSONException ignored) {
                }
                return !responseInfo.isOK() || etag == null || serverMd5 == null;
            }
        };
        regionRequest.put(action, isAsync, partData, header, shouldRetryHandler, progressHandler, new HttpRegionRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                completeHandler.complete(responseInfo, requestMetrics, response);
            }
        });
    }

    /**
     * complete part api
     *
     * @param isAsync         是否为同步
     * @param fileName        文件名
     * @param uploadId        上传 ID
     * @param partInfoArray   part info
     * @param completeHandler 结束回调
     */
    public void completeParts(boolean isAsync,
                              String fileName,
                              String uploadId,
                              List<Map<String, Object>> partInfoArray,
                              final RequestCompleteHandler completeHandler) {

        requestInfo.requestType = UploadRequestInfo.RequestTypeCompletePart;

        if (partInfoArray == null || partInfoArray.size() == 0) {
            ResponseInfo responseInfo = ResponseInfo.invalidArgument("partInfoArray");
            if (completeHandler != null) {
                completeHandler.complete(responseInfo, null, responseInfo.response);
            }
            return;
        }

        String token = String.format("UpToken %s", (this.token.token != null ? this.token.token : ""));
        HashMap<String, String> header = new HashMap<String, String>();
        header.put("Authorization", token);
        header.put("Content-Type", "application/octet-stream");
        header.put("User-Agent", userAgent);

        String buckets = "/buckets/" + this.token.bucket;
        String objects = "/objects/" + resumeV2EncodeKey(key);
        String uploads = "/uploads/" + uploadId;
        String action = buckets + objects + uploads;

        HashMap<String, Object> bodyMap = new HashMap<>();
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < partInfoArray.size(); i++) {
            jsonArray.put(new JSONObject(partInfoArray.get(i)));
        }
        bodyMap.put("parts", jsonArray);

        if (fileName != null) {
            bodyMap.put("fname", fileName);
        }
        if (uploadOption.mimeType != null) {
            bodyMap.put("mimeType", uploadOption.mimeType);
        }
        if (uploadOption.params != null) {
            bodyMap.put("customVars", new JSONObject(uploadOption.params));
        }
        if (uploadOption.metaDataParam != null) {
            bodyMap.put("metaData", new JSONObject(uploadOption.metaDataParam));
        }

        String bodyString = new JSONObject(bodyMap).toString();
        byte[] body = bodyString.getBytes();
        RequestShouldRetryHandler shouldRetryHandler = new RequestShouldRetryHandler() {
            @Override
            public boolean shouldRetry(ResponseInfo responseInfo, JSONObject response) {
                return !responseInfo.isOK();
            }
        };

        regionRequest.post(action, isAsync, body, header, shouldRetryHandler, null, new HttpRegionRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                completeHandler.complete(responseInfo, requestMetrics, response);
            }
        });
    }

    /**
     * report log api
     *
     * @param logData         日志数据
     * @param logClientId     日志 client id
     * @param isAsync         是否为同步
     * @param completeHandler 结束回调
     */
    public void reportLog(byte[] logData,
                          String logClientId,
                          boolean isAsync,
                          final RequestCompleteHandler completeHandler) {

        requestInfo.requestType = UploadRequestInfo.RequestTypeUpLog;

        String token = String.format("UpToken %s", (this.token.token != null ? this.token.token : ""));
        HashMap<String, String> header = new HashMap<String, String>();
        header.put("Authorization", token);
        header.put("Content-Type", "text/plain");
        header.put("User-Agent", userAgent);

        if (logClientId != null) {
            header.put("X-Log-Client-Id", logClientId);
        }

        RequestShouldRetryHandler shouldRetryHandler = new RequestShouldRetryHandler() {
            @Override
            public boolean shouldRetry(ResponseInfo responseInfo, JSONObject response) {
                return !responseInfo.isOK();
            }
        };

        regionRequest.post("/log/4?compressed=gzip", isAsync, GZipUtil.gZip(logData), header, shouldRetryHandler, null, new HttpRegionRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                completeAction(responseInfo, requestMetrics, response, completeHandler);
            }
        });
    }

    /**
     * server config
     *
     * @param isAsync         是否为异步
     * @param completeHandler 结束回调
     */
    public void serverConfig(boolean isAsync, final RequestCompleteHandler completeHandler) {

        requestInfo.requestType = UploadRequestInfo.RequestTypeServerConfig;

        HashMap<String, String> header = new HashMap<String, String>();
        header.put("User-Agent", userAgent);

        RequestShouldRetryHandler shouldRetryHandler = new RequestShouldRetryHandler() {
            @Override
            public boolean shouldRetry(ResponseInfo responseInfo, JSONObject response) {
                return !responseInfo.isOK();
            }
        };

        String action = String.format("/v1/sdk/config?sdk_name=%s&sdk_version=%s", Utils.sdkLanguage(), Utils.sdkVerion());
        regionRequest.post(action, isAsync, null, header, shouldRetryHandler, null, new HttpRegionRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                completeAction(responseInfo, requestMetrics, response, completeHandler);
            }
        });
    }

    /**
     * server user config
     *
     * @param isAsync         是否为异步
     * @param completeHandler 完成回调
     */
    public void serverUserConfig(boolean isAsync, final RequestCompleteHandler completeHandler) {

        requestInfo.requestType = UploadRequestInfo.RequestTypeServerUserConfig;

        HashMap<String, String> header = new HashMap<String, String>();
        header.put("User-Agent", userAgent);

        RequestShouldRetryHandler shouldRetryHandler = new RequestShouldRetryHandler() {
            @Override
            public boolean shouldRetry(ResponseInfo responseInfo, JSONObject response) {
                return !responseInfo.isOK();
            }
        };

        String action = String.format("/v1/sdk/config/user?ak=%s&sdk_name=%s&sdk_version=%s", token.accessKey, Utils.sdkLanguage(), Utils.sdkVerion());
        regionRequest.post(action, isAsync, null, header, shouldRetryHandler, null, new HttpRegionRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                completeAction(responseInfo, requestMetrics, response, completeHandler);
            }
        });
    }

    private String resumeV2EncodeKey(String key) {
        String encodeKey = null;
        if (key == null) {
            encodeKey = "~";
        } else if (key.equals("")) {
            encodeKey = "";
        } else {
            encodeKey = UrlSafeBase64.encodeToString(key);
        }
        return encodeKey;
    }

    private void completeAction(ResponseInfo responseInfo,
                                UploadRegionRequestMetrics requestMetrics,
                                JSONObject response,
                                RequestCompleteHandler completeHandler) {
        requestInfo = null;
        regionRequest = null;
        regionRequest = null;

        if (completeHandler != null) {
            completeHandler.complete(responseInfo, requestMetrics, response);
        }
    }

    /**
     * 请求结束回调
     *
     * @hidden
     */
    public interface RequestCompleteHandler {

        /**
         * 请求结束回调
         *
         * @param responseInfo   请求响应信息
         * @param requestMetrics 请求指标
         * @param response       请求响应
         */
        void complete(ResponseInfo responseInfo,
                      UploadRegionRequestMetrics requestMetrics,
                      JSONObject response);
    }

}
