package com.qiniu.android.http.dns;

/**
 * Dns 解析源
 */
public class DnsSource {

    /**
     * Doh 解析源
     */
    public static final String Doh = "doh";

    /**
     * udp 方式的解析源
     */
    public static final String Udp = "dns";

    /**
     * DnsPod 解析源
     */
    public static final String Dnspod = "dnspod";

    /**
     * System 解析源
     */
    public static final String System = "system";

    /**
     * 自定义解析源
     */
    public static final String Custom = "customized";

    /**
     * 未知解析源
     */
    public static final String None = "none";

    /**
     * 判断解析源是否为 Doh
     *
     * @param source 解析源
     * @return 解析源是否为 Doh
     */
    public static boolean isDoh(String source) {
        return source != null && source.contains(Doh);
    }

    /**
     * 判断解析源是否为 Udp
     *
     * @param source 解析源
     * @return 解析源是否为 Udp
     */
    public static boolean isUdp(String source) {
        return source != null && source.contains(Udp);
    }

    /**
     * 判断解析源是否为 DnsPod
     *
     * @param source 解析源
     * @return 解析源是否为 DnsPod
     */
    public static boolean isDnspod(String source) {
        return source != null && source.contains(Dnspod);
    }

    /**
     * 判断解析源是否为系统的
     *
     * @param source 解析源
     * @return 解析源是否为系统的
     */
    public static boolean isSystem(String source) {
        return source != null && source.contains(System);
    }

    /**
     * 判断解析源是否为自定义的
     *
     * @param source 解析源
     * @return 解析源是否为自定义的
     */
    public static boolean isCustom(String source) {
        return source != null && source.contains(Custom);
    }
}
