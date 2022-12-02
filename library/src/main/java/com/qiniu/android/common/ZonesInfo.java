package com.qiniu.android.common;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class ZonesInfo implements Cloneable {

    // 临时 zone，不建议长期使用
    private boolean isTemporary;

    public final ArrayList<ZoneInfo> zonesInfo;

    public ZonesInfo(ArrayList<ZoneInfo> zonesInfo) {
        this(zonesInfo, false);
    }

    public ZonesInfo(ArrayList<ZoneInfo> zonesInfo, boolean isTemporary) {
        this.zonesInfo = zonesInfo;
        this.isTemporary = isTemporary;
    }

    public static ZonesInfo createZonesInfo(JSONObject jsonObject) {
        ArrayList<ZoneInfo> zonesInfo = new ArrayList<>();
        if (jsonObject != null) {
            try {
                JSONArray hosts = jsonObject.getJSONArray("hosts");
                for (int i = 0; i < hosts.length(); i++) {
                    ZoneInfo zoneInfo = ZoneInfo.buildFromJson(hosts.getJSONObject(i));
                    if (zoneInfo != null && zoneInfo.isValid()) {
                        zonesInfo.add(zoneInfo);
                    }
                }

            } catch (Exception ignored) {
            }
        }

        return new ZonesInfo(zonesInfo);
    }

    public boolean isValid() {
        if (zonesInfo == null || zonesInfo.size() == 0) {
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

    // 是否为临时 zone, 临时 zone，不建议长期使用
    public boolean isTemporary() {
        return isTemporary;
    }

    public void toTemporary() {
        isTemporary = true;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        ArrayList<ZoneInfo> infos = new ArrayList<>();
        if (zonesInfo != null && zonesInfo.size() > 0) {
            for (ZoneInfo zoneInfo : zonesInfo) {
                infos.add((ZoneInfo) zoneInfo.clone());
            }
        }
        ZonesInfo info = new ZonesInfo(infos);
        info.isTemporary = isTemporary;
        return info;
    }
}
