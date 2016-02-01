package com.qiniu.android.http;

import com.qiniu.android.common.Constants;
import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.Domain;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.StringMap;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Dns;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    public Client() {
        this(null, 10, 30, null, null);
    }

    public Client(Proxy proxy, int connectTimeout, int responseTimeout, UrlConverter converter, final DnsManager dns) {
        this.converter = converter;
        httpClient = new OkHttpClient();

        if (proxy != null) {
            httpClient.setProxy(proxy.toSystemProxy());
        }
        if (dns != null) {
            httpClient.setDns(new Dns() {
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
        httpClient.networkInterceptors().add(new Interceptor() {
            @Override
            public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
                Request request = chain.request();

                com.squareup.okhttp.Response response = chain.proceed(request);
                IpTag tag = (IpTag) request.tag();
                String ip = "";
                try {
                    ip = chain.connection().getSocket().getRemoteSocketAddress().toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                tag.ip = ip;
                return response;
            }
        });

        httpClient.setConnectTimeout(connectTimeout, TimeUnit.SECONDS);
        httpClient.setReadTimeout(responseTimeout, TimeUnit.SECONDS);
        httpClient.setWriteTimeout(0, TimeUnit.SECONDS);
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

                URL u = request.url();
                ResponseInfo info = new ResponseInfo(statusCode, "", "", "", u.getHost(), u.getPath(), "", u.getPort(), duration, 0, e.getMessage());

                complete.complete(info, null);
            }

            @Override
            public void onResponse(com.squareup.okhttp.Response response) throws IOException {
                long duration = (System.currentTimeMillis() - start) / 1000;
                IpTag tag = (IpTag) response.request().tag();
                onRet(response, tag.ip, duration, complete);
            }
        });
    }

    public void asyncPost(String url, byte[] body,
                          StringMap headers, ProgressHandler progressHandler,
                          CompletionHandler completionHandler, UpCancellationSignal c) {
        asyncPost(url, body, 0, body.length, headers, progressHandler, completionHandler, c);
    }

    public void asyncPost(String url, byte[] body, int offset, int size,
                          StringMap headers, ProgressHandler progressHandler,
                          CompletionHandler completionHandler, CancellationHandler c) {
        if (converter != null) {
            url = converter.convert(url);
        }

        RequestBody rbody;
        if (body != null && body.length > 0) {
            MediaType t = MediaType.parse(DefaultMime);
            rbody = RequestBody.create(t, body, offset, size);
        } else {
            rbody = RequestBody.create(null, new byte[0]);
        }
        if (progressHandler != null) {
            rbody = new CountingRequestBody(rbody, progressHandler, c);
        }

        Request.Builder requestBuilder = new Request.Builder().url(url).post(rbody);
        asyncSend(requestBuilder, headers, completionHandler);
    }

    public void asyncMultipartPost(String url,
                                   PostArgs args,
                                   ProgressHandler progressHandler,
                                   CompletionHandler completionHandler,
                                   CancellationHandler c) {
        RequestBody file;
        if (args.file != null) {
            file = RequestBody.create(MediaType.parse(args.mimeType), args.file);
        } else {
            file = RequestBody.create(MediaType.parse(args.mimeType), args.data);
        }
        asyncMultipartPost(url, args.params, progressHandler, args.fileName, file, completionHandler, c);
    }

    private void asyncMultipartPost(String url,
                                    StringMap fields,
                                    ProgressHandler progressHandler,
                                    String fileName,
                                    RequestBody file,
                                    CompletionHandler completionHandler,
                                    CancellationHandler cancellationHandler) {
        if (converter != null) {
            url = converter.convert(url);
        }
        final MultipartBuilder mb = new MultipartBuilder();
        mb.addFormDataPart("file", fileName, file);

        fields.forEach(new StringMap.Consumer() {
            @Override
            public void accept(String key, Object value) {
                mb.addFormDataPart(key, value.toString());
            }
        });
        mb.type(MediaType.parse("multipart/form-data"));
        RequestBody body = mb.build();
        if (progressHandler != null) {
            body = new CountingRequestBody(body, progressHandler, cancellationHandler);
        }
        Request.Builder requestBuilder = new Request.Builder().url(url).post(body);
        asyncSend(requestBuilder, null, completionHandler);
    }

    private void onRet(com.squareup.okhttp.Response response, String ip, long duration,
                       final CompletionHandler complete) {
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
            error = new String(body);
        }

        URL u = response.request().url();
        final ResponseInfo info = new ResponseInfo(code, reqId, response.header("X-Log"), via(response),
                u.getHost(), u.getPath(), ip, u.getPort(), duration, 0, error);
        final JSONObject json2 = json;
        AsyncRun.run(new Runnable() {
            @Override
            public void run() {
                complete.complete(info, json2);
            }
        });

    }

    private static class IpTag {
        public String ip = null;
    }
}
