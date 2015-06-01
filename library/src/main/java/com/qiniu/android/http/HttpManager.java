package com.qiniu.android.http;

import com.loopj.android.http.AsyncHttpClient;
import com.qiniu.android.common.Constants;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
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
    private UrlConverter converter;

    public HttpManager(Proxy proxy) {
        this(proxy, null);
    }

    public HttpManager(Proxy proxy, IReport reporter) {
        this(proxy, reporter, 10, 30, null);
    }

    public HttpManager(Proxy proxy, IReport reporter, int connectTimeout, int responseTimeout,
                       UrlConverter converter) {
        client = new AsyncHttpClient();
        client.setConnectTimeout(connectTimeout * 1000);
        client.setResponseTimeout(responseTimeout * 1000);
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
                         ProgressHandler progressHandler, final CompletionHandler completionHandler,
                         CancellationHandler c, Addresses addressList) {
        ByteArrayEntity entity = new ByteArrayEntity(data, offset, size, progressHandler, c);
        postEntity(url, entity, headers, progressHandler, completionHandler, addressList);
    }

    public void postData(String url, byte[] data, Header[] headers, ProgressHandler progressHandler,
                         CompletionHandler completionHandler, CancellationHandler c, Addresses addressList) {
        postData(url, data, 0, data.length, headers, progressHandler, completionHandler, c, addressList);
    }

    public void postData(String url, byte[] data, Header[] headers, ProgressHandler progressHandler,
                         CompletionHandler completionHandler, CancellationHandler c) {
        postData(url, data, 0, data.length, headers, progressHandler, completionHandler, c, null);
    }

    private void postEntity(final String url, final HttpEntity entity, final Header[] headers,
                            final ProgressHandler progressHandler, final CompletionHandler completionHandler,
                            final Addresses addressList) {
        // AsyncHttpClient 默认会重试，不变 url 情况下不重试
        if(addressList == null){
            postEntity0(wrapUrl(url), entity, headers, progressHandler, completionHandler);
            return;
        }
        final Iterator<Addresses.Address> iter = addressList.iterator();
        if(!iter.hasNext()){
            postEntity0(wrapUrl(url), entity, headers, progressHandler, completionHandler);
            return;
        }

        final URI uri = URI.create(url);
        final Header[] newHeaders = new Header[headers.length + 1];

        //若第一个地址 HOST 不对，对后面有影响吗？
        newHeaders[headers.length] = new BasicHeader("Host", uri.getHost());

        String newUrl = genUrl(uri, iter.next());
        final String[] storeUrl = {newUrl};
        final int[] storeCount = {0};
        // 重试次数会不会太大，怎样和回退上传host、port组合 相匹配？
        final int retryMax = addressList.size();

        // 更换上传服务器后，调整各 Address 的权重：即重新排序?????
        final CompletionHandler newCompletionHandler = new CompletionHandler() {

            @Override
            public void complete(ResponseInfo info, JSONObject response) {
                storeCount[0] += 1;

                if(info.needRetry() && storeCount[0] < retryMax) {
                    if(info.needSwitchServer() && iter.hasNext()) {
                        String newUrl = genUrl(uri, iter.next());
                        storeUrl[0] = newUrl;
                    }
                    postEntity0(storeUrl[0], entity, newHeaders, progressHandler, this);
                    return;
                }
                completionHandler.complete(info, response);
            }

        };

        postEntity0(newUrl, entity, newHeaders, progressHandler, newCompletionHandler);
    }


    private String wrapUrl(String url){
        if (converter != null){
            url = converter.convert(url);
        }
        return url;
    }

    private String wrapUrl(String url, Addresses.Address address){
        if (converter != null && address.allowConvert){
            url = converter.convert(url);
        }
        return url;
    }

    private String genUrl(URI uri, Addresses.Address address) {
        try {
            String url = new URI(address.scheme, null, address.host, address.port, uri.getPath(), uri.getQuery(), null).toString();
            return wrapUrl(url, address);
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    private void postEntity0(String url, final HttpEntity entity, Header[] headers,
                         ProgressHandler progressHandler, CompletionHandler completionHandler) {
        final CompletionHandler wrapper = wrap(completionHandler);
        final Header[] h = reporter.appendStatHeaders(headers);

        client.post(null, url, h, entity, null, new ResponseHandler(url, wrapper, progressHandler));
    }

    public void multipartPost(String url, PostArgs args, ProgressHandler progressHandler,
                              final CompletionHandler completionHandler, CancellationHandler c) {
        multipartPost(url, args, progressHandler, completionHandler, c, null);
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
                              final CompletionHandler completionHandler, CancellationHandler c,
                              Addresses addressList) {
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
        postEntity(url, entity, h, progressHandler, completionHandler, addressList);
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
