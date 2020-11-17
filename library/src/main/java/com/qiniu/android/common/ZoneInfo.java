package com.qiniu.android.common;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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

    public final int ttl;
    public final List<String> domains;
    public final List<String> old_domains;

    public final String regionId;
    public List<String> allHosts;
    public JSONObject detailInfo;

    public static ZoneInfo buildInfo(List<String> mainHosts,
                                     String regionId){
        return buildInfo(mainHosts, null, regionId);
    }

    public static ZoneInfo buildInfo(List<String> mainHosts,
                                     List<String> oldHosts,
                                     String regionId){
        if (mainHosts == null){
            return null;
        }

        HashMap<String, Object> up = new HashMap<>();
        up.put("domains", mainHosts);
        if (oldHosts != null){
            up.put("old", oldHosts);
        }

        if (regionId == null){
            regionId = EmptyRegionId;
        }
        HashMap<String, Object> info = new HashMap<>();
        info.put("ttl", 86400*1000);
        info.put("region", regionId);
        info.put("up", up);

        JSONObject object = new JSONObject(info);

        ZoneInfo zoneInfo = null;
        try {
            zoneInfo = ZoneInfo.buildFromJson(object);
        } catch (JSONException e) {}
        return zoneInfo;
    }

    private ZoneInfo(int ttl,
                     String regionId,
                     List<String> domains,
                     List<String> old_domains) {
        this.ttl = ttl;
        this.regionId = regionId;
        this.domains = domains;
        this.old_domains = old_domains;
    }

    /**
     *
     * @param obj Not allowed to be null
     * @return
     * @throws JSONException
     */
    public static ZoneInfo buildFromJson(JSONObject obj) throws JSONException {
        if (obj == null) {
            return null;
        }

        int ttl = obj.optInt("ttl");
        String regionId = obj.optString("region");
        if (regionId == null){
            regionId = EmptyRegionId;
        }

        JSONObject up = obj.optJSONObject("up");
        if (up == null){
            return null;
        }

        List<String> allHosts = new ArrayList<>();
        List<String> domains = new ArrayList<>();
        JSONArray domainsJson = up.optJSONArray("domains");
        if (domainsJson != null && domainsJson.length() > 0){
            for (int i=0; i< domainsJson.length(); i++) {
                String domain = domainsJson.optString(i);
                if (domain != null && domain.length() > 0){
                    domains.add(domain);
                    allHosts.add(domain);
                }
            }
        }

        List<String> old_domains = new ArrayList<>();
        JSONArray old_domainsJson = up.optJSONArray("old");
        if (old_domainsJson != null && old_domainsJson.length() > 0){
            for (int i=0; i< old_domainsJson.length(); i++) {
                String domain = old_domainsJson.optString(i);
                if (domain != null && domain.length() > 0){
                    old_domains.add(domain);
                    allHosts.add(domain);
                }
            }
        }

        if (domains.size() == 0 && old_domains.size() == 0){
            return null;
        }

        ZoneInfo zoneInfo = new ZoneInfo(ttl, regionId, domains, old_domains);
        zoneInfo.detailInfo = obj;

        zoneInfo.allHosts = allHosts;

        return zoneInfo;
    }

    public String getRegionId(){
        return regionId;
    }

    @Override
    public String toString() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("ttl", this.ttl);
        m.put("allHost", this.allHosts);
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
