package com.qiniu.android.http.request.httpclient;

import com.qiniu.android.common.Constants;
import com.qiniu.android.http.CancellationHandler;
import com.qiniu.android.http.CountingRequestBody;
import com.qiniu.android.http.ProgressHandler;
import com.qiniu.android.http.ProxyConfiguration;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.dns.DnsPrefetcher;
import com.qiniu.android.http.request.Request;
import com.qiniu.android.http.request.RequestClient;
import com.qiniu.android.http.metrics.UploadSingleRequestMetrics;
import com.qiniu.android.utils.AndroidNetwork;
import com.qiniu.android.utils.StringUtils;


import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.qiniu.android.http.ResponseInfo.NetworkError;

public class SystemHttpClient implements RequestClient {

    public static final String ContentTypeHeader = "Content-Type";
    public static final String DefaultMime = "application/octet-stream";
    public static final String JsonMime = "application/json";
    public static final String FormMime = "application/x-www-form-urlencoded";

    private OkHttpClient httpClient;
    private Call call;
    private UploadSingleRequestMetrics metrics;

    @Override
    public void request(final Request request,
                        boolean isAsync,
                        ProxyConfiguration connectionProxy,
                        RequestClientProgress progress,
                        final RequestClientCompleteHandler complete) {

        metrics = new UploadSingleRequestMetrics();
        metrics.request = request;

        httpClient = createHttpClient(request, connectionProxy);
        okhttp3.Request.Builder requestBuilder = createRequestBuilder(request, progress);
        if (requestBuilder == null){
            ResponseInfo responseInfo = ResponseInfo.invalidArgument("invalid http request");
            handleError(request, responseInfo.statusCode, responseInfo.message, complete);
            return;
        }

        ResponseTag tag = new ResponseTag();
        call = httpClient.newCall(requestBuilder.tag(tag).build());

        if (isAsync){
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    int statusCode = NetworkError;
                    String msg = e.getMessage();
                    if (e.getMessage().contains("Canceled")){
                        statusCode = ResponseInfo.Cancelled;
                    } else if (e instanceof CancellationHandler.CancellationException) {
                        statusCode = ResponseInfo.Cancelled;
                    } else if (e instanceof UnknownHostException) {
                        statusCode = ResponseInfo.UnknownHost;
                    } else if (msg != null && msg.indexOf("Broken pipe") == 0) {
                        statusCode = ResponseInfo.NetworkConnectionLost;
                    } else if (e instanceof SocketTimeoutException) {
                        statusCode = ResponseInfo.TimedOut;
                    } else if (e instanceof java.net.ConnectException) {
                        statusCode = ResponseInfo.CannotConnectToHost;
                    }
                    handleError(request, statusCode, msg, complete);
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    handleResponse(request, response, complete);
                }
            });

        } else {
            try {
                okhttp3.Response response = call.execute();
                handleResponse(request, response, complete);
            } catch (Exception e) {
                e.printStackTrace();
                int statusCode = NetworkError;
                String msg = e.getMessage();
                if (e instanceof UnknownHostException) {
                    statusCode = ResponseInfo.UnknownHost;
                } else if (msg != null && msg.indexOf("Broken pipe") == 0) {
                    statusCode = ResponseInfo.NetworkConnectionLost;
                } else if (e instanceof SocketTimeoutException) {
                    statusCode = ResponseInfo.TimedOut;
                } else if (e instanceof java.net.ConnectException) {
                    statusCode = ResponseInfo.CannotConnectToHost;
                } else if(e instanceof SSLException){
                    statusCode = ResponseInfo.NetworkSSLError;
                }
                handleError(request, statusCode, msg, complete);
            }

        }
    }

    @Override
    public synchronized void cancel() {
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
    }

    private OkHttpClient createHttpClient(Request request,
                                          ProxyConfiguration connectionProxy){

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        if (connectionProxy != null) {
            clientBuilder.proxy(connectionProxy.proxy());
            if (connectionProxy.user != null && connectionProxy.password != null) {
                clientBuilder.proxyAuthenticator(connectionProxy.authenticator());
            }
        }

        clientBuilder.dns(new okhttp3.Dns() {
            @Override
            public List<InetAddress> lookup(String hostname) throws UnknownHostException {
                if (DnsPrefetcher.getInstance().getInetAddressByHost(hostname) != null) {
                    return DnsPrefetcher.getInstance().getInetAddressByHost(hostname);
                }
                return okhttp3.Dns.SYSTEM.lookup(hostname);
            }
        });

        clientBuilder.eventListener(createEventLister());

        clientBuilder.networkInterceptors().add(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                okhttp3.Request request = chain.request();
                final long before = System.currentTimeMillis();
                okhttp3.Response response = chain.proceed(request);
                final long after = System.currentTimeMillis();

                ResponseTag tag = (ResponseTag) request.tag();
                String ip = "";
                try {
                    ip = chain.connection().socket().getRemoteSocketAddress().toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                tag.ip = ip;
                tag.duration = after - before;
                return response;
            }
        });

        clientBuilder.connectTimeout(request.timeout, TimeUnit.SECONDS);
        clientBuilder.readTimeout(request.timeout, TimeUnit.SECONDS);
        clientBuilder.writeTimeout(0, TimeUnit.SECONDS);

        return clientBuilder.build();
    }

    private okhttp3.Request.Builder createRequestBuilder(Request request,
                                                         final RequestClientProgress progress){

        Headers allHeaders = null;
        if (request.allHeaders != null){
            allHeaders = Headers.of(request.allHeaders);
        }

        okhttp3.Request.Builder requestBuilder = null;
        if (request.httpMethod == Request.HttpMethodGet){
            requestBuilder = new okhttp3.Request.Builder().get().url(request.urlString);
            if (allHeaders != null && request.allHeaders != null){
                for (String key : request.allHeaders.keySet()){
                    String value = request.allHeaders.get(key);
                    requestBuilder.header(key, value);
                }
            }
        } else if (request.httpMethod == Request.HttpMethodPOST){
            requestBuilder = new okhttp3.Request.Builder().url(request.urlString);

            if (allHeaders != null){
                requestBuilder = requestBuilder.headers(allHeaders);
            }

            RequestBody rbody;
            if (request.httpBody != null && request.httpBody.length > 0) {
                MediaType type = MediaType.parse(DefaultMime);
                if (request.allHeaders != null) {
                    String contentType = request.allHeaders.get(ContentTypeHeader);
                    if (contentType != null) {
                        type = MediaType.parse(contentType);
                    }
                }
                rbody = RequestBody.create(type, request.httpBody, 0, request.httpBody.length);
            } else {
                rbody = RequestBody.create(null, new byte[0]);
            }
            rbody = new CountingRequestBody(rbody, new ProgressHandler() {
                @Override
                public void onProgress(long bytesWritten, long totalSize) {
                if (progress != null){
                    progress.progress(bytesWritten, totalSize);
                }
                }
            }, request.httpBody.length, null);

            requestBuilder = requestBuilder.post(rbody);
        }
        return requestBuilder;
    }

    private EventListener createEventLister(){
        return new EventListener() {
            @Override public void callStart(Call call) {
                metrics.startDate = new Date();
            }
            @Override public void proxySelectStart(Call call,
                                                   HttpUrl url) {
            }
            @Override public void proxySelectEnd(Call call,
                                                 HttpUrl url,
                                                 List<Proxy> proxies) {
            }
            @Override public void dnsStart(Call call,
                                           String domainName) {
                metrics.domainLookupStartDate = new Date();
            }
            @Override public void dnsEnd(Call call,
                                         String domainName,
                                         @NotNull List<InetAddress> inetAddressList) {
                metrics.domainLookupEndDate = new Date();
            }
            @Override public void connectStart(Call call,
                                               InetSocketAddress inetSocketAddress,
                                               Proxy proxy) {
                metrics.connectStartDate = new Date();
                metrics.remoteAddress = inetSocketAddress.getAddress().getHostAddress();
                metrics.remotePort = inetSocketAddress.getPort();
                metrics.localAddress = AndroidNetwork.getHostIP();
            }
            @Override public void secureConnectStart(Call call) {
                metrics.connectEndDate = new Date();
            }
            @Override public void secureConnectEnd(Call call,
                                                   Handshake handshake) {
                metrics.secureConnectionStartDate = new Date();
            }
            @Override public void connectEnd(Call call,
                                             InetSocketAddress inetSocketAddress,
                                             Proxy proxy,
                                             Protocol protocol) {
                metrics.secureConnectionEndDate = new Date();
            }
            @Override public void connectFailed(Call call,
                                                InetSocketAddress inetSocketAddress,
                                                Proxy proxy,
                                                Protocol protocol,
                                                IOException ioe) {
                metrics.connectEndDate = new Date();
            }
            @Override public void connectionAcquired(Call call, Connection connection) {
            }
            @Override public void connectionReleased(Call call, Connection connection) {
            }
            @Override public void requestHeadersStart(Call call) {
                metrics.requestStartDate = new Date();
            }
            @Override public void requestHeadersEnd(Call call, okhttp3.Request request) {
                metrics.countOfRequestHeaderBytesSent = request.headers().toString().length();
            }
            @Override public void requestBodyStart(Call call) {
            }
            @Override public void requestBodyEnd(Call call, long byteCount) {
                metrics.requestEndDate = new Date();
                metrics.countOfRequestBodyBytesSent = byteCount;
            }
            @Override public void requestFailed(Call call, IOException ioe) {
                metrics.requestEndDate = new Date();
                metrics.countOfRequestBodyBytesSent = 0;
            }
            @Override public void responseHeadersStart(Call call) {
                metrics.responseStartDate = new Date();
            }
            @Override public void responseHeadersEnd(Call call, Response response) {

            }
            @Override public void responseBodyStart(Call call) {
            }
            @Override public void responseBodyEnd(Call call, long byteCount) {
                metrics.responseEndDate = new Date();
            }
            @Override public void responseFailed(Call call, IOException ioe) {
                metrics.responseEndDate = new Date();
            }
            @Override public void callEnd(Call call) {
                metrics.endDate = new Date();
            }
            @Override public void callFailed(Call call, IOException ioe) {
                metrics.endDate = new Date();
            }
        };
    }

    private synchronized void handleError(Request request,
                             int responseCode,
                             String errorMsg,
                             RequestClientCompleteHandler complete){
        if (metrics == null || metrics.response != null) {
            return;
        }

        ResponseInfo info = ResponseInfo.create(request, responseCode, null,null, errorMsg);
        metrics.response = info;
        complete.complete(info, metrics, info.response);

        releaseResource();
    }

    private synchronized void handleResponse(Request request,
                                okhttp3.Response response,
                                RequestClientCompleteHandler complete){
        if (metrics == null || metrics.response != null) {
            return;
        }

        int statusCode = response.code();

        HashMap<String, String> responseHeader = new HashMap<String, String>();
        int headerCount = response.headers().size();
        for (int i = 0; i < headerCount; i++) {
            String name = response.headers().name(i).toLowerCase();
            String value = response.headers().value(i);
            responseHeader.put(name, value);
        }

        byte[] responseBody = null;
        JSONObject responseJson = null;
        String errorMessage = null;
        try {
            responseBody = response.body().bytes();
        } catch (IOException e) {
            errorMessage = e.getMessage();
        }

        if (responseBody == null){
            errorMessage = response.message();
        } else if (responseContentType(response) != "application/json"){
            String responseString = new String(responseBody);
            if (responseString != null && responseString.length() > 0){
                try {
                    responseJson = new JSONObject(responseString);
                } catch (Exception e) {}
            }
        } else {
            try {
                responseJson = buildJsonResp(responseBody);
            } catch (Exception e) {
                errorMessage = e.getMessage();
            }
        }


        final ResponseInfo info = ResponseInfo.create(request, statusCode, responseHeader, responseJson, errorMessage);
        metrics.response = info;
        complete.complete(info, metrics, info.response);

        releaseResource();
    }

    private void releaseResource(){
        this.httpClient = null;
        this.call = null;
    }

    private static String responseContentType(okhttp3.Response response) {
        MediaType mediaType = response.body().contentType();
        if (mediaType == null) {
            return "";
        }
        return mediaType.type() + "/" + mediaType.subtype();
    }

    private static JSONObject buildJsonResp(byte[] body) throws Exception {
        String str = new String(body, Constants.UTF_8);
        // 允许 空 字符串
        if (StringUtils.isNullOrEmpty(str)) {
            return new JSONObject();
        }
        return new JSONObject(str);
    }


    private static class ResponseTag {
        public String ip = "";
        public long duration = -1;
    }
}
