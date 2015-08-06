/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package com.qiniu.android.http;

import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.Domain;

import org.apache.http.HttpHost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.OperatedClientConnection;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.conn.DefaultClientConnection;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


/*get from 4.3, mixed 4.0*/
public final class ClientConnectionOperator implements org.apache.http.conn.ClientConnectionOperator {


    /**
     * The scheme registry for looking up socket factories.
     */
    protected final SchemeRegistry schemeRegistry; // @ThreadSafe

    /**
     * the custom-configured DNS lookup mechanism.
     */
    protected final DnsManager dnsResolver;

    /**
     * Creates a new client connection operator for the given scheme registry
     * and the given custom DNS lookup mechanism.
     *
     * @param schemes     the scheme registry
     * @param dnsResolver the custom DNS lookup mechanism
     */
    public ClientConnectionOperator(final SchemeRegistry schemes, final DnsManager dnsResolver) {
        if (schemes == null) {
            throw new IllegalArgumentException
                    ("Scheme registry must not be null.");
        }
        this.schemeRegistry = schemes;
        this.dnsResolver = dnsResolver;
    }

    public static boolean validIP(String ip) {
        if (ip == null || ip.length() < 7 || ip.length() > 15) return false;
        if (ip.contains("-")) return false;

        try {
            int x = 0;
            int y = ip.indexOf('.');

            if (y != -1 && Integer.parseInt(ip.substring(x, y)) > 255) return false;

            x = ip.indexOf('.', ++y);
            if (x != -1 && Integer.parseInt(ip.substring(y, x)) > 255) return false;

            y = ip.indexOf('.', ++x);
            return !(y != -1 && Integer.parseInt(ip.substring(x, y)) > 255 &&
                    Integer.parseInt(ip.substring(++y, ip.length() - 1)) > 255 &&
                    ip.charAt(ip.length() - 1) != '.');

        } catch (NumberFormatException e) {
            return false;
        }
    }

    public OperatedClientConnection createConnection() {
        return new DefaultClientConnection();
    }

    public void openConnection(
            final OperatedClientConnection conn,
            final HttpHost target,
            final InetAddress local,
            final HttpContext context,
            final HttpParams params) throws IOException {
        if (conn == null) {
            throw new IllegalArgumentException
                    ("Connection must not be null.");
        }
        if (target == null) {
            throw new IllegalArgumentException
                    ("Target host must not be null.");
        }
        // local address may be null
        //@@@ is context allowed to be null?
        if (params == null) {
            throw new IllegalArgumentException
                    ("Parameters must not be null.");
        }
        if (conn.isOpen()) {
            throw new IllegalArgumentException
                    ("Connection must not be open.");
        }

        final Scheme schm = schemeRegistry.getScheme(target.getSchemeName());
        final SocketFactory sf = schm.getSocketFactory();
        String host = target.getHostName();
        String[] ips;
        if (validIP(host)) {
            ips = new String[]{host};
        } else {
            ips = systemResolv(host);
            if (ips == null || ips.length == 0) {
                throw new UnknownHostException("no ip for " + host);
            }
        }

        final int port = schm.resolvePort(target.getPort());
        for (int i = 0; i < ips.length; i++) {
            final String ip = ips[i];
            final boolean last = i == ips.length - 1;

            Socket sock = sf.createSocket();
            conn.opening(sock, target);

            try {
                Socket connsock = sf.connectSocket(sock, ip, port, local, 0, params);
                if (sock != connsock) {
                    sock = connsock;
                    conn.opening(sock, target);
                }
                prepareSocket(sock, context, params);
                conn.openCompleted(sf.isSecure(sock), params);
                AsyncHttpClientMod.ip.set(ip);
                return;
            } catch (ConnectException ex) {
                if (last) {
                    throw new HttpHostConnectException(target, ex);
                }
            }
        }
    }

    public void updateSecureConnection(OperatedClientConnection conn,
                                       HttpHost target,
                                       HttpContext context,
                                       HttpParams params) throws IOException {
        if (conn == null) {
            throw new IllegalArgumentException
                    ("Connection must not be null.");
        }
        if (target == null) {
            throw new IllegalArgumentException
                    ("Target host must not be null.");
        }
        if (params == null) {
            throw new IllegalArgumentException
                    ("Parameters must not be null.");
        }
        if (!conn.isOpen()) {
            throw new IllegalArgumentException
                    ("Connection must be open.");
        }

        final Scheme schm = schemeRegistry.getScheme(target.getSchemeName());
        if (!(schm.getSocketFactory() instanceof LayeredSocketFactory)) {
            throw new IllegalArgumentException
                    ("Target scheme (" + schm.getName() + ") must have layered socket factory.");
        }

        final LayeredSocketFactory lsf = (LayeredSocketFactory) schm.getSocketFactory();
        final Socket sock;
        try {
            sock = lsf.createSocket
                    (conn.getSocket(), target.getHostName(), target.getPort(), true);
        } catch (ConnectException ex) {
            throw new HttpHostConnectException(target, ex);
        }
        prepareSocket(sock, context, params);
        conn.update(sock, target, lsf.isSecure(sock), params);
    }

    /**
     * Performs standard initializations on a newly created socket.
     *
     * @param sock    the socket to prepare
     * @param context the context for the connection
     * @param params  the parameters from which to prepare the socket
     * @throws IOException in case of an IO problem
     */
    protected void prepareSocket(
            final Socket sock,
            final HttpContext context,
            final HttpParams params) throws IOException {
        sock.setTcpNoDelay(HttpConnectionParams.getTcpNoDelay(params));
        sock.setSoTimeout(HttpConnectionParams.getSoTimeout(params));

        final int linger = HttpConnectionParams.getLinger(params);
        if (linger >= 0) {
            sock.setSoLinger(linger > 0, linger);
        }
    }

    private String[] systemResolv(String domain) throws UnknownHostException {
        if (dnsResolver == null) {
            InetAddress[] addresses = InetAddress.getAllByName(domain);
            String[] x = new String[addresses.length];
            for (int i = 0; i < addresses.length; i++) {
                x[i] = addresses[i].getHostAddress();
            }
            return x;
        }
        try {
            return dnsResolver.query(new Domain(domain, true, false, 3600));
        } catch (IOException e) {
            throw new UnknownHostException(e.getMessage());
        }
    }
}
