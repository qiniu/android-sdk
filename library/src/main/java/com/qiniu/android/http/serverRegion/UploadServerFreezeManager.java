package com.qiniu.android.http.serverRegion;

import com.qiniu.android.utils.Utils;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by yangsen on 2020/6/3
 *
 * @hidden
 */
public class UploadServerFreezeManager {

    private ConcurrentHashMap<String, UploadServerFreezeItem> frozenInfo = new ConcurrentHashMap<>();
    private final static UploadServerFreezeManager manager = new UploadServerFreezeManager();

    /**
     * 构造函数
     */
    public UploadServerFreezeManager() {
    }

    /**
     * 获取单例
     *
     * @return 单例
     */
    public static UploadServerFreezeManager getInstance() {
        return manager;
    }

    /**
     * 查看 type 是否冻结
     *
     * @param type type
     * @return 是否冻结
     */
    public boolean isTypeFrozen(String type) {
        if (type == null || type.length() == 0) {
            return true;
        }
        boolean isFrozen = true;
        UploadServerFreezeItem item = frozenInfo.get(type);
        if (item == null || !item.isFrozenByDate(new Date())) {
            isFrozen = false;
        }
        return isFrozen;
    }

    /**
     * 冻结 type
     *
     * @param type       type
     * @param frozenTime 冻结时间
     */
    public void freezeType(String type, int frozenTime) {
        if (type == null || type.length() == 0) {
            return;
        }
        UploadServerFreezeItem item = frozenInfo.get(type);
        if (item == null) {
            item = new UploadServerFreezeItem(type);
            frozenInfo.put(type, item);
        }
        item.freeze(frozenTime);
    }

    /**
     * 解冻 type
     *
     * @param type type
     */
    public void unfreezeType(String type) {
        if (type == null || type.length() == 0) {
            return;
        }
        frozenInfo.remove(type);
    }

    private static class UploadServerFreezeItem {
        protected final String type;
        private Date freezeDate;

        private UploadServerFreezeItem(String type) {
            this.type = type;
        }

        private synchronized boolean isFrozenByDate(Date date) {
            boolean isFrozen = true;
            if (freezeDate == null || freezeDate.getTime() < date.getTime()) {
                isFrozen = false;
            }
            return isFrozen;
        }

        private synchronized void freeze(int frozenTime) {
            freezeDate = new Date(Utils.currentTimestamp() + frozenTime * 1000);
        }

    }

}
