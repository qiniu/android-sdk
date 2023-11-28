package com.qiniu.android.http.serverRegion;

import com.qiniu.android.utils.Utils;

/**
 * UploadServerFreezeUtil
 */
public class UploadServerFreezeUtil {
    private final static UploadServerFreezeManager globalHttp3Freezer = new UploadServerFreezeManager();

    private UploadServerFreezeUtil() {
    }

    /**
     * 获取 HTTP/3 冻结管理单例
     *
     * @return 单例
     */
    public static UploadServerFreezeManager globalHttp3Freezer() {
        return globalHttp3Freezer;
    }

    private final static UploadServerFreezeManager globalHttp2Freezer = new UploadServerFreezeManager();

    /**
     * 获取 HTTP/2 冻结管理单例
     *
     * @return 单例
     */
    public static UploadServerFreezeManager globalHttp2Freezer() {
        return globalHttp2Freezer;
    }

    /**
     * 查看 type 是否被冻结管理者冻结
     *
     * @param type              type
     * @param freezeManagerList 结管理者
     * @return 是否冻结
     */
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

    /**
     * 获取 type
     *
     * @param host host
     * @param ip   ip
     * @return type
     */
    public static String getFrozenType(String host, String ip) {
        String ipType = Utils.getIpType(ip, host);
        return String.format("%s-%s", host, ipType);
    }
}
