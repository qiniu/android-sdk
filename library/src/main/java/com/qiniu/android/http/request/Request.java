package com.qiniu.android.http.request;

import java.util.HashMap;
import java.util.Map;

/**
 * request
 *
 * @hidden
 */
public class Request {

    /**
     * 请求方式：HEAD
     */
    public static final String HttpMethodHEAD = "HEAD";

    /**
     * 请求方式：GET
     */
    public static final String HttpMethodGet = "GET";

    /**
     * 请求方式：POST
     */
    public static final String HttpMethodPOST = "POST";

    /**
     * 请求方式：PUT
     */
    public static final String HttpMethodPUT = "PUT";

    /**
     * 请求 Url
     */
    public final String urlString;

    /**
     * 请求方式
     */
    public final String httpMethod;

    /**
     * 请求 header
     */
    public final Map<String, String> allHeaders;

    /**
     * 请求超时时间
     */
    public final int timeout;

    /**
     * 请求建立连接超时时间
     */
    public final int connectTimeout;

    /**
     * 请求读超时时间
     */
    public final int readTimeout;

    /**
     * 请求写超时时间
     */
    public final int writeTimeout;

    /**
     * 请求 body
     */
    public byte[] httpBody;

    private String host;

    /**
     * 构造函数
     *
     * @param urlString  请求 url
     * @param httpMethod 请求方式
     * @param allHeaders 请求头
     * @param httpBody   请求体
     * @param timeout    请求超时时间
     */
    public Request(String urlString,
                   String httpMethod,
                   Map<String, String> allHeaders,
                   byte[] httpBody,
                   int timeout) {
        this(urlString, httpMethod, allHeaders, httpBody, 10,
                (timeout - 10) >> 1,
                (timeout - 10) >> 1);
    }

    /**
     * 构造函数
     *
     * @param urlString      请求 url
     * @param httpMethod     请求方式
     * @param allHeaders     请求头
     * @param httpBody       请求体
     * @param connectTimeout 请求建立连接超时时间
     * @param readTimeout    请求读超时时间
     * @param writeTimeout   请求写超时时间
     */
    public Request(String urlString,
                   String httpMethod,
                   Map<String, String> allHeaders,
                   byte[] httpBody,
                   int connectTimeout,
                   int readTimeout,
                   int writeTimeout) {
        if (connectTimeout < 0) {
            connectTimeout = 10;
        }
        if (readTimeout < 0) {
            readTimeout = 10;
        }
        if (writeTimeout < 0) {
            writeTimeout = 30;
        }

        this.urlString = urlString;
        this.httpMethod = (httpMethod != null) ? httpMethod : HttpMethodGet;
        this.allHeaders = (allHeaders != null) ? allHeaders : new HashMap<String, String>();
        this.httpBody = (httpBody != null) ? httpBody : new byte[0];
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
        this.timeout = connectTimeout + writeTimeout + readTimeout;
    }

    void setHost(String host) {
        this.host = host;
    }

    /**
     * 获取 host
     *
     * @return host
     */
    public String getHost() {
        return host;
    }

    /**
     * 请求是否有效
     *
     * @return 是否有效
     */
    protected boolean isValid() {
        return this.urlString == null || httpMethod == null;
    }

    /**
     * copy 请求，但是不 copy body
     *
     * @return 新的请求对象
     */
    public Request copyWithoutBody() {
        Request request = new Request(urlString, httpMethod, allHeaders, null,
                connectTimeout, readTimeout, writeTimeout);
        request.host = host;
        return request;
    }
}
