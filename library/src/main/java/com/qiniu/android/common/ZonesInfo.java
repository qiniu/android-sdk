package com.qiniu.android.common;

import com.qiniu.android.utils.Cache;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ZonesInfo implements Cloneable, Cache.Object {

    // 临时 zone，不建议长期使用
    private boolean isTemporary;

    public final ArrayList<ZoneInfo> zonesInfo = new ArrayList<>();

    private JSONObject jsonInfo;

    public ZonesInfo(JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        try {
            JSONArray hosts = jsonObject.getJSONArray("hosts");
            if (hosts.length() == 0) {
                return;
            }

            for (int i = 0; i < hosts.length(); i++) {
                ZoneInfo zoneInfo = ZoneInfo.buildFromJson(hosts.getJSONObject(i));
                if (zoneInfo != null && zoneInfo.isValid()) {
                    zonesInfo.add(zoneInfo);
                }
            }
            this.jsonInfo = jsonObject;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ZonesInfo(List<ZoneInfo> zonesInfo) {
        this(zonesInfo, false);
    }

    public ZonesInfo(List<ZoneInfo> zonesInfo, boolean isTemporary) {
        this(createJsonWithZoneInfoList(zonesInfo));
        this.isTemporary = isTemporary;
    }

    public static ZonesInfo createZonesInfo(JSONObject jsonObject) {
        return new ZonesInfo(jsonObject);
    }

    public boolean isValid() {
        if (zonesInfo == null || zonesInfo.isEmpty()) {
            return false;
        }

        boolean valid = true;
        for (ZoneInfo info : zonesInfo) {
            if (!info.isValid()) {
                valid = false;
                break;
            }
        }
        return valid;
    }

    @Deprecated
    // 是否为临时 zone, 临时 zone，不建议长期使用
    public boolean isTemporary() {
        return isTemporary;
    }

    @Deprecated
    public void toTemporary() {
        isTemporary = true;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        ArrayList<ZoneInfo> infos = new ArrayList<>();
        if (zonesInfo != null && !zonesInfo.isEmpty()) {
            for (ZoneInfo zoneInfo : zonesInfo) {
                infos.add((ZoneInfo) zoneInfo.clone());
            }
        }
        ZonesInfo info = new ZonesInfo(infos);
        info.isTemporary = isTemporary;
        return info;
    }

    @Override
    public JSONObject toJson() {
        return jsonInfo;
    }

    private static JSONObject createJsonWithZoneInfoList(List<ZoneInfo> zonesInfo) {
        JSONArray zoneJsonArray = new JSONArray();
        for (ZoneInfo info : zonesInfo) {
            if (info != null && info.detailInfo != null) {
                zoneJsonArray.put(info.detailInfo);
            }
        }

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("hosts", zoneJsonArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
