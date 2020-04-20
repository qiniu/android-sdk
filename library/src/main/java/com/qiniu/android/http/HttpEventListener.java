package com.qiniu.android.http;

import com.qiniu.android.collect.LogHandler;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

public class HttpEventListener extends EventListener {

    /**
     * 每次请求的标识
     */
    private long callId = 1L;

    /**
     * 每次请求的开始时间，单位纳秒
     */
    private final long callStartNanos;

    private long total_elapsed_time;
    private long dns_elapsed_time;
    private long connect_elapsed_time;
    private long tls_connect_elapsed_time;
    private long request_elapsed_time;
    private long wait_elapsed_time;
    private long response_elapsed_time;
    private Client.ResponseTag responseTag;
    private LogHandler logHandler;
    private long start_dns_elapsed_time;
    private long start_total_elapsed_time;
    private long start_connect_elapsed_time;
    private long start_tls_connect_elapsed_time;
    private long start_request_elapsed_time;
    private long start_response_elapsed_time;

    public HttpEventListener(long callId, Client.ResponseTag responseTag, long callStartNanos) {
        this.callId = callId;
        this.callStartNanos = callStartNanos;
        this.responseTag = responseTag;
        this.logHandler = responseTag.logHandler;
    }

    public static final Factory FACTORY = new Factory() {
        final AtomicLong nextCallId = new AtomicLong(1L);

        @Override
        public EventListener create(@NotNull Call call) {
            long callId = nextCallId.getAndIncrement();
            return new HttpEventListener(callId, (Client.ResponseTag) call.request().tag(), System.nanoTime());

        }
    };

    @Override
    public void callStart(Call call) {
        super.callStart(call);
        start_total_elapsed_time = System.currentTimeMillis();
    }


    @Override
    public void dnsStart(Call call, String domainName) {
        super.dnsStart(call, domainName);
        start_dns_elapsed_time = System.currentTimeMillis();
    }

    @Override
    public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList) {
        super.dnsEnd(call, domainName, inetAddressList);
        dns_elapsed_time = System.currentTimeMillis() - start_dns_elapsed_time;//dns解析耗时
        logHandler.send("dns_elapsed_time", dns_elapsed_time);
    }

    @Override
    public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
        super.connectStart(call, inetSocketAddress, proxy);
        start_connect_elapsed_time = System.currentTimeMillis();
    }

    @Override
    public void secureConnectStart(Call call) {
        super.secureConnectStart(call);
        start_tls_connect_elapsed_time = System.currentTimeMillis();
    }

    @Override
    public void secureConnectEnd(Call call, Handshake handshake) {
        super.secureConnectEnd(call, handshake);
        tls_connect_elapsed_time = System.currentTimeMillis() - start_tls_connect_elapsed_time;
        logHandler.send("tls_connect_elapsed_time", tls_connect_elapsed_time);
    }

    @Override
    public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol) {
        super.connectEnd(call, inetSocketAddress, proxy, protocol);
        connect_elapsed_time = System.currentTimeMillis() - start_connect_elapsed_time;
        logHandler.send("connect_elapsed_time", connect_elapsed_time);
    }

    @Override
    public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol, IOException ioe) {
        super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe);
    }

    @Override
    public void connectionAcquired(Call call, Connection connection) {
        super.connectionAcquired(call, connection);
    }

    @Override
    public void connectionReleased(Call call, Connection connection) {
        super.connectionReleased(call, connection);
    }

    @Override
    public void requestHeadersStart(Call call) {
        super.requestHeadersStart(call);
        start_request_elapsed_time = System.currentTimeMillis();
    }

    @Override
    public void requestHeadersEnd(Call call, Request request) {
        super.requestHeadersEnd(call, request);
    }

    @Override
    public void requestBodyStart(Call call) {
        super.requestBodyStart(call);
    }

    @Override
    public void requestBodyEnd(Call call, long byteCount) {
        super.requestBodyEnd(call, byteCount);
        request_elapsed_time = System.currentTimeMillis() - start_request_elapsed_time;
        logHandler.send("request_elapsed_time", request_elapsed_time);
    }

    @Override
    public void responseHeadersStart(Call call) {
        super.responseHeadersStart(call);
        start_response_elapsed_time = System.currentTimeMillis();
    }

    @Override
    public void responseHeadersEnd(Call call, Response response) {
        super.responseHeadersEnd(call, response);
    }

    @Override
    public void responseBodyStart(Call call) {
        super.responseBodyStart(call);
    }

    @Override
    public void responseBodyEnd(Call call, long byteCount) {
        super.responseBodyEnd(call, byteCount);
        response_elapsed_time = System.currentTimeMillis() - start_response_elapsed_time;
        wait_elapsed_time = System.currentTimeMillis() - start_request_elapsed_time;
        logHandler.send("response_elapsed_time", response_elapsed_time);
        logHandler.send("wait_elapsed_time", wait_elapsed_time);
    }

    @Override
    public void callEnd(Call call) {
        super.callEnd(call);
        total_elapsed_time = System.currentTimeMillis() - start_total_elapsed_time;
        logHandler.send("total_elapsed_time", total_elapsed_time);
    }

    @Override
    public void callFailed(Call call, IOException ioe) {
        super.callFailed(call, ioe);
    }

}