package com.qiniu.android.http;

import com.qiniu.android.common.Constants;
import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.Domain;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.StringMap;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import static com.qiniu.android.http.ResponseInfo.NetworkError;

/**
 * Created by bailong on 15/11/12.
 */
public final class Client {
    public static final String ContentTypeHeader = "Content-Type";
    public static final String DefaultMime = "application/octet-stream";
    public static final String JsonMime = "application/json";
    public static final String FormMime = "application/x-www-form-urlencoded";
    private OkHttpClient httpClient;

    public Client() {
        this(10, 30, null);
    }

    public Client(int connectTimeout, int responseTimeout, final DnsManager dns) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (dns != null) {
            builder.dns(new Dns() {
                @Override
                public List<InetAddress> lookup(String hostname) throws UnknownHostException {
                    InetAddress[] ips;
                    try {
                        ips = dns.queryInetAdress(new Domain(hostname));
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new UnknownHostException(e.getMessage());
                    }
                    if (ips == null) {
                        throw new UnknownHostException(hostname + " resolve failed");
                    }
                    List<InetAddress> l = new ArrayList<>();
                    Collections.addAll(l, ips);
                    return l;
                }
            });
        }
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

    private static ResponseInfo buildResponseInfo(okhttp3.Response response, String ip, long duration, final UpToken upToken) {
        int code = response.code();
        String reqId = response.header("X-Reqid");
        reqId = (reqId == null) ? null : reqId.trim();
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

        HttpUrl u = response.request().url();
        return ResponseInfo.create(json, code, reqId, response.header("X-Log"),
                via(response), u.host(), u.encodedPath(), ip, u.port(), duration, getContentLength(response), error, upToken);
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

    public ResponseInfo syncSend(final Request.Builder requestBuilder, StringMap headers, final UpToken upToken) {
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
            return buildResponseInfo(response, tag.ip, tag.duration, upToken);
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

            HttpUrl u = req.url();
            return ResponseInfo.create(null, statusCode, "", "", "", u.host(),
                    u.encodedPath(), "", u.port(), tag.duration, -1, e.getMessage(), upToken);
        }
    }

    public ResponseInfo syncMultipartPost(String url,
                                          PostArgs args,
                                          final UpToken upToken) {
        RequestBody file;
        if (args.file != null) {
            file = RequestBody.create(MediaType.parse(args.mimeType), args.file);
        } else {
            file = RequestBody.create(MediaType.parse(args.mimeType), args.data);
        }
        return syncMultipartPost(url, args.params, upToken, args.fileName, file);
    }

    private ResponseInfo syncMultipartPost(String url,
                                           StringMap fields,
                                           final UpToken upToken,
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
        return syncSend(requestBuilder, null, upToken);
    }

    public ResponseInfo send(final Request.Builder requestBuilder, StringMap headers, final UpToken upToken) {
        if (headers != null) {
            headers.forEach(new StringMap.Consumer() {
                @Override
                public void accept(String key, Object value) {
                    requestBuilder.header(key, value.toString());
                }
            });
        }

        requestBuilder.header("User-Agent", UserAgent.instance().getUa(upToken.accessKey));
        long start = System.currentTimeMillis();
        okhttp3.Response res = null;
        ResponseTag tag = new ResponseTag();
        Request req = requestBuilder.tag(tag).build();
        try {
            res = httpClient.newCall(req).execute();
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseInfo.create(null, NetworkError, "", "", "",
                    req.url().host(), req.url().encodedPath(), tag.ip, req.url().port(),
                    tag.duration, -1, e.getMessage(), upToken);
        }

        return buildResponseInfo(res, tag.ip, tag.duration, upToken);
    }

    private static class ResponseTag {
        public String ip = "";
        public long duration = -1;
    }
}
