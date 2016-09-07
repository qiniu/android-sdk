package com.qiniu.android.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by bailong on 16/9/7.
 */
public final class AndroidNetwork {
    public static boolean isNetWorkReady() {
        Context c = ContextGetter.applicationContext();
        if (c == null) {
            return true;
        }
        ConnectivityManager connMgr = (ConnectivityManager)
                c.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            NetworkInfo info = connMgr.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) {
            return true;
        }
    }
}
