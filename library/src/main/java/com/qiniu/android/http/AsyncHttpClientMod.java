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
    static final DnsManager[] tempDns = new DnsManager[1];
    private final DnsManager dns;

    private AsyncHttpClientMod(DnsManager dns) {
        this.dns = dns;
    }

    public static AsyncHttpClientMod create(DnsManager dns) {
        synchronized (tempDns) {
            tempDns[0] = dns;
            AsyncHttpClientMod a = new AsyncHttpClientMod(dns);
            tempDns[0] = null;
            return a;
        }
    }

    //在父类构造函数中调用
    protected ClientConnectionManager createConnectionManager(SchemeRegistry schemeRegistry, BasicHttpParams httpParams) {
        DnsManager d = dns == null ? tempDns[0] : dns;
        return new ThreadSafeClientConnManager(httpParams, schemeRegistry, d);
    }
}
