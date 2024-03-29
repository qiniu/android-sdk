package com.qiniu.android.bigdata.client;

import com.qiniu.android.common.Constants;
import com.qiniu.android.http.CancellationHandler;
import com.qiniu.android.http.dns.DnsPrefetcher;
import com.qiniu.android.http.dns.IDnsNetworkAddress;
import com.qiniu.android.http.request.httpclient.CountingRequestBody;
import com.qiniu.android.http.request.httpclient.MultipartBody;
import com.qiniu.android.http.ProgressHandler;
import com.qiniu.android.http.ProxyConfiguration;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.UrlConverter;
import com.qiniu.android.http.UserAgent;
import com.qiniu.android.http.dns.Dns;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.StringMap;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import static com.qiniu.android.http.ResponseInfo.NetworkError;

/**
 * Created by bailong on 15/11/12.
 *
 * @hidden
 */
public final class Client {
    /**
     * HTTP 请求头：Content-Type
     */
    public static final String ContentTypeHeader = "Content-Type";

    /**
     * HTTP 请求默认的 MimeType
     */
    public static final String DefaultMime = "application/octet-stream";

    /**
     * HTTP 请求 Json 的 MimeType
     */
    public static final String JsonMime = "application/json";

    /**
     * HTTP 请求 FormMime 的 MimeType
     */
    public static final String FormMime = "application/x-www-form-urlencoded";

    private final UrlConverter converter;
    private OkHttpClient httpClient;

    /**
     * 构造方法
     */
    public Client() {
        this(null, 10, 30, null, null);
    }

    /**
     * 构造函数
     *
     * @param proxy           请求代理
     * @param connectTimeout  请求建立连接超时时间
     * @param responseTimeout 请求接收数据超时时间
     * @param converter       请求 Url 拦截器
     * @param dns             请求的 Dns 解析器
     */
    public Client(ProxyConfiguration proxy, int connectTimeout, int responseTimeout, UrlConverter converter, final Dns dns) {
        this.converter = converter;
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (proxy != null) {
            builder.proxy(proxy.proxy());
            if (proxy.user != null && proxy.password != null) {
                builder.proxyAuthenticator(proxy.authenticator());
            }
        }

        builder.dns(new okhttp3.Dns() {
            @Override
            public List<InetAddress> lookup(String hostname) throws UnknownHostException {

                List<IDnsNetworkAddress> networkAddressList = DnsPrefetcher.getInstance().getInetAddressByHost(hostname);
                if (networkAddressList != null && networkAddressList.size() > 0) {
                    List<InetAddress> inetAddressList = new ArrayList<>();
                    for (IDnsNetworkAddress networkAddress : networkAddressList) {
                        InetAddress address = null;
                        if (networkAddress.getIpValue() != null && (address = InetAddress.getByName(networkAddress.getIpValue())) != null) {
                            inetAddressList.add(address);
                        }
                    }

                    if (inetAddressList.size() > 0) {
                        return inetAddressList;
                    }
                }
                return okhttp3.Dns.SYSTEM.lookup(hostname);
            }
        });

        builder.networkInterceptors().add(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
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

        builder.connectTimeout(connectTimeout, TimeUnit.SECONDS);
        builder.readTimeout(responseTimeout, TimeUnit.SECONDS);
        builder.writeTimeout(0, TimeUnit.SECONDS);
        httpClient = builder.build();

    }

    private static String via(okhttp3.Response response) {
        String via;
        if (!(via = response.header("X-Via", "")).equals("")) {
            return via;
        }

        if (!(via = response.header("X-Px", "")).equals("")) {
            return via;
        }

        if (!(via = response.header("Fw-Via", "")).equals("")) {
            return via;
        }
        return via;
    }

    private static String ctype(okhttp3.Response response) {
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

    private static ResponseInfo buildResponseInfo(okhttp3.Response response, String ip, long duration,
                                                  final UpToken upToken, final long totalSize) {
        int code = response.code();
        String reqId = response.header("X-Reqid");
        reqId = (reqId == null) ? null : reqId.trim().split(",")[0];
        byte[] body = null;
        String error = null;
        try {
            body = response.body().bytes();
        } catch (IOException e) {
            error = e.getMessage();
        }
        JSONObject json = null;
        if (ctype(response).equals(Client.JsonMime) && body != null) {
            try {
                json = buildJsonResp(body);
                if (response.code() != 200) {
                    String err = new String(body, Constants.UTF_8);
                    error = json.optString("error", err);
                }
            } catch (Exception e) {
                if (response.code() < 300) {
                    error = e.getMessage();
                }
            }
        } else {
            error = body == null ? "null body" : new String(body);
        }

        HashMap<String, String> responseHeader = new HashMap<String, String>();
        int headerCount = response.headers().size();
        for (int i = 0; i < headerCount; i++) {
            String name = response.headers().name(i).toLowerCase();
            String value = response.headers().value(i);
            responseHeader.put(name, value);
        }
        return ResponseInfo.create(null, code, responseHeader, json, error);
    }

    private static long getContentLength(okhttp3.Response response) {
        try {
            RequestBody body = response.request().body();
            if (body == null) {
                return 0;
            }
            return body.contentLength();
        } catch (Throwable t) {
            return -1;
        }
    }

    private static void onRet(okhttp3.Response response, String ip, long duration, final UpToken upToken,
                              final long totalSize, final CompletionHandler complete) {
        final ResponseInfo info = buildResponseInfo(response, ip, duration, upToken, totalSize);

        AsyncRun.runInMain(new Runnable() {
            @Override
            public void run() {
                complete.complete(info, info.response);
            }
        });
    }

    /**
     * 异步请求
     *
     * @param requestBuilder 请求构造器
     * @param headers        请求头
     * @param upToken        上传 Token
     * @param totalSize      请求体大小
     * @param complete       结束回调
     */
    public void asyncSend(final Request.Builder requestBuilder, StringMap headers, final UpToken upToken,
                          final long totalSize, final CompletionHandler complete) {
        if (headers != null) {
            headers.forEach(new StringMap.Consumer() {
                @Override
                public void accept(String key, Object value) {
                    requestBuilder.header(key, value.toString());
                }
            });
        }

        if (upToken != null) {
            requestBuilder.header("User-Agent", UserAgent.instance().getUa(upToken.accessKey));
        } else {
            requestBuilder.header("User-Agent", UserAgent.instance().getUa("pandora"));
        }


        final ResponseTag tag = new ResponseTag();
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

                ResponseInfo responseInfo = ResponseInfo.create(null, statusCode, null, null, e.getMessage());
                complete.complete(responseInfo, null);
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                ResponseTag tag = (ResponseTag) response.request().tag();
                onRet(response, tag.ip, tag.duration, upToken, totalSize, complete);
            }
        });
    }

    /**
     * 异步 POST 请求
     *
     * @param url               请求 url
     * @param body              请求 body
     * @param headers           请求 header
     * @param upToken           上传 token
     * @param totalSize         请求总大小
     * @param progressHandler   请求进度回调
     * @param completionHandler 结束回调
     * @param c                 取消回调
     */
    public void asyncPost(String url, byte[] body,
                          StringMap headers, final UpToken upToken,
                          final long totalSize, ProgressHandler progressHandler,
                          CompletionHandler completionHandler, UpCancellationSignal c) {
        asyncPost(url, body, 0, body.length, headers, upToken, totalSize, progressHandler, completionHandler, c);
    }

    /**
     * 异步 POST 请求
     *
     * @param url               请求 Url
     * @param body              请求体
     * @param offset            请求体偏移量
     * @param size              请求体大小
     * @param headers           请求 Header
     * @param upToken           上传 Token
     * @param totalSize         请求体总大小
     * @param progressHandler   进度回调
     * @param completionHandler 完成回调
     * @param c                 取消回调
     */
    public void asyncPost(String url, byte[] body, int offset, int size,
                          StringMap headers, final UpToken upToken,
                          final long totalSize, ProgressHandler progressHandler,
                          CompletionHandler completionHandler, CancellationHandler c) {
        if (converter != null) {
            url = converter.convert(url);
        }

        RequestBody rbody;
        if (body != null && body.length > 0) {
            MediaType t = MediaType.parse(DefaultMime);
            if (headers != null) {
                Object ct = headers.get(ContentTypeHeader);
                if (ct != null) {
                    t = MediaType.parse(ct.toString());
                }
            }
            rbody = RequestBody.create(t, body, offset, size);
        } else {
            rbody = RequestBody.create(null, new byte[0]);
        }
        if (progressHandler != null || c != null) {
            rbody = new CountingRequestBody(rbody, progressHandler, totalSize, c);
        }

        Request.Builder requestBuilder = new Request.Builder().url(url).post(rbody);
        asyncSend(requestBuilder, headers, upToken, totalSize, completionHandler);
    }

    /**
     * 异步表单请求
     *
     * @param url               请求 Url
     * @param args              请求参数
     * @param upToken           上传的 Token
     * @param progressHandler   进度回调
     * @param completionHandler 完成回答
     * @param c                 取消回调
     */
    public void asyncMultipartPost(String url,
                                   PostArgs args,
                                   final UpToken upToken,
                                   ProgressHandler progressHandler,
                                   CompletionHandler completionHandler,
                                   CancellationHandler c) {
        RequestBody file;
        long totalSize;
        if (args.file != null) {
            file = RequestBody.create(MediaType.parse(args.mimeType), args.file);
            totalSize = args.file.length();
        } else {
            file = RequestBody.create(MediaType.parse(args.mimeType), args.data);
            totalSize = args.data.length;
        }
        asyncMultipartPost(url, args.params, upToken, totalSize, progressHandler, args.fileName, file, completionHandler, c);
    }

    private void asyncMultipartPost(String url,
                                    StringMap fields,
                                    final UpToken upToken,
                                    final long totalSize,
                                    ProgressHandler progressHandler,
                                    String fileName,
                                    RequestBody file,
                                    CompletionHandler completionHandler,
                                    CancellationHandler cancellationHandler) {
        if (converter != null) {
            url = converter.convert(url);
        }
        final MultipartBody.Builder mb = new MultipartBody.Builder();
        mb.addFormDataPart("file", fileName, file);

        fields.forEach(new StringMap.Consumer() {
            @Override
            public void accept(String key, Object value) {
                mb.addFormDataPart(key, value.toString());
            }
        });
        mb.setType(MediaType.parse("multipart/form-data"));
        RequestBody body = mb.build();
        if (progressHandler != null || cancellationHandler != null) {
            body = new CountingRequestBody(body, progressHandler, totalSize, cancellationHandler);
        }
        Request.Builder requestBuilder = new Request.Builder().url(url).post(body);
        asyncSend(requestBuilder, null, upToken, totalSize, completionHandler);
    }

    /**
     * 异步 GET 请求
     *
     * @param url               请求 Url
     * @param headers           请求 Header
     * @param upToken           上传的 Token
     * @param completionHandler 请求完成回调
     */
    public void asyncGet(String url, StringMap headers, final UpToken upToken,
                         CompletionHandler completionHandler) {
        Request.Builder requestBuilder = new Request.Builder().get().url(url);
        asyncSend(requestBuilder, headers, upToken, 0, completionHandler);
    }

    /**
     * 同步 GET 请求
     *
     * @param url     请求 Url
     * @param headers 请求 Header
     * @return ResponseInfo
     */
    public ResponseInfo syncGet(String url, StringMap headers) {
        Request.Builder requestBuilder = new Request.Builder().get().url(url);
        return send(requestBuilder, headers);
    }

    private ResponseInfo send(final Request.Builder requestBuilder, StringMap headers) {
        if (headers != null) {
            headers.forEach(new StringMap.Consumer() {
                @Override
                public void accept(String key, Object value) {
                    requestBuilder.header(key, value.toString());
                }
            });
        }

        requestBuilder.header("User-Agent", UserAgent.instance().getUa(""));
        long start = System.currentTimeMillis();
        okhttp3.Response res = null;
        ResponseTag tag = new ResponseTag();
        Request req = requestBuilder.tag(tag).build();
        try {
            res = httpClient.newCall(req).execute();
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseInfo.create(null, NetworkError, null, null, e.getMessage());
        }

        return buildResponseInfo(res, tag.ip, tag.duration, null, 0);
    }

    /**
     * 同步表单请求
     *
     * @param url     请求 Url
     * @param args    请求参数
     * @param upToken 上传 Token
     * @return ResponseInfo
     */
    public ResponseInfo syncMultipartPost(String url, PostArgs args, final UpToken upToken) {
        RequestBody file;
        long totalSize;
        if (args.file != null) {
            file = RequestBody.create(MediaType.parse(args.mimeType), args.file);
            totalSize = args.file.length();
        } else {
            file = RequestBody.create(MediaType.parse(args.mimeType), args.data);
            totalSize = args.data.length;
        }
        return syncMultipartPost(url, args.params, upToken, totalSize, args.fileName, file);
    }

    private ResponseInfo syncMultipartPost(String url,
                                           StringMap fields,
                                           final UpToken upToken,
                                           final long totalSize,
                                           String fileName,
                                           RequestBody file) {
        final MultipartBody.Builder mb = new MultipartBody.Builder();
        mb.addFormDataPart("file", fileName, file);

        fields.forEach(new StringMap.Consumer() {
            @Override
            public void accept(String key, Object value) {
                mb.addFormDataPart(key, value.toString());
            }
        });
        mb.setType(MediaType.parse("multipart/form-data"));
        RequestBody body = mb.build();
        Request.Builder requestBuilder = new Request.Builder().url(url).post(body);
        return syncSend(requestBuilder, null, upToken, totalSize);
    }

    /**
     * 同步请求
     *
     * @param requestBuilder 请求构造器
     * @param headers        请求 Header
     * @param upToken        上传的 Token
     * @param totalSize      请求体大小
     * @return ResponseInfo
     */
    public ResponseInfo syncSend(final Request.Builder requestBuilder, StringMap headers,
                                 final UpToken upToken, final long totalSize) {
        if (headers != null) {
            headers.forEach(new StringMap.Consumer() {
                @Override
                public void accept(String key, Object value) {
                    requestBuilder.header(key, value.toString());
                }
            });
        }

        requestBuilder.header("User-Agent", UserAgent.instance().getUa(upToken.accessKey));
        final ResponseTag tag = new ResponseTag();
        Request req = null;
        try {
            req = requestBuilder.tag(tag).build();
            okhttp3.Response response = httpClient.newCall(req).execute();
            return buildResponseInfo(response, tag.ip, tag.duration, upToken, totalSize);
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

            return ResponseInfo.create(null, statusCode, null, null, e.getMessage());
        }
    }

    private static class ResponseTag {
        public String ip = "";
        public long duration = -1;
    }
}
