package com.qiniu.android.http;

import com.qiniu.android.dns.DnsManager;

import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.params.HttpParams;

/**
 * Created by bailong on 15/7/4.
 */
public final class ThreadSafeClientConnManager extends org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager {
    private final DnsManager dns;

    public ThreadSafeClientConnManager(HttpParams params, SchemeRegistry schreg, DnsManager dns) {
        super(params, schreg);
        this.dns = dns;
    }

    //在父类构造函数中调用
    @Override
    protected org.apache.http.conn.ClientConnectionOperator createConnectionOperator(final SchemeRegistry schreg) {
        DnsManager d = dns == null ? AsyncHttpClientMod.local.get() : dns;
        return new ClientConnectionOperator(schreg, d);// @ThreadSafe
    }
}
