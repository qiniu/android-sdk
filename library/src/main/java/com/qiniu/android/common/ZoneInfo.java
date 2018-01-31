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
    private static int DOMAIN_FROZEN_SECONDS = 10 * 60;
    //upHost
    public final List<String> upDomainsList;
    //upHost -> frozenTillTimestamp
    public final Map<String, Long> upDomainsMap;
    private final int ttl;

    public ZoneInfo(int ttl, List<String> upDomainsList, Map<String, Long> upDomainsMap) {
        this.ttl = ttl;
        this.upDomainsList = upDomainsList;
        this.upDomainsMap = upDomainsMap;
    }

    public static ZoneInfo buildFromJson(JSONObject obj) throws JSONException {
        int ttl = obj.getInt("ttl");
        List<String> domainsList = new ArrayList<>();
        ConcurrentHashMap<String, Long> domainsMap = new ConcurrentHashMap<>();
        JSONObject upObj = obj.getJSONObject("up");

        String[] upDomainTags = new String[]{"acc.main", "src.main", "acc.backup", "src.backup",
                "old_acc.main", "old_src.main", "old_acc.backup", "old_src.backup"};
        for (String tagC : upDomainTags) {
            String[] tmp = tagC.split("\\.");
            String tag = tmp[0];
            String scope = tmp[1];
            JSONObject tagRootObj = upObj.getJSONObject(tag);
            try {
                JSONArray tagScopeObj = tagRootObj.getJSONArray(scope);
                // if scope is main and tagScopeObj is null, that means something wrong.
                // the next step will throw exception.
                if (tagScopeObj == null && "backup".equals(scope)) {
                    continue;
                }
                for (int i = 0; i < tagScopeObj.length(); i++) {
                    String upDomain = tagScopeObj.getString(i);
                    domainsList.add(upDomain);
                    domainsMap.put(upDomain, 0L);
                }
            } catch (JSONException ex) {
                if ("main".equals(scope)) {
                    throw ex;
                }
                //some zone has not backup domain, just ignore here
            }
        }

        return new ZoneInfo(ttl, domainsList, domainsMap);
    }


    public synchronized void frozenDomain(String domain) {
        // synchronized is ok.
        //frozen for 10 minutes
        upDomainsMap.put(domain, System.currentTimeMillis() / 1000 + DOMAIN_FROZEN_SECONDS);
        upDomainsList.remove(domain);
        upDomainsList.add(domain);
    }

    @Override
    public String toString() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("ttl", this.ttl);
        m.put("upDomainList", this.upDomainsList);
        m.put("upDomainMap", this.upDomainsMap);
        return new JSONObject(m).toString();
    }
}
