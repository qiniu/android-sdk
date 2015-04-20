package com.qiniu.android.http;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.qiniu.android.common.Config;

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

import static java.lang.String.format;

/**
 * 定义HTTP请求管理相关方法
 */
public final class HttpManager {
    private static final String userAgent = getUserAgent();
    private AsyncHttpClient client;
    private IReport reporter;
    private String backUpIp;

    public HttpManager(Proxy proxy) {
        this(proxy, null);
    }

    public HttpManager(Proxy proxy, IReport reporter) {
        this(proxy, reporter, null);
    }

    public HttpManager(Proxy proxy, IReport reporter, String backUpIp) {
        this.backUpIp = backUpIp;
        client = new AsyncHttpClient();
        client.setConnectTimeout(Config.CONNECT_TIMEOUT);
        client.setResponseTimeout(Config.RESPONSE_TIMEOUT);
        client.setUserAgent(userAgent);
        client.setEnableRedirects(false);
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
    }

    public HttpManager() {
        this(null);
    }

    private static String genId() {
        Random r = new Random();
        return System.currentTimeMillis() + "" + r.nextInt(999);
    }

    private static String getUserAgent() {
        return format("QiniuAndroid/%s (%s; %s; %s)", Config.VERSION,
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
                         ProgressHandler progressHandler, final CompletionHandler completionHandler, CancellationHandler c) {
        ByteArrayEntity entity = new ByteArrayEntity(data, offset, size, progressHandler, c);
        postEntity(url, entity, headers, progressHandler, completionHandler);
    }

    public void postData(String url, byte[] data, Header[] headers, ProgressHandler progressHandler,
                         CompletionHandler completionHandler, CancellationHandler c) {
        postData(url, data, 0, data.length, headers, progressHandler, completionHandler, c);
    }

    private void postEntity(final String url, final HttpEntity entity, Header[] headers,
                         ProgressHandler progressHandler, CompletionHandler completionHandler) {
        final CompletionHandler wrapper = wrap(completionHandler);
        final Header[] h = reporter.appendStatHeaders(headers);
        final AsyncHttpResponseHandler originHandler = new ResponseHandler(url, wrapper, progressHandler);
        if(backUpIp == null){
            client.post(null, url, h, entity, null, originHandler);
            return;
        }

        client.post(null, url, h, entity, null, new ResponseHandler(url, new CompletionHandler() {
            @Override
            public void complete(ResponseInfo info, JSONObject response) {
                if (info.statusCode != ResponseInfo.UnknownHost){
                    wrapper.complete(info, response);
                    return;
                }
                Header[] h2 = new Header[h.length + 1];
                System.arraycopy(h, 0, h2, 0, h.length);

                URI uri = URI.create(url);
                String newUrl = null;
                try {
                    newUrl = new URI(uri.getScheme(), null, backUpIp, uri.getPort(), uri.getPath(), uri.getQuery(), null).toString();
                } catch (URISyntaxException e) {
                    throw new AssertionError(e);
                }
                h2[h.length] = new BasicHeader("Host", uri.getHost());
                client.post(null, newUrl, h2, entity, null, originHandler);
            }
        }, progressHandler));
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
                              final CompletionHandler completionHandler, CancellationHandler c) {
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
        postEntity(url, entity, h, progressHandler, completionHandler);
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

    /**
     * fixed key escape for async http client
     * Appends a quoted-string to a StringBuilder.
     * <p/>
     * <p>RFC 2388 is rather vague about how one should escape special characters
     * in form-data parameters, and as it turns out Firefox and Chrome actually
     * do rather different things, and both say in their comments that they're
     * not really sure what the right approach is. We go with Chrome's behavior
     * (which also experimentally seems to match what IE does), but if you
     * actually want to have a good chance of things working, please avoid
     * double-quotes, newlines, percent signs, and the like in your field names.
     */
    private static String escapeMultipartString(String key) {
        StringBuilder target = new StringBuilder();

        for (int i = 0, len = key.length(); i < len; i++) {
            char ch = key.charAt(i);
            switch (ch) {
                case '\n':
                    target.append("%0A");
                    break;
                case '\r':
                    target.append("%0D");
                    break;
                case '"':
                    target.append("%22");
                    break;
                default:
                    target.append(ch);
                    break;
            }
        }
        return target.toString();
    }

}
