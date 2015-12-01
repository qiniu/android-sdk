package com.qiniu.android.http;

import com.qiniu.android.common.Constants;
import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.StringMap;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import okio.BufferedSink;

/**
 * Created by bailong on 15/11/12.
 */
public final class Client {
    public static final String ContentTypeHeader = "Content-Type";
    public static final String DefaultMime = "application/octet-stream";
    public static final String JsonMime = "application/json";
    public static final String FormMime = "application/x-www-form-urlencoded";

    private final OkHttpClient httpClient;
    private final UrlConverter converter;

    public Client(Proxy proxy, int connectTimeout, int responseTimeout, UrlConverter converter, DnsManager dns {
        this.converter = converter;
        httpClient = new OkHttpClient();
        httpClient.ne
        if (proxy != null) {
            httpClient.setProxy(proxy.toSystemProxy());
        }
        httpClient.networkInterceptors().add(new Interceptor() {
            @Override
            public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
                Request request = chain.request();

                com.squareup.okhttp.Response response = chain.proceed(request);
                IpTag tag = (IpTag) request.tag();
                String ip = chain.connection().getSocket().getRemoteSocketAddress().toString();
                tag.ip = ip;
                return response;
            }
        });

        httpClient.setConnectTimeout(connectTimeout, TimeUnit.SECONDS);
        httpClient.setReadTimeout(responseTimeout, TimeUnit.SECONDS);
    }


    private static RequestBody create(final MediaType contentType,
                                      final byte[] content, final int offset, final int size) {
        if (content == null) throw new NullPointerException("content == null");

        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return contentType;
            }

            @Override
            public long contentLength() {
                return size;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.write(content, offset, size);
            }
        };
    }

    public void asyncSend(final Request.Builder requestBuilder, StringMap headers, final CompletionHandler completionHandler) {
        if (headers != null) {
            headers.forEach(new StringMap.Consumer() {
                @Override
                public void accept(String key, Object value) {
                    requestBuilder.header(key, value.toString());
                }
            });
        }

        final CompletionHandler complete = new CompletionHandler() {
            @Override
            public void complete(final ResponseInfo info, final JSONObject response) {
                AsyncRun.run(new Runnable() {
                    @Override
                    public void run() {
                        completionHandler.complete(info, response);
                    }
                });
            }
        };

        requestBuilder.header("User-Agent", UserAgent.instance().toString());
        final long start = System.currentTimeMillis();
        IpTag tag = new IpTag();
        httpClient.newCall(requestBuilder.tag(tag).build()).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                e.printStackTrace();
                long duration = (System.currentTimeMillis() - start) / 1000;
                int statusCode = ResponseInfo.NetworkError;
                String msg = e.getMessage();
                if (msg != null && msg.indexOf("UnknownHostException") == 0) {
                    statusCode = ResponseInfo.UnknownHost;
                } else if (msg != null && msg.indexOf("Broken pipe") == 0) {
                    statusCode = ResponseInfo.NetworkConnectionLost;
                } else if (e instanceof SocketTimeoutException) {
                    statusCode = ResponseInfo.TimedOut;
                }

                URL u = request.url();
                ResponseInfo info = new ResponseInfo(statusCode, "", "", "", u.getHost(), u.getPath(), "", u.getPort(), duration, 0, e.getMessage());

                complete.complete(info, null);
            }

            @Override
            public void onResponse(com.squareup.okhttp.Response response) throws IOException {
                long duration = (System.currentTimeMillis() - start) / 1000;
                onRet(response, "", duration, complete);
            }
        });
    }

    public void asyncPost(String url, byte[] body, int offset, int size,
                          StringMap headers, String contentType, CompletionHandler completionHandler) {
        if (converter != null) {
            url = converter.convert(url);
        }

        RequestBody rbody;
        if (body != null && body.length > 0) {
            MediaType t = MediaType.parse(contentType);
            rbody = create(t, body, offset, size);
        } else {
            rbody = RequestBody.create(null, new byte[0]);
        }

        Request.Builder requestBuilder = new Request.Builder().url(url).post(rbody);
        asyncSend(requestBuilder, headers, completionHandler);
    }

    public void asyncMultipartPost(String url,
                                   StringMap fields,
                                   String name,
                                   String fileName,
                                   byte[] fileBody,
                                   String mimeType,
                                   StringMap headers,
                                   CompletionHandler completionHandler) {
        RequestBody file = RequestBody.create(MediaType.parse(mimeType), fileBody);
        asyncMultipartPost(url, fields, name, fileName, file, headers, completionHandler);
    }

    public void asyncMultipartPost(String url,
                                   StringMap fields,
                                   String name,
                                   String fileName,
                                   File fileBody,
                                   String mimeType,
                                   StringMap headers,
                                   CompletionHandler completionHandler) {
        RequestBody file = RequestBody.create(MediaType.parse(mimeType), fileBody);
        asyncMultipartPost(url, fields, name, fileName, file, headers, completionHandler);
    }

    private void asyncMultipartPost(String url,
                                    StringMap fields,
                                    String name,
                                    String fileName,
                                    RequestBody file,
                                    StringMap headers,
                                    CompletionHandler completionHandler) {
        if (converter != null) {
            url = converter.convert(url);
        }
        final MultipartBuilder mb = new MultipartBuilder();
        mb.addFormDataPart(name, fileName, file);

        fields.forEach(new StringMap.Consumer() {
            @Override
            public void accept(String key, Object value) {
                mb.addFormDataPart(key, value.toString());
            }
        });
        mb.type(MediaType.parse("multipart/form-data"));
        RequestBody body = mb.build();
        Request.Builder requestBuilder = new Request.Builder().url(url).post(body);
        asyncSend(requestBuilder, headers, completionHandler);
    }

    private static class IpTag {
        public String ip = null;
    }


    private void onRet(com.squareup.okhttp.Response response, String ip, long duration,
                            CompletionHandler complete){
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
                String err = new String(body, Constants.UTF_8);
                error = json.optString("error", err);
            } catch (Exception e) {
                if (response.code() < 300) {
                    error = e.getMessage();
                }
            }
        }

        URL u = response.request().url();
        ResponseInfo info = new ResponseInfo(code, reqId, response.header("X-Log"), via(response),
                u.getHost(), u.getPath(), ip, u.getPort(), duration, 0, error);
        complete.complete(info,json);
    }

    private static String via(com.squareup.okhttp.Response response) {
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

    private static String ctype(com.squareup.okhttp.Response response) {
        MediaType mediaType = response.body().contentType();
        if (mediaType == null) {
            return "";
        }
        return mediaType.type() + "/" + mediaType.subtype();
    }

    private static JSONObject buildJsonResp(byte[] body) throws Exception {
        String str = new String(body, Constants.UTF_8);
        return new JSONObject(str);
    }
}
