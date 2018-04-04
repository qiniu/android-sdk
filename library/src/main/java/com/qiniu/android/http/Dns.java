package com.qiniu.android.http;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by sxci on 03/04/2018.
 */

public interface Dns {
    List<InetAddress> lookup(String hostname) throws UnknownHostException;
}
