package com.qiniu.android.http.request;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class Request {

    public static final String HttpMethodGet = "GET";
    public static final String HttpMethodPOST = "POST";

    public final String urlString;
    public final String httpMethod;
    public final Map<String, String> allHeaders;
    public final byte[] httpBody;
    public final int timeout;

    public String host;
    public String ip;

    protected IUploadServer uploadServer;

    public Request(String urlString,
                   String httpMethod,
                   Map<String, String> allHeaders,
                   byte[] httpBody,
                   int timeout) {

        this.urlString = urlString;
        this.httpMethod = (httpMethod != null) ? httpMethod : HttpMethodGet;
        this.allHeaders = (allHeaders != null) ? allHeaders : new HashMap<String, String>();
        this.httpBody = (httpBody != null) ? httpBody :  new byte[0];
        this.timeout = timeout;
    }

    public InetAddress getInetAddress(){
        if (uploadServer == null || uploadServer.getIp() == null) {
            return null;
        }

        try {
            return InetAddress.getByName(uploadServer.getIp());
        } catch (UnknownHostException e) {
            return null;
        }
    }

    protected boolean isValid() {
        return this.urlString == null || httpMethod == null;
    }
}
