package com.qiniu.android.http.dns;

public class DnsSource {
    public static final String Doh = "doh";
    public static final String Udp = "dns";
    public static final String Dnspod = "dnspod";
    public static final String System = "system";
    public static final String Custom = "customized";
    public static final String None = "none";

    public static boolean isDoh(String source) {
        return source != null && source.contains(Doh);
    }

    public static boolean isUdp(String source) {
        return source != null && source.contains(Udp);
    }

    public static boolean isDnspod(String source) {
        return source != null && source.contains(Dnspod);
    }

    public static boolean isSystem(String source) {
        return source != null && source.contains(System);
    }

    public static boolean isCustom(String source) {
        return source != null && source.contains(Custom);
    }
}
