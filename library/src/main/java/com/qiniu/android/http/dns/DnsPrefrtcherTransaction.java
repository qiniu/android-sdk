package com.qiniu.android.http.dns;

import com.qiniu.android.common.Zone;

/**
 * Created by yangsen on 2020/6/4
 */
public class DnsPrefrtcherTransaction {

    private static boolean isLoadedDns = false;

    public static synchronized void addDnsLocalLoadTransaction(){
        if (isLoadedDns){
            return;
        }

        isLoadedDns = true;

        DnsPrefetcher.getInstance().recoverCache();
        DnsPrefetcher.getInstance().localFetch();
    }


    public static void addDnsCheckAndPrefetchTransaction(Zone currentZone,
                                                         String token){
        if (token == null || token.length() == 0){
            return;
        }

        DnsPrefetcher.getInstance().checkAndPrefetchDnsIfNeed(currentZone, token);
    }

}
