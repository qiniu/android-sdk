package com.qiniu.android.common;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ZonesInfo {

    public final ArrayList<ZoneInfo> zonesInfo;


    public ZonesInfo(ArrayList<ZoneInfo> zonesInfo) {
        this.zonesInfo = zonesInfo;
    }

    public static ZonesInfo createZonesInfo(JSONObject jsonObject){
        ArrayList<ZoneInfo> zonesInfo = new ArrayList<>();
        try {
            JSONArray hosts = jsonObject.getJSONArray("hosts");
            for (int i = 0; i < hosts.length(); i++) {
                ZoneInfo zoneInfo = ZoneInfo.buildFromJson(hosts.getJSONObject(i));
                if (zoneInfo != null){
                    zonesInfo.add(zoneInfo);
                }
            }
        } catch (JSONException ignored) {}

        return new ZonesInfo(zonesInfo);
    }


}
