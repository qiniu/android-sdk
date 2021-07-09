package com.qiniu.android.http.serverRegion;

import com.qiniu.android.utils.Utils;

public class UploadServerFreezeUtil {
    private final static UploadServerFreezeManager globalHttp3Freezer = new UploadServerFreezeManager();
    public static UploadServerFreezeManager globalHttp3Freezer() {
        return globalHttp3Freezer;
    }

    private final static UploadServerFreezeManager globalHttp2Freezer = new UploadServerFreezeManager();
    public static UploadServerFreezeManager globalHttp2Freezer() {
        return globalHttp2Freezer;
    }

    public static boolean isTypeFrozenByFreezeManagers(String type, UploadServerFreezeManager[] freezeManagerList) {
        if (type == null || type.length() == 0) {
            return true;
        }
        if (freezeManagerList == null || freezeManagerList.length == 0) {
            return false;
        }

        boolean isFrozen = false;
        for (UploadServerFreezeManager freezeManager : freezeManagerList) {
            isFrozen = freezeManager.isTypeFrozen(type);
            if (isFrozen) {
                break;
            }
        }
        return isFrozen;
    }

    public static String getFrozenType(String host, String ip) {
        String ipType = Utils.getIpType(ip, host);
        return String.format("%s-%s", host, ipType);
    }
}
