package com.qiniu.android.http.request;

import java.util.HashMap;
import java.util.Map;

public class Request {

    public static final String HttpMethodHEAD = "HEAD";
    public static final String HttpMethodGet = "GET";
    public static final String HttpMethodPOST = "POST";
    public static final String HttpMethodPUT = "PUT";

    public final String urlString;
    public final String httpMethod;
    public final Map<String, String> allHeaders;
    public final int timeout;
    public final int connectTimeout;
    public final int readTimeout;
    public final int writeTimeout;
    public byte[] httpBody;

    private String host;

    public Request(String urlString,
                   String httpMethod,
                   Map<String, String> allHeaders,
                   byte[] httpBody,
                   int timeout) {
        this(urlString, httpMethod, allHeaders, httpBody, 10,
                (timeout - 10) >> 1,
                (timeout - 10) >> 1);
    }

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

    public String getHost() {
        return host;
    }

    protected boolean isValid() {
        return this.urlString == null || httpMethod == null;
    }
}
