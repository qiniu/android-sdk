package com.qiniu.android.http.serverRegion;

import com.qiniu.android.utils.Utils;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by yangsen on 2020/6/3
 */
public class UploadServerFreezeManager {

    private ConcurrentHashMap<String, UploadServerFreezeItem> frozenInfo = new ConcurrentHashMap<>();
    private final static UploadServerFreezeManager manager = new UploadServerFreezeManager();

    public UploadServerFreezeManager() {
    }

    public static UploadServerFreezeManager getInstance() {
        return manager;
    }

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
