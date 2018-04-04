package com.qiniu.android;

import android.test.AndroidTestCase;

import com.qiniu.android.storage.Configuration;
import com.qiniu.android.utils.Dns;

import junit.framework.Assert;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by bailong on 15/1/5.
 */
public class DnsTest extends AndroidTestCase {
    public void testDns() {
        String ip = Dns.getAddressesString("qiniu.com");
        Assert.assertTrue(!ip.equals(""));
        ip = Dns.getAddressesString("nodns.qiniu.com");
        Assert.assertEquals("", ip);
    }


    public void testDnsConfig() throws UnknownHostException {
        Configuration config = new Configuration.Builder().build();
        Assert.assertTrue(" 默认使用 happlyDns ", config.dns != null);
        List<InetAddress> inetAddress = config.dns.lookup("www.baidu.com");
//        System.out.println(inetAddress.get(0).getHostAddress());
        Assert.assertFalse(" 解析不能为空 ", inetAddress.isEmpty());
        Assert.assertTrue(" 禁用 happlyDns ", new Configuration.Builder().dns(null).build().dns == null);
    }
}
