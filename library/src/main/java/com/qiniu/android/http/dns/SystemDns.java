package com.qiniu.android.http.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by yangsen on 2020/5/28
 */
public class SystemDns implements Dns {

    public List<InetAddress> lookupInetAddress(String hostname) throws UnknownHostException {
        InetAddress[] addressArray = InetAddress.getAllByName(hostname);
        return Arrays.asList(addressArray);
    }

    public List<IDnsNetworkAddress> lookup(String hostname) throws UnknownHostException {
        ArrayList<IDnsNetworkAddress> addressList = new ArrayList<>();
        InetAddress[] addressArray = InetAddress.getAllByName(hostname);
        for (InetAddress inetAddress : addressArray) {
            SystemDnsNetworkAddress address = new SystemDnsNetworkAddress(inetAddress.getHostName(), inetAddress.getHostAddress(), 120L, null, (new Date()).getTime());
            addressList.add(address);
        }
        return addressList;
    }

    private static class SystemDnsNetworkAddress implements IDnsNetworkAddress {

        private final String hostValue;
        private final String ipValue;
        private final Long ttlValue;
        private final String sourceValue;
        private final Long timestampValue;

        private SystemDnsNetworkAddress(String hostValue,
                                        String ipValue,
                                        Long ttlValue,
                                        String sourceValue,
                                        Long timestampValue) {
            this.hostValue = hostValue;
            this.ipValue = ipValue;
            this.ttlValue = ttlValue;
            this.sourceValue = sourceValue;
            this.timestampValue = timestampValue;
        }

        @Override
        public String getHostValue() {
            return hostValue;
        }

        @Override
        public String getIpValue() {
            return ipValue;
        }

        @Override
        public Long getTtlValue() {
            return ttlValue;
        }

        @Override
        public String getSourceValue() {
            return sourceValue;
        }

        @Override
        public Long getTimestampValue() {
            return timestampValue;
        }
    }
}
