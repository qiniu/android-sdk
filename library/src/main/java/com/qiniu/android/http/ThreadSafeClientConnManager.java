package com.qiniu.android.http;

import com.qiniu.android.dns.DnsManager;

import cz.msebera.android.httpclient.conn.scheme.SchemeRegistry;
import cz.msebera.android.httpclient.params.HttpParams;


/**
 * Created by bailong on 15/7/4.
 */
public final class ThreadSafeClientConnManager extends cz.msebera.android.httpclient.impl.conn.tsccm.ThreadSafeClientConnManager {
    private final DnsManager dns;

    public ThreadSafeClientConnManager(HttpParams params, SchemeRegistry schreg, DnsManager dns) {
        super(params, schreg);
        this.dns = dns;
    }

    //在父类构造函数中调用
    @Override
    protected cz.msebera.android.httpclient.conn.ClientConnectionOperator createConnectionOperator(final SchemeRegistry schreg) {
        DnsManager d = dns == null ? AsyncHttpClientMod.local.get() : dns;
        return new ClientConnectionOperator(schreg, d);// @ThreadSafe
    }
}
