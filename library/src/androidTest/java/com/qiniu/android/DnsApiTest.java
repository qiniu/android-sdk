package com.qiniu.android;

import android.graphics.LinearGradient;
import android.test.AndroidTestCase;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.qiniu.android.collect.Config;
import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.DnsPrefetcher;
import com.qiniu.android.storage.Recorder;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.persistent.FileRecorder;
import com.qiniu.android.utils.AndroidNetwork;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by jemy on 2019/8/20.
 */

public class DnsApiTest extends InstrumentationTestCase {
    public void testDns() throws Throwable {
        List<InetAddress> inetAddresses = null;
        DnsPrefetcher dnsPrefetcher;
//        try {
//            inetAddresses = DnsPrefetcher.getDnsBySystem().lookup("upload.qiniup.com");
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        }
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
            info = dnsPrefetcher.init(TestConfig.uptoken_prefetch).getPreQueryZone();
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
            info = dnsPrefetcher.init(TestConfig.uptoken_prefetch).getLocalZone();
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


    public void testSerializeCache() {
        String recordKey = "/sdcard/dnschache";
        try {
            Recorder recorder = new FileRecorder(Config.dnscacheDir);

            String s = String.valueOf(System.currentTimeMillis());
            TestCompany company = new TestCompany("qiniu", 8);
            byte[] com = StringUtils.toByteArray(company);
            Log.e("qiniutest", s);
            recorder.set("time", s.getBytes());
            recorder.set("compant", com);

            try {
                Thread.sleep(900);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            byte[] time = recorder.get("time");
            byte[] getcom = recorder.get("compant");
            Log.e("qiniutest", new String(time));
            TestCompany company1 = (TestCompany) StringUtils.toObject(getcom);
            Log.e("qiniutest", "name: " + company1.getName() + " ,age: " + company1.getAge());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testDnsPreAndcache() {
        UploadManager up = new UploadManager();
        boolean needPrefetch = up.checkRePrefetchDns(Config.dnscacheDir);
        Log.e("qiniutest", "check:" + needPrefetch);
        if (needPrefetch) {
            up.startPrefetchDns(TestConfig.uptoken_prefetch);
        } else {
            testRecoverCache();
            return;
        }
        //预取或者recover success
        List<String> list = DnsPrefetcher.getDnsPrefetcher().getHosts();
        HashMap<String, List<InetAddress>> map = DnsPrefetcher.getDnsPrefetcher().getConcurrentHashMap();
        Log.e("qiniutest: ", "list size: " + list.size());
        for (String s : list) {
            Log.e("qiniutest: ", "uphost: " + s);
            List<InetAddress> list1 = map.get(s);
            for (InetAddress inetAddress :
                    list1) {
                Log.e("qiniutest: ", "ip: " + inetAddress.getHostAddress());
            }
        }

    }

    //test recover
    public void testRecoverCache() {

        Recorder recorder = null;
        try {
            recorder = new FileRecorder(Config.dnscacheDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        new UploadManager().recoverDnsCache(recorder);


        HashMap<String, List<InetAddress>> map1 = DnsPrefetcher.getDnsPrefetcher().getConcurrentHashMap();
        List<String> list = DnsPrefetcher.getDnsPrefetcher().getHosts();
        for (String s : list) {
            Log.e("qiniutest: ", "uphost for cache: " + s);
            List<InetAddress> list1 = map1.get(s);
            for (InetAddress inetAddress :
                    list1) {
                Log.e("qiniutest: ", "ip for cache: " + inetAddress.getHostAddress());
            }
        }
    }


}
