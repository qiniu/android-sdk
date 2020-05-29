package com.qiniu.android.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jemy on 17/04/2017.
 */

public class ZoneInfo {

    // 只允许内部使用
    public final static String SDKDefaultIOHost = "sdkDefaultIOHost";
    public final static String EmptyRegionId = "sdkEmptyRegionId";

    private static int DOMAIN_FROZEN_SECONDS = 10 * 60;

    private int ttl;
    public UploadServerGroup acc;
    public UploadServerGroup src;
    public UploadServerGroup old_acc;
    public UploadServerGroup old_src;

    public String zoneRegionId;
    public ArrayList<String> allHosts;
    public JSONObject detailInfo;

    //upHost
    public List<String> upDomainsList;
    //upHost -> frozenTillTimestamp
    public Map<String, Long> upDomainsMap;

    private String regionId;

    public static ZoneInfo buildInfo(@NotNull ArrayList<String> mainHosts,
                                     @Nullable ArrayList<String> ioHosts){
        if (mainHosts == null){
            return null;
        }

        HashMap<String, Object> up_acc = new HashMap<>();
        HashMap<String, Object> up = new HashMap<>();
        up_acc.put("main", mainHosts);
        up.put("acc", up_acc);

        HashMap<String, Object> io_src = new HashMap<>();
        HashMap<String, Object> io = new HashMap<>();
        io_src.put("main", (ioHosts != null ? ioHosts : new ArrayList<String>()));
        io.put("src", io_src);

        HashMap<String, Object> info = new HashMap<>();
        info.put("ttl", 86400*1000);
        info.put("up", up);
        info.put("io", io);

        JSONObject object = new JSONObject(info);

        ZoneInfo zoneInfo = null;
        try {
            zoneInfo = ZoneInfo.buildFromJson(object);
        } catch (JSONException e) {}
        return zoneInfo;
    }

    private ZoneInfo(int ttl,
                     @Nullable UploadServerGroup acc,
                     @Nullable UploadServerGroup src,
                     @Nullable UploadServerGroup old_acc,
                     @Nullable UploadServerGroup old_src) {

        this.ttl = ttl;
        this.acc = acc;
        this.src = src;
        this.old_acc = old_acc;
        this.old_src = old_src;
    }

    /**
     *
     * @param obj Not allowed to be null
     * @return
     * @throws JSONException
     */
    public static ZoneInfo buildFromJson(@NotNull JSONObject obj) throws JSONException {
        if (obj == null) {
            return null;
        }

        int ttl = obj.getInt("ttl");
        List<String> domainsList = new ArrayList<>();
        ConcurrentHashMap<String, Long> domainsMap = new ConcurrentHashMap<>();

        String io_host = "null";
        try {
            JSONObject io = obj.getJSONObject("io");
            JSONObject io_src = io.getJSONObject("src");
            JSONArray io_main = io_src.getJSONArray("main");
            io_host = io_main.length() > 0 ? io_main.getString(0) : "null";
        } catch (JSONException e){}

        String zoneRegion = "unknown";
        if (io_host.equals("iovip.qbox.me")){
            zoneRegion = "z0";
        } else if (io_host.equals("iovip-z1.qbox.me")){
            zoneRegion = "z1";
        } else if (io_host.equals("iovip-z2.qbox.me")){
            zoneRegion = "z2";
        } else if (io_host.equals("iovip-na0.qbox.me")){
            zoneRegion = "na0";
        } else if (io_host.equals("iovip-as0.qbox.me")){
            zoneRegion = "as0";
        } else if (io_host.equals(SDKDefaultIOHost)){
            zoneRegion = EmptyRegionId;
        }

        JSONObject up = obj.getJSONObject("up");

        JSONObject acc = null;
        JSONObject src = null;
        JSONObject old_acc = null;
        JSONObject old_src = null;
        try {
            acc = up.getJSONObject("acc");;
        } catch (JSONException e) {}
        try {
            src = up.getJSONObject("src");;
        } catch (JSONException e) {}
        try {
            old_acc = up.getJSONObject("old_acc");;
        } catch (JSONException e) {}
        try {
            old_src = up.getJSONObject("old_src");;
        } catch (JSONException e) {}

        ZoneInfo zoneInfo = new ZoneInfo(ttl,
                UploadServerGroup.buildInfoFromJson(acc),
                UploadServerGroup.buildInfoFromJson(src),
                UploadServerGroup.buildInfoFromJson(old_acc),
                UploadServerGroup.buildInfoFromJson(old_src));
        zoneInfo.zoneRegionId = zoneRegion;
        zoneInfo.detailInfo = obj;

        ArrayList<String> allHosts = new ArrayList<>();
        if (zoneInfo.acc != null && zoneInfo.acc.allHosts != null){
            allHosts.addAll(zoneInfo.acc.allHosts);
        }
        if (zoneInfo.src != null && zoneInfo.src.allHosts != null){
            allHosts.addAll(zoneInfo.src.allHosts);
        }
        if (zoneInfo.old_acc != null && zoneInfo.old_acc.allHosts != null){
            allHosts.addAll(zoneInfo.old_acc.allHosts);
        }
        if (zoneInfo.old_src != null && zoneInfo.old_src.allHosts != null){
            allHosts.addAll(zoneInfo.old_src.allHosts);
        }
        zoneInfo.allHosts = allHosts;

        return zoneInfo;
    }

    public void frozenDomain(String domain) {
        //frozen for 10 minutes
        upDomainsMap.put(domain, System.currentTimeMillis() / 1000 + DOMAIN_FROZEN_SECONDS);
    }

    public String getRegionId(){
        return zoneRegionId;
    }

    @Override
    public String toString() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("ttl", this.ttl);
        m.put("upDomainList", this.upDomainsList);
        m.put("upDomainMap", this.upDomainsMap);
        return new JSONObject(m).toString();
    }


    public static class UploadServerGroup {
        public final String info;
        public final ArrayList<String> main;
        public final ArrayList<String> backup;
        public final ArrayList<String> allHosts;

        public static UploadServerGroup buildInfoFromJson(JSONObject jsonObject){
            if (jsonObject == null){
                return null;
            }

            String info = null;
            ArrayList<String> main = new ArrayList<String>();
            ArrayList<String> backup = new ArrayList<String>();
            ArrayList<String> allHosts = new ArrayList<String>();

            try {
                info = jsonObject.getString("info");
            } catch (JSONException e) {}

            try {
                JSONArray mainArray = jsonObject.getJSONArray("main");
                for (int i = 0; i < mainArray.length(); i++) {
                    String item = mainArray.getString(i);
                    main.add(item);
                    allHosts.add(item);
                }
            } catch (JSONException e) {}

            try {
                JSONArray mainArray = jsonObject.getJSONArray("backup");
                for (int i = 0; i < mainArray.length(); i++) {
                    String item = mainArray.getString(i);
                    main.add(item);
                    allHosts.add(item);
                }
            } catch (JSONException e){}

            return new UploadServerGroup(info, main, backup, allHosts);
        }

        public UploadServerGroup(String info,
                                 ArrayList<String> main,
                                 ArrayList<String> backup,
                                 ArrayList<String> allHosts) {
            this.info = info;
            this.main = main;
            this.backup = backup;
            this.allHosts = allHosts;
        }
    }
}
