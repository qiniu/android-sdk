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
        return createZonesInfo(jsonObject, ApiType.ActionTypeNone);
    }

    public static ZonesInfo createZonesInfo(JSONObject jsonObject, int actionType) {
        ArrayList<ZoneInfo> zonesInfo = new ArrayList<>();
        if (jsonObject != null) {
            try {
                String[] supportApis = ApiType.apisWithActionType(actionType);
                if (supportApis != null && supportApis.length > 0) {
                    JSONObject universal = jsonObject.getJSONObject("universal");
                    JSONArray apis = universal.getJSONArray("support_apis");

                    boolean support = true;
                    for (String supportApi : supportApis) {

                        // 需要支持的  api 是否存在，任何一个不存在则不支持。
                        boolean contain = false;
                        for (int i = 0; i < apis.length(); i++) {
                            String api = apis.getString(i);
                            if (supportApi.equals(api)) {
                                contain = true;
                                break;
                            }
                        }

                        if (!contain) {
                            support = false;
                            break;
                        }
                    }

                    if (support) {
                        // 支持 api ，universal 满足条件
                        ZoneInfo zoneInfo = ZoneInfo.buildFromJson(universal);
                        if (zoneInfo != null && zoneInfo.isValid()) {
                            zonesInfo.add(zoneInfo);
                        }
                    }
                }
            } catch (Exception ignored) {
            }


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
        return zonesInfo != null && zonesInfo.size() > 0 && zonesInfo.get(0).isValid();
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
