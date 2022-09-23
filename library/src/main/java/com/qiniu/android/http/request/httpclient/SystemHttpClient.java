package com.qiniu.android.http.request.httpclient;

import com.qiniu.android.common.Constants;
import com.qiniu.android.http.CancellationHandler;
import com.qiniu.android.http.ProgressHandler;
import com.qiniu.android.http.ProxyConfiguration;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.dns.SystemDns;
import com.qiniu.android.http.request.IUploadServer;
import com.qiniu.android.http.request.Request;
import com.qiniu.android.http.request.IRequestClient;
import com.qiniu.android.http.metrics.UploadSingleRequestMetrics;
import com.qiniu.android.storage.GlobalConfiguration;
import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.StringUtils;


import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Connection;
import okhttp3.ConnectionPool;
import okhttp3.Dns;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.qiniu.android.http.ResponseInfo.NetworkError;

public class SystemHttpClient extends IRequestClient {

    public static final String ContentTypeHeader = "Content-Type";
    public static final String DefaultMime = "application/octet-stream";
    public static final String JsonMime = "application/json";
    public static final String FormMime = "application/x-www-form-urlencoded";

    private boolean hasHandleComplete = false;
    private static ConnectionPool pool;
    private IUploadServer currentServer;
    private Request currentRequest;
    private static final OkHttpClient baseClient = new OkHttpClient();
    private OkHttpClient httpClient;
    private Call call;
    private UploadSingleRequestMetrics metrics;
    private Progress requestProgress;
    private CompleteHandler completeHandler;

    public void request(Request request,
                        boolean isAsync,
                        ProxyConfiguration connectionProxy,
                        Progress progress,
                        CompleteHandler complete) {
        request(request, new Options(null, isAsync, connectionProxy), progress, complete);
    }

    @Override
    public void request(Request request,
                        Options options,
                        Progress progress,
                        CompleteHandler complete) {
        IUploadServer server = null;
        boolean isAsync = true;
        ProxyConfiguration connectionProxy = null;
        if (options != null) {
            server = options.server;
            isAsync = options.isAsync;
            connectionProxy = options.connectionProxy;
        }

        metrics = new UploadSingleRequestMetrics();
        metrics.start();
        metrics.setClientName(getClientId());
        metrics.setClientVersion(getOkHttpVersion());
        if (server != null) {
            currentServer = server;
            metrics.setRemoteAddress(server.getIp());
        }
        metrics.setRequest(request);
        currentRequest = request;
        requestProgress = progress;
        completeHandler = complete;

        httpClient = createHttpClient(connectionProxy);

        okhttp3.Request.Builder requestBuilder = createRequestBuilder(requestProgress);
        if (requestBuilder == null) {
            ResponseInfo responseInfo = ResponseInfo.invalidArgument("invalid http request");
            handleError(request, responseInfo.statusCode, responseInfo.message, complete);
            return;
        }

        call = httpClient.newCall(requestBuilder.build());

        if (isAsync) {
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    String msg = e.getMessage();
                    int status = getStatusCodeByException(e);
                    if (call.isCanceled()) {
                        status = ResponseInfo.Cancelled;
                        msg = "user cancelled";
                    }
                    handleError(currentRequest, status, msg, completeHandler);
                }

                @Override
                public void onResponse(Call call, final okhttp3.Response response) throws IOException {
                    AsyncRun.runInBack(new Runnable() {
                        @Override
                        public void run() {
                            handleResponse(currentRequest, response, completeHandler);
                        }
                    });
                }
            });

        } else {
            try {
                okhttp3.Response response = call.execute();
                handleResponse(request, response, complete);
            } catch (Exception e) {
                e.printStackTrace();
                String msg = e.getMessage();
                int status = getStatusCodeByException(e);
                if (call.isCanceled()) {
                    status = ResponseInfo.Cancelled;
                    msg = "user cancelled";
                }
                handleError(request, status, msg, complete);
            }
        }
    }

    @Override
    public synchronized void cancel() {
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
    }

    @Override
    public String getClientId() {
        return "okhttp";
    }

    private OkHttpClient createHttpClient(ProxyConfiguration connectionProxy) {
        if (currentRequest == null) {
            return null;
        }

        OkHttpClient.Builder clientBuilder = baseClient.newBuilder();

        if (connectionProxy != null) {
            clientBuilder.proxy(connectionProxy.proxy());
            if (connectionProxy.user != null && connectionProxy.password != null) {
                clientBuilder.proxyAuthenticator(connectionProxy.authenticator());
            }
        }

        clientBuilder.eventListener(createEventLister());

        if (GlobalConfiguration.getInstance().isDnsOpen) {
            clientBuilder.dns(new Dns() {
                @Override
                public List<InetAddress> lookup(String s) throws UnknownHostException {
                    if (currentServer != null && s.equals(currentServer.getHost())) {
                        InetAddress address = currentServer.getInetAddress();
                        if (address != null) {
                            List<InetAddress> inetAddressList = new ArrayList<>();
                            inetAddressList.add(address);
                            return inetAddressList;
                        } else {
                            return new SystemDns().lookupInetAddress(s);
                        }
                    } else {
                        return new SystemDns().lookupInetAddress(s);
                    }
                }
            });
        }

        clientBuilder.connectionPool(SystemHttpClient.getConnectPool());

        clientBuilder.connectTimeout(currentRequest.connectTimeout, TimeUnit.SECONDS);
        clientBuilder.readTimeout(currentRequest.readTimeout, TimeUnit.SECONDS);
        clientBuilder.writeTimeout(currentRequest.writeTimeout, TimeUnit.SECONDS);

        return clientBuilder.build();
    }

    private synchronized static ConnectionPool getConnectPool() {
        if (pool == null) {
            pool = new ConnectionPool(10, 10, TimeUnit.MINUTES);
        }
        return pool;
    }

    private okhttp3.Request.Builder createRequestBuilder(final Progress progress) {
        if (currentRequest == null) {
            return null;
        }

        Headers allHeaders = Headers.of(currentRequest.allHeaders);

        okhttp3.Request.Builder requestBuilder = null;
        if (currentRequest.httpMethod.equals(Request.HttpMethodHEAD) ||
                currentRequest.httpMethod.equals(Request.HttpMethodGet)) {
            requestBuilder = new okhttp3.Request.Builder().get().url(currentRequest.urlString);
            for (String key : currentRequest.allHeaders.keySet()) {
                String value = currentRequest.allHeaders.get(key);
                requestBuilder.header(key, value);
            }
        } else if (currentRequest.httpMethod.equals(Request.HttpMethodPOST) ||
                currentRequest.httpMethod.equals(Request.HttpMethodPUT)) {
            requestBuilder = new okhttp3.Request.Builder().url(currentRequest.urlString);
            requestBuilder = requestBuilder.headers(allHeaders);

            RequestBody rbody;
            if (currentRequest.httpBody.length > 0) {
                MediaType type = MediaType.parse(DefaultMime);
                String contentType = currentRequest.allHeaders.get(ContentTypeHeader);
                if (contentType != null) {
                    type = MediaType.parse(contentType);
                }
                rbody = new ByteBody(type, currentRequest.httpBody);
            } else {
                rbody = new ByteBody(null, new byte[0]);
            }
            rbody = new CountingRequestBody(rbody, new ProgressHandler() {
                @Override
                public void onProgress(long bytesWritten, long totalSize) {
                    if (progress != null) {
                        progress.progress(bytesWritten, totalSize);
                    }
                }
            }, currentRequest.httpBody.length, null);

            if (currentRequest.httpMethod.equals(Request.HttpMethodPOST)) {
                requestBuilder = requestBuilder.post(rbody);
            } else if (currentRequest.httpMethod.equals(Request.HttpMethodPUT)) {
                requestBuilder = requestBuilder.put(rbody);
            }

        }
        return requestBuilder;
    }

    private EventListener createEventLister() {
        return new EventListener() {
            @Override
            public void callStart(Call call) {
            }

            @Override
            public void dnsStart(Call call,
                                 String domainName) {
                metrics.setDomainLookupStartDate(new Date());
            }

            @Override
            public void dnsEnd(Call call,
                               String domainName,
                               List<InetAddress> inetAddressList) {
                metrics.setDomainLookupEndDate(new Date());
            }

            @Override
            public void connectStart(Call call,
                                     InetSocketAddress inetSocketAddress,
                                     Proxy proxy) {
                metrics.setConnectStartDate(new Date());
                if (inetSocketAddress != null && inetSocketAddress.getAddress() != null) {
                    metrics.setRemoteAddress(inetSocketAddress.getAddress().getHostAddress());
                    metrics.setRemotePort(inetSocketAddress.getPort());
                }
            }

            @Override
            public void secureConnectStart(Call call) {
                metrics.setConnectEndDate(new Date());
            }

            @Override
            public void secureConnectEnd(Call call,
                                         Handshake handshake) {
                metrics.setSecureConnectionStartDate(new Date());
            }

            @Override
            public void connectEnd(Call call,
                                   InetSocketAddress inetSocketAddress,
                                   Proxy proxy,
                                   Protocol protocol) {
                metrics.setSecureConnectionEndDate(new Date());
            }

            @Override
            public void connectFailed(Call call,
                                      InetSocketAddress inetSocketAddress,
                                      Proxy proxy,
                                      Protocol protocol,
                                      IOException ioe) {
                metrics.setConnectEndDate(new Date());
            }

            @Override
            public void connectionAcquired(Call call, Connection connection) {
            }

            @Override
            public void connectionReleased(Call call, Connection connection) {
            }

            @Override
            public void requestHeadersStart(Call call) {
                metrics.setRequestStartDate(new Date());
            }

            @Override
            public void requestHeadersEnd(Call call, okhttp3.Request request) {
                metrics.setCountOfRequestHeaderBytesSent(request.headers().toString().length());
            }

            @Override
            public void requestBodyStart(Call call) {
            }

            @Override
            public void requestBodyEnd(Call call, long byteCount) {
                metrics.setRequestEndDate(new Date());
                metrics.setCountOfRequestBodyBytesSent(byteCount);
            }

            public void requestFailed(Call call, IOException ioe) {
                metrics.setCountOfRequestBodyBytesSent(0);
            }

            @Override
            public void responseHeadersStart(Call call) {
                metrics.setResponseStartDate(new Date());
            }

            @Override
            public void responseHeadersEnd(Call call, Response response) {
                Headers headers = response.headers();
                if (headers != null && headers.byteCount() > 0) {
                    metrics.setCountOfResponseHeaderBytesReceived(headers.byteCount());
                }
            }

            @Override
            public void responseBodyStart(Call call) {
            }

            @Override
            public void responseBodyEnd(Call call, long byteCount) {
                metrics.setResponseEndDate(new Date());
                metrics.setCountOfResponseBodyBytesReceived(byteCount);
            }

            public void responseFailed(Call call, IOException ioe) {
                metrics.setResponseEndDate(new Date());
            }

            @Override
            public void callEnd(Call call) {
                metrics.end();
            }

            @Override
            public void callFailed(Call call, IOException ioe) {
                metrics.end();
            }
        };
    }

    private void handleError(Request request,
                             int responseCode,
                             String errorMsg,
                             CompleteHandler complete) {
        synchronized (this) {
            if (hasHandleComplete) {
                return;
            }
            hasHandleComplete = true;
        }

        ResponseInfo info = ResponseInfo.create(request, responseCode, null, null, errorMsg);
        metrics.setResponse(info);
        metrics.setRequest(request);
        metrics.end();
        complete.complete(info, metrics, info.response);
        releaseResource();
    }

    private void handleResponse(Request request,
                                okhttp3.Response response,
                                CompleteHandler complete) {
        synchronized (this) {
            if (hasHandleComplete) {
                return;
            }
            hasHandleComplete = true;
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
        } catch (Exception e) {
            errorMessage = e.getMessage();
        }

        if (responseBody == null) {
            errorMessage = response.message();
        } else if (responseContentType(response) != "application/json") {
            String responseString = new String(responseBody);
            if (responseString.length() > 0) {
                try {
                    responseJson = new JSONObject(responseString);
                } catch (Exception ignored) {
                }
            }
        } else {
            try {
                responseJson = buildJsonResp(responseBody);
            } catch (Exception e) {
                statusCode = ResponseInfo.ParseError;
                errorMessage = e.getMessage();
            }
        }


        final ResponseInfo info = ResponseInfo.create(request, statusCode, responseHeader, responseJson, errorMessage);
        metrics.setResponse(info);
        metrics.setRequest(request);
        if (response.protocol() == Protocol.HTTP_1_0) {
            metrics.setHttpVersion("1.0");
        } else if (response.protocol() == Protocol.HTTP_1_1) {
            metrics.setHttpVersion("1.1");
        } else if (response.protocol() == Protocol.HTTP_2) {
            metrics.setHttpVersion("2");
        }
        metrics.end();
        complete.complete(info, metrics, info.response);

        releaseResource();
    }

    private void releaseResource() {
        this.currentRequest = null;
        this.requestProgress = null;
        this.completeHandler = null;
        this.metrics = null;
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

    private int getStatusCodeByException(Exception e) {
        int statusCode = NetworkError;
        String msg = e.getMessage();
        if (msg != null && msg.contains("Canceled")) {
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
        } else if (e instanceof ProtocolException) {
            statusCode = ResponseInfo.NetworkProtocolError;
        } else if (e instanceof SSLException) {
            statusCode = ResponseInfo.NetworkSSLError;
        }
        return statusCode;
    }

    private static JSONObject buildJsonResp(byte[] body) throws Exception {
        String str = new String(body, Constants.UTF_8);
        // 允许 空 字符串
        if (StringUtils.isNullOrEmpty(str)) {
            return new JSONObject();
        }
        return new JSONObject(str);
    }


    private static String getOkHttpVersion() {

        // 4.9.+
        try {
            Class clazz = Class.forName("okhttp3.OkHttp");
            Field versionField = clazz.getField("VERSION");
            Object version = versionField.get(clazz);
            return (version + "");
        } catch (Exception ignore) {
        }

        try {
            Class clazz = Class.forName("okhttp3.internal.Version");
            Field versionField = clazz.getField("userAgent");
            Object version = versionField.get(clazz);
            return (version + "").replace("okhttp/", "");
        } catch (Exception ignore) {
        }

        try {
            Class clazz = Class.forName("okhttp3.internal.Version");
            Method get = clazz.getMethod("userAgent");
            Object version = get.invoke(clazz);
            return (version + "").replace("okhttp/", "");
        } catch (Exception ignore) {
        }

        return "";
    }

    private static class ResponseTag {
        public String ip = "";
        public long duration = -1;
    }
}
