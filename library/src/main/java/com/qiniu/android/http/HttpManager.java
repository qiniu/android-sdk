package com.qiniu.android.http;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.qiniu.android.common.Constants;
import com.qiniu.android.utils.Dns;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;

import static java.lang.String.format;

/**
 * 定义HTTP请求管理相关方法
 */
public final class HttpManager {
    private static final String userAgent = getUserAgent();
    private AsyncHttpClient client;
    private IReport reporter;
    private String backUpIp;
    private UrlConverter converter;

    public HttpManager(Proxy proxy) {
        this(proxy, null);
    }

    public HttpManager(Proxy proxy, IReport reporter) {
        this(proxy, reporter, null, 10, 30, null);
    }

    public HttpManager(Proxy proxy, IReport reporter, String backUpIp,
                       int connectTimeout, int responseTimeout, UrlConverter converter) {
        this.backUpIp = backUpIp;
        client = new AsyncHttpClient();
        client.setConnectTimeout(connectTimeout*1000);
        client.setResponseTimeout(responseTimeout * 1000);
        client.setUserAgent(userAgent);
        client.setEnableRedirects(true);
        client.setRedirectHandler(new UpRedirectHandler());
        AsyncHttpClient.blockRetryExceptionClass(CancellationHandler.CancellationException.class);
        if (proxy != null) {
            client.setProxy(proxy.hostAddress, proxy.port, proxy.user, proxy.password);
        }
        this.reporter = reporter;
        if (reporter == null) {
            this.reporter = new IReport() {
                @Override
                public Header[] appendStatHeaders(Header[] headers) {
                    return headers;
                }

                @Override
                public void updateErrorInfo(ResponseInfo info) {

                }

                @Override
                public void updateSpeedInfo(ResponseInfo info) {

                }
            };
        }

        this.converter = converter;
    }

    public HttpManager() {
        this(null);
    }

    private static String genId() {
        Random r = new Random();
        return System.currentTimeMillis() + "" + r.nextInt(999);
    }

    private static String getUserAgent() {
        return format("QiniuAndroid/%s (%s; %s; %s)", Constants.VERSION,
                android.os.Build.VERSION.RELEASE, android.os.Build.MODEL, genId());
    }

    /**
     * 以POST方法发送请求数据
     *
     * @param url               请求的URL
     * @param data              发送的数据
     * @param offset            发送的数据起始字节索引
     * @param size              发送的数据字节长度
     * @param headers           发送的数据请求头部
     * @param progressHandler   发送数据进度处理对象
     * @param completionHandler 发送数据完成后续动作处理对象
     */
    public void postData(String url, byte[] data, int offset, int size, Header[] headers,
                         ProgressHandler progressHandler, final CompletionHandler completionHandler, CancellationHandler c, boolean forceIp) {
        ByteArrayEntity entity = new ByteArrayEntity(data, offset, size, progressHandler, c);
        postEntity(url, entity, headers, progressHandler, completionHandler, forceIp);
    }

    public void postData(String url, byte[] data, Header[] headers, ProgressHandler progressHandler,
                         CompletionHandler completionHandler, CancellationHandler c, boolean forceIp) {
        postData(url, data, 0, data.length, headers, progressHandler, completionHandler, c, forceIp);
    }

    private void postEntity(String url, final HttpEntity entity, Header[] headers,
                         final ProgressHandler progressHandler, final CompletionHandler completionHandler, final boolean forceIp) {
        final CompletionHandler wrapper = wrap(completionHandler);
        final Header[] h = reporter.appendStatHeaders(headers);

        if (converter != null){
            url = converter.convert(url);
        }

        ResponseHandler handler = new ResponseHandler(url, wrapper, progressHandler);
        if(backUpIp == null || converter != null){
            client.post(null, url, h, entity, null, handler);
            return;
        }
        final String url2 = url;

        ExecutorService t = client.getThreadPool();
        t.execute(new Runnable() {
            @Override
            public void run() {
                final URI uri = URI.create(url2);
                String ip = null;
                if (forceIp) {
                    ip = backUpIp;
                }else {
                    ip = Dns.getAddress(uri.getHost());
                    if (ip == null || ip.equals("")){
                        ip = backUpIp;
                    }
                }

                final Header[] h2 = new Header[h.length + 1];
                System.arraycopy(h, 0, h2, 0, h.length);

                String newUrl = null;
                try {
                    newUrl = new URI(uri.getScheme(), null, ip, uri.getPort(), uri.getPath(), uri.getQuery(), null).toString();
                } catch (URISyntaxException e) {
                    throw new AssertionError(e);
                }
                h2[h.length] = new BasicHeader("Host", uri.getHost());
                final String ip2 = ip;
                ResponseHandler handler2 = new ResponseHandler(url2, wrap(new CompletionHandler() {
                    @Override
                    public void complete(ResponseInfo info, JSONObject response) {
                        if (uri.getPort() == 80 || info.statusCode != ResponseInfo.CannotConnectToHost){
                            completionHandler.complete(info, response);
                            return;
                        }
                        String newUrl80 = null;
                        try {
                            newUrl80 = new URI(uri.getScheme(), null, ip2, 80, uri.getPath(), uri.getQuery(), null).toString();
                        } catch (URISyntaxException e) {
                            throw new AssertionError(e);
                        }
                        ResponseHandler handler3 = new ResponseHandler(newUrl80, completionHandler, progressHandler);
                        client.post(null, newUrl80, h2, entity, null, handler3);
                    }
                }), progressHandler);
                client.post(null, newUrl, h2, entity, null, handler2);
            }
        });
    }

    /**
     * 以POST方式发送multipart/form-data格式数据
     *
     * @param url               请求的URL
     * @param args              发送的数据
     * @param progressHandler   发送数据进度处理对象
     * @param completionHandler 发送数据完成后续动作处理对象
     */
    public void multipartPost(String url, PostArgs args, ProgressHandler progressHandler,
                              final CompletionHandler completionHandler, CancellationHandler c, boolean forceIp) {
        MultipartBuilder mbuilder = new MultipartBuilder();
        for (Map.Entry<String, String> entry : args.params.entrySet()) {
            mbuilder.addPart(entry.getKey(), entry.getValue());
        }
        if (args.data != null) {
            ByteArrayInputStream buff = new ByteArrayInputStream(args.data);
            try {
                mbuilder.addPart("file", args.fileName, buff, args.mimeType);
            } catch (IOException e) {
                completionHandler.complete(ResponseInfo.fileError(e), null);
                return;
            }
        } else {
            try {
                mbuilder.addPart("file", args.file, args.mimeType, "filename");
            } catch (IOException e) {
                completionHandler.complete(ResponseInfo.fileError(e), null);
                return;
            }
        }

        ByteArrayEntity entity = mbuilder.build(progressHandler, c);
        Header[] h = reporter.appendStatHeaders(new Header[0]);
        postEntity(url, entity, h, progressHandler, completionHandler, forceIp);
    }

    private CompletionHandler wrap(final CompletionHandler completionHandler) {
        return new CompletionHandler() {
            @Override
            public void complete(ResponseInfo info, JSONObject response) {
                completionHandler.complete(info, response);
                if (info.isOK()) {
                    reporter.updateSpeedInfo(info);
                } else {
                    reporter.updateErrorInfo(info);
                }
            }
        };
    }
}
