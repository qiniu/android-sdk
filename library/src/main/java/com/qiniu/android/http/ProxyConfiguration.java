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

    /**
     * hostAddress
     */
    public final String hostAddress;

    /**
     * port
     */
    public final int port;

    /**
     * user
     */
    public final String user;

    /**
     * password
     */
    public final String password;

    /**
     * type
     */
    public final Proxy.Type type;

    /**
     * 构造函数
     *
     * @param hostAddress 服务器域名或IP，比如proxy.com, 192.168.1.1
     * @param port        端口
     * @param user        用户名，无则填null
     * @param password    用户密码，无则填null
     * @param type        proxy type
     */
    public ProxyConfiguration(String hostAddress, int port, String user, String password, java.net.Proxy.Type type) {
        this.hostAddress = hostAddress;
        this.port = port;
        this.user = user;
        this.password = password;
        this.type = type;
    }

    /**
     * 构造函数
     *
     * @param hostAddress 服务器域名或IP，比如proxy.com, 192.168.1.1
     * @param port        端口
     */
    public ProxyConfiguration(String hostAddress, int port) {
        this(hostAddress, port, null, null, Proxy.Type.HTTP);
    }

    /**
     * 获取 proxy
     *
     * @return proxy
     */
    public Proxy proxy() {
        return new Proxy(type, new InetSocketAddress(hostAddress, port));
    }

    /**
     * 获取 authenticator
     *
     * @return Authenticator
     */
    public Authenticator authenticator() {
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
