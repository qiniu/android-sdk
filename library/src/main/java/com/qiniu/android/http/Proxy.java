package com.qiniu.android.http;

/**
 * http 代理
 */
public final class Proxy {

    public final String hostAddress;
    public final int port;
    public final String user;
    public final String password;

    /**
     * @param hostAddress 服务器域名或IP，比如proxy.com, 192.168.1.1
     * @param port        端口
     * @param user        用户名，无则填null
     * @param password    用户密码，无则填null
     */
    public Proxy(String hostAddress, int port, String user, String password) {
        this.hostAddress = hostAddress;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public java.net.Proxy toSystemProxy() {
        return null;
    }
}
