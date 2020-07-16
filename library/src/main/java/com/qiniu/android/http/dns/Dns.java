package com.qiniu.android.http.dns;

import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by sxci on 03/04/2018.
 */
public interface Dns {
    List<IDnsNetworkAddress> lookup(String hostname) throws UnknownHostException;
}
