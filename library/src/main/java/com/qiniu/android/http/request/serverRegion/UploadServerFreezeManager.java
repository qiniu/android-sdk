package com.qiniu.android.http.request.serverRegion;

import com.qiniu.android.utils.Utils;

import java.util.Date;
import java.util.HashMap;

import okhttp3.internal.Util;

/**
 * Created by yangsen on 2020/6/3
 */
public class UploadServerFreezeManager {

    private HashMap<String, UploadServerFreezeItem> freezeInfo = new HashMap<>();
    private final static UploadServerFreezeManager manager = new UploadServerFreezeManager();

    private UploadServerFreezeManager(){
    }

    public static UploadServerFreezeManager getInstance(){
        return manager;
    }

    public boolean isFreezeHost(String host, String type){
        if (host == null || host.length() == 0){
            return true;
        }
        boolean isFreezed = true;
        String infoKey = getItemInfoKey(host, type);
        UploadServerFreezeItem item = freezeInfo.get(infoKey);
        if (item == null || !item.isFreezedByDate(new Date())){
            isFreezed = false;
        }
        return isFreezed;
    }

    public void freezeHost(String host, String type){
        if (host == null || host.length() == 0){
            return;
        }
        String infoKey = getItemInfoKey(host, type);
        UploadServerFreezeItem item = freezeInfo.get(infoKey);
        if (item == null){
            item = new UploadServerFreezeItem(host, type);
            freezeInfo.put(infoKey, item);
        }
        item.freeze();
    }

    private String getItemInfoKey(String host, String type){
        return String.format("%s:%s", (host != null ? host : "none"), (type != null ? type : "none"));
    }

    private static class UploadServerFreezeItem{
        protected final String host;
        protected final String type;
        protected Date freezeDate;

        protected UploadServerFreezeItem(String host,
                                       String type) {
            this.host = host;
            this.type = type;
        }

        protected synchronized boolean isFreezedByDate(Date date){
            boolean isFreezed = true;
            if (freezeDate == null || freezeDate.getTime() < date.getTime()){
                isFreezed = false;
            }
            return isFreezed;
        }

        protected synchronized void freeze(){
            freezeDate = new Date(Utils.currentTimestamp() + 10*60*1000);
        }
    }

}
