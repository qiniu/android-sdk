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
    static final ThreadLocal<DnsManager> local = new ThreadLocal<>();
    static final ThreadLocal<String> ip = new ThreadLocal<>();
    private final DnsManager dns;

    private AsyncHttpClientMod(DnsManager dns) {
        this.dns = dns;
    }

    public static AsyncHttpClientMod create(DnsManager dns) {
        local.set(dns);
        AsyncHttpClientMod a = new AsyncHttpClientMod(dns);
        local.remove();
        return a;
    }

    //在父类构造函数中调用
    @Override
    protected ClientConnectionManager createConnectionManager(SchemeRegistry schemeRegistry, BasicHttpParams httpParams) {
        DnsManager d = dns == null ? local.get() : dns;
        return new ThreadSafeClientConnManager(httpParams, schemeRegistry, d);
    }
}
