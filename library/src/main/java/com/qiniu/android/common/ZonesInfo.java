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

    public static ZonesInfo createZonesInfo(JSONObject jsonObject) {
        ArrayList<ZoneInfo> zonesInfo = new ArrayList<>();
        try {
            if (jsonObject != null) {
                JSONArray hosts = jsonObject.getJSONArray("hosts");
                for (int i = 0; i < hosts.length(); i++) {
                    ZoneInfo zoneInfo = ZoneInfo.buildFromJson(hosts.getJSONObject(i));
                    if (zoneInfo != null && zoneInfo.isValid()) {
                        zonesInfo.add(zoneInfo);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return new ZonesInfo(zonesInfo);
    }

    public boolean isValid() {
        return zonesInfo != null && zonesInfo.size() > 0;
    }
}
