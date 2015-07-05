package com.qiniu.android.http;

import com.loopj.android.http.AsyncHttpClient;
import com.qiniu.android.dns.DnsManager;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.params.BasicHttpParams;

/**
 * Created by bailong on 15/7/4.
 */
public final class AsyncHttpClientMod extends AsyncHttpClient {
    private final DnsManager dns;

    public AsyncHttpClientMod(DnsManager dns) {
        this.dns = dns;
    }

    protected ClientConnectionManager createConnectionManager(SchemeRegistry schemeRegistry, BasicHttpParams httpParams) {
        return new ThreadSafeClientConnManager(httpParams, schemeRegistry, dns);
    }
}
