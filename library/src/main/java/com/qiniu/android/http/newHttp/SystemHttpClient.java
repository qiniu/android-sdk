package com.qiniu.android.http.newHttp;

import com.qiniu.android.common.Constants;
import com.qiniu.android.http.CancellationHandler;
import com.qiniu.android.http.CountingRequestBody;
import com.qiniu.android.http.DnsPrefetcher;
import com.qiniu.android.http.ProxyConfiguration;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.newHttp.metrics.UploadSingleRequestMetrics;
import com.qiniu.android.utils.StringUtils;


import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private UploadSingleRequestMetrics metrics;

    @Override
    public void request(final Request request,
                        boolean isAsync,
                        ProxyConfiguration connectionProxy,
                        RequestClientProgress progress,
                        final RequestClientCompleteHandler complete) {

        metrics = new UploadSingleRequestMetrics();

        httpClient = createHttpClient(request, connectionProxy);
        okhttp3.Request.Builder requestBuilder = createRequestBuilder(request, progress);

        if (isAsync){

            ResponseTag tag = new ResponseTag();
            httpClient.newCall(requestBuilder.tag(tag).build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    int statusCode = NetworkError;
                    String msg = e.getMessage();
                    if (e instanceof CancellationHandler.CancellationException) {
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

            ResponseTag tag = new ResponseTag();
            try {
                okhttp3.Response response = httpClient.newCall(requestBuilder.tag(tag).build())
                        .execute();
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
                }
                handleError(request, statusCode, msg, complete);
            }

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
                if (DnsPrefetcher.getDnsPrefetcher().getInetAddressByHost(hostname) != null) {
                    return DnsPrefetcher.getDnsPrefetcher().getInetAddressByHost(hostname);
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
                                                         RequestClientProgress progress){

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
        } else {
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
            if (progress != null) {
                rbody = new CountingRequestBody(rbody, null, request.httpBody.length, null);
            }

            RequestBody body = RequestBody.create("", MediaType.get("application/text"));
            if (request.httpBody != null){
                body =  RequestBody.create(request.httpBody);
            }
            if (body != null){
                requestBuilder = requestBuilder.post(body);
            } else {
                requestBuilder = requestBuilder.post(body);
            }
        }
        return requestBuilder;
    }

    private EventListener createEventLister(){
        return new EventListener() {
            @Override public void callStart(Call call) {
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
            }
            @Override public void dnsEnd(Call call,
                                         String domainName,
                                         @NotNull List<InetAddress> inetAddressList) {
            }
            @Override public void connectStart(Call call,
                                               InetSocketAddress inetSocketAddress,
                                               Proxy proxy) {
            }
            @Override public void secureConnectStart(Call call) {
            }
            @Override public void secureConnectEnd(Call call,
                                                   Handshake handshake) {
            }
            @Override public void connectEnd(Call call,
                                             InetSocketAddress inetSocketAddress,
                                             Proxy proxy,
                                             Protocol protocol) {
            }
            @Override public void connectFailed(Call call,
                                                InetSocketAddress inetSocketAddress,
                                                Proxy proxy,
                                                Protocol protocol,
                                                IOException ioe) {
            }
            @Override public void connectionAcquired(Call call, Connection connection) {
            }
            @Override public void connectionReleased(Call call, Connection connection) {
            }
            @Override public void requestHeadersStart(Call call) {
            }
            @Override public void requestHeadersEnd(Call call, okhttp3.Request request) {
            }
            @Override public void requestBodyStart(Call call) {
            }
            @Override public void requestBodyEnd(Call call, long byteCount) {
            }
            @Override public void requestFailed(Call call, IOException ioe) {
            }
            @Override public void responseHeadersStart(Call call) {
            }
            @Override public void responseHeadersEnd(Call call, Response response) {
            }
            @Override public void responseBodyStart(Call call) {
            }
            @Override public void responseBodyEnd(Call call, long byteCount) {
            }
            @Override public void responseFailed(Call call, IOException ioe) {
            }
            @Override public void callEnd(Call call) {
            }
            @Override public void callFailed(Call call, IOException ioe) {
            }
        };
    }

    private void handleError(Request request,
                             int responseCode,
                             String errorMsg,
                             RequestClientCompleteHandler complete){
        ResponseInfo info = ResponseInfo.create(request, responseCode, null,null, errorMsg);
        complete.complete(info, null, info.response);
    }

    private void handleResponse(Request request,
                                okhttp3.Response response,
                                RequestClientCompleteHandler complete){

        int statusCode = response.code();

        HashMap<String, String> responseHeader = new HashMap<String, String>();
        int headerCount = response.headers().size();
        for (int i = 0; i < headerCount; i++) {
            String name = response.headers().name(i);
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
            errorMessage = new String(responseBody);
        } else if (responseContentType(response) != "application/json"){
            String responseString = new String(responseBody);
            try {
                responseJson = new JSONObject(responseString);
            } catch (Exception e) {
                errorMessage = e.getMessage();
            }
        } else {
            try {
                responseJson = buildJsonResp(responseBody);
            } catch (Exception e) {
                errorMessage = e.getMessage();
            }
        }

        final ResponseInfo info = ResponseInfo.create(request, statusCode, responseHeader, responseJson, errorMessage);
        complete.complete(info, null, info.response);
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
