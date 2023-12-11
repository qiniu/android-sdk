package com.qiniu.android.http.dns;

import java.net.UnknownHostException;
import java.util.List;

/**
 * Dns 解析器
 *
 * Created by sxci on 03/04/2018.
 */
public interface Dns {

    /**
     * Dns 解析 host 域名
     *
     * @param hostname host 域名
     * @return 解析结果
     * @throws UnknownHostException 异常信息
     */
    List<IDnsNetworkAddress> lookup(String hostname) throws UnknownHostException;
}
