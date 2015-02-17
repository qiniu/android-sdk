package com.qiniu.android.http;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.qiniu.android.common.Config;

import org.apache.http.Header;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.util.Random;

import static java.lang.String.format;

/**
 * 定义HTTP请求管理相关方法
 */
public final class HttpManager {
    private static final String userAgent = getUserAgent();
    private AsyncHttpClient client;
    private IReport reporter;

    public HttpManager(Proxy proxy) {
        this(proxy, null);
    }

    public HttpManager(Proxy proxy, IReport reporter) {
        client = new AsyncHttpClient();
        client.setConnectTimeout(Config.CONNECT_TIMEOUT);
        client.setResponseTimeout(Config.RESPONSE_TIMEOUT);
        client.setUserAgent(userAgent);
        client.setEnableRedirects(false);
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
                         ProgressHandler progressHandler, final CompletionHandler completionHandler) {

        CompletionHandler wrapper =  wrap(completionHandler);
        Header[] h = reporter.appendStatHeaders(headers);
        AsyncHttpResponseHandler handler = new ResponseHandler(url, wrapper, progressHandler);
        ByteArrayEntity entity = new ByteArrayEntity(data, offset, size, progressHandler);

        client.post(null, url, h, entity, RequestParams.APPLICATION_OCTET_STREAM, handler);
    }

    public void postData(String url, byte[] data, Header[] headers,
                         ProgressHandler progressHandler, CompletionHandler completionHandler) {
        postData(url, data, 0, data.length, headers, progressHandler, completionHandler);
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
                              final CompletionHandler completionHandler) {
        RequestParams requestParams = new RequestParams(args.params);
        if (args.data != null) {
            ByteArrayInputStream buff = new ByteArrayInputStream(args.data);
            requestParams.put("file", buff, args.fileName, args.mimeType);
        } else {
            try {
                requestParams.put("file", args.file, args.mimeType);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                completionHandler.complete(ResponseInfo.fileError(e), null);
                return;
            }
        }

        CompletionHandler wrapper = wrap(completionHandler);
        Header[] h = reporter.appendStatHeaders(new Header[0]);
        AsyncHttpResponseHandler handler = new ResponseHandler(url, wrapper, progressHandler);
        client.post(null, url, h, requestParams, null, handler);
    }

    private CompletionHandler wrap(final CompletionHandler completionHandler){
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
