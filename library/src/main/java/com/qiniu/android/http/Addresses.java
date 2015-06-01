package com.qiniu.android.http;

import com.qiniu.android.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * Created by Simon on 2015/5/28.
 */
public class Addresses implements Iterable {
    private ArrayList<Address> ups;
    private Addresses(ArrayList<Address> ups) {
        this.ups = ups;
    }
    @Override
    public Iterator<Address> iterator() {
        return ups.iterator();
    }

    public int size() {
        return ups.size();
    }

    public static class Helper {
        private ArrayList<Address> ups = new ArrayList<Address>();

        public void add(Address address) {
            add(address, 1);
        }

        public void add(Address address, int count) {
            for(int i=0; i < count; i++) {
                ups.add(address);
            }
        }

        public Addresses build() {
            ArrayList<Address> newUps = new ArrayList<Address>();
            Collections.copy(newUps, ups);
            return new Addresses(newUps);
        }

        public static String genAddress(String scheme, String host, int port) {
            if(host.indexOf(":", 5) >= 0) {
                return scheme + "://" + host;
            }else {
                return scheme + "://" + host + ":" + port;
            }
        }

        public static String genAddress(String host, int port) {
            return genAddress("http", host, port);
        }

        public static String genAddress(Address address) {
            return genAddress(address.scheme, address.host, address.port);
        }
    }

    public static class Address {
        public final String host;
        public final int port;
        public final boolean allowConvert;
        public final String scheme;

        public Address(String host) {
            this(host, 80);
        }

        public Address(String host, int port) {
            this(host, port, true);
        }

        public Address(String host, int port, boolean allowConvert) {
            this(host, port, allowConvert, null);
        }

        public Address(String host, int port, boolean allowConvert, String scheme) {
            if(port <= 0) {
                throw new IllegalArgumentException("port must bigger than zero.");
            }
            this.host = host;
            this.port = port;
            this.allowConvert = allowConvert;
            this.scheme = genScheme(scheme);
        }

        private String genScheme(String scheme) {
            if(StringUtils.isEmpty(scheme)) {
                scheme = "http";
            }
            return scheme;
        }
    }
}
