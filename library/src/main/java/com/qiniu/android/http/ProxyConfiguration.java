package com.qiniu.android.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Route;

/**
 * http 代理
 */
public final class ProxyConfiguration {

    public final String hostAddress;
    public final int port;
    public final String user;
    public final String password;
    public final Proxy.Type type;

    /**
     * @param hostAddress 服务器域名或IP，比如proxy.com, 192.168.1.1
     * @param port        端口
     * @param user        用户名，无则填null
     * @param password    用户密码，无则填null
     */
    public ProxyConfiguration(String hostAddress, int port, String user, String password, java.net.Proxy.Type type) {
        this.hostAddress = hostAddress;
        this.port = port;
        this.user = user;
        this.password = password;
        this.type = type;
    }

    public ProxyConfiguration(String hostAddress, int port) {
        this(hostAddress, port, null, null, Proxy.Type.HTTP);
    }

    Proxy proxy() {
        return new Proxy(type, new InetSocketAddress(hostAddress, port));
    }

    Authenticator authenticator() {
        return new Authenticator() {
            @Override
            public okhttp3.Request authenticate(Route route, okhttp3.Response response) throws IOException {
                String credential = Credentials.basic(user, password);
                return response.request().newBuilder().
                        header("Proxy-Authorization", credential).
                        header("Proxy-Connection", "Keep-Alive").build();
            }
        };
    }
}
