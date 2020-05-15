package com.qiniu.android.http.newHttp;

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

    public String host(){
        return null;
    }

    public boolean isValid() {
        if (this.urlString == null || httpMethod == null) {
            return true;
        } else {
            return false;
        }
    }
}
