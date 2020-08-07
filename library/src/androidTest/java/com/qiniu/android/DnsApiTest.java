package com.qiniu.android;


import android.test.AndroidTestCase;
import android.util.Log;

import com.qiniu.android.collect.Config;
import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.DnsPrefetcher;
import com.qiniu.android.http.custom.DnsCacheInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.Recorder;
import com.qiniu.android.storage.persistent.DnsCacheFile;
import com.qiniu.android.utils.AndroidNetwork;
import com.qiniu.android.utils.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jemy on 2019/8/20.
 */

public class DnsApiTest extends AndroidTestCase {
    private Configuration configuration = null;

    @Override
    protected void setUp() throws Exception {
        configuration = new Configuration.Builder().build();
        DnsPrefetcher dnsPrefetcher = DnsPrefetcher.getDnsPrefetcher();
        dnsPrefetcher.setToken(TestConfig.commonToken);
    }

    public void testConcurrentHashMap(){
        InetAddress address = null;
        try {
            address = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        if (address == null){
            return;
        }

        List<InetAddress> addressList = new ArrayList<>();
        addressList.add(address);

        ConcurrentHashMap<String, List<InetAddress>> hashMap = new ConcurrentHashMap<>();
        hashMap.put("localhost", addressList);

        DnsPrefetcher dnsPrefetcher = DnsPrefetcher.getDnsPrefetcher();
        dnsPrefetcher.setConcurrentHashMap(hashMap);

        assertTrue(hashMap == dnsPrefetcher.getConcurrentHashMap());

    }

    public void testDns() throws Throwable {
        List<InetAddress> inetAddresses = null;
        try {
            inetAddresses = okhttp3.Dns.SYSTEM.lookup("upload.qiniup.com");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        Log.e("qiniutest", "InetAddress: " + inetAddresses.size());
        //超耗时过程
//        for (int i = 0; i < inetAddresses.size(); i++) {
//            Log.e("qiniutest", "InetAddress.getCanonicalHostName: " + inetAddresses.get(i).getCanonicalHostName());
//
//        }
        for (int i = 0; i < inetAddresses.size(); i++) {
            Log.e("qiniutest", "InetAddress.getHostAddress: " + inetAddresses.get(i).getHostAddress());
        }
    }


    public void testQueryDomain() {
        ZoneInfo info = null;

        DnsPrefetcher dnsPrefetcher = DnsPrefetcher.getDnsPrefetcher();
        try {
            info = dnsPrefetcher.init(TestConfig.uptoken_prefetch, configuration).getPreQueryZone();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if (info == null) {
            Log.e("qiniutest: ", "null");
        }
        Log.e("qiniutest: ", info.toString());
        Log.e("qiniutest: ", info.upDomainsList.get(0));
    }


    public void testLocalDomain() {
        List<ZoneInfo> info = null;
        DnsPrefetcher dnsPrefetcher = DnsPrefetcher.getDnsPrefetcher();
        try {
            info = dnsPrefetcher.init(TestConfig.uptoken_prefetch, configuration).getLocalZone();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if (info == null) {
            Log.e("qiniutest: ", "null");
        }
        for (int i = 0; i < info.size(); i++) {
            Log.e("qiniutest: ", info.get(i).toString());
        }

    }


    public void testLocalIp() {
        String s = AndroidNetwork.getHostIP();
        Log.e("qiniutest", s);
    }

    //test recover
    public void testRecoverCache() {

        Recorder recorder = null;

        DnsPrefetcher.recoverCache(configuration);

        ConcurrentHashMap<String, List<InetAddress>> map1 = DnsPrefetcher.getDnsPrefetcher().getConcurrentHashMap();
        if (map1.size() <= 0)
            return;
        for (String s : map1.keySet()) {
            Log.e("qiniutest: ", "uphost for cache: " + s);
            List<InetAddress> list1 = map1.get(s);
            for (InetAddress inetAddress :
                    list1) {
                Log.e("qiniutest: ", "ip for cache: " + inetAddress.getHostAddress());
            }
        }
    }

    public void testSerializable() throws UnknownHostException {
        InetAddress address = InetAddress.getByName("127.0.0.1");
        List<InetAddress> addressList = new ArrayList<>();
        addressList.add(address);
        ConcurrentHashMap<String, List<InetAddress>> info = new ConcurrentHashMap<>();
        info.put("localhost", addressList);

        DnsCacheInfo cacheInfo = new DnsCacheInfo("12321", "192.168.1.1", "akScope", info);
        Log.e("qiniutest", cacheInfo.toString());

        byte[] data = StringUtils.toByteArray(cacheInfo);

        DnsCacheInfo cacheInfoSer = (DnsCacheInfo)StringUtils.toObject(data);

        assertTrue(cacheInfoSer != null);
        assertTrue(cacheInfoSer.localIp != null);
        assertTrue(cacheInfoSer.info != null);
        assertTrue(cacheInfoSer.info.get("localhost") != null);
        assertTrue(cacheInfoSer.info.get("localhost").get(0).getHostAddress().equals("127.0.0.1"));
    }
}
