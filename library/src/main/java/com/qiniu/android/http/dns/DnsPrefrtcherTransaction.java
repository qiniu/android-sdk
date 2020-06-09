package com.qiniu.android.http.dns;

import com.qiniu.android.common.Zone;
import com.qiniu.android.transaction.TransactionManager;

/**
 * Created by yangsen on 2020/6/4
 */
public class DnsPrefrtcherTransaction {

    private static boolean isLoadedDns = false;

    public static synchronized boolean addDnsLocalLoadTransaction(){
        if (isLoadedDns){
            return false;
        }

        isLoadedDns = true;

        TransactionManager.Transaction loadDns = new TransactionManager.Transaction("loadDns", 0, new Runnable() {
            @Override
            public void run() {
                DnsPrefetcher.getInstance().recoverCache();
                DnsPrefetcher.getInstance().localFetch();
            }
        });
        TransactionManager.getInstance().addTransaction(loadDns);
        return true;
    }


    public static synchronized boolean addDnsCheckAndPrefetchTransaction(final Zone currentZone,
                                                            final String token){
        if (token == null || token.length() == 0){
            return false;
        }

        TransactionManager manager = TransactionManager.getInstance();
        if (manager.existtransactionsForName(token)){
            return false;
        }

        TransactionManager.Transaction loadDns = new TransactionManager.Transaction(token, 0, new Runnable() {
            @Override
            public void run() {
                DnsPrefetcher.getInstance().checkAndPrefetchDnsIfNeed(currentZone, token);
            }
        });
        manager.addTransaction(loadDns);
        return true;
    }


    public static synchronized boolean setDnsCheckWhetherCachedValidTransactionAction(){
        if (! DnsPrefetcher.getInstance().isDnsOpen()){
            return false;
        }

        String name = "dnsCheckWhetherCachedValidTransaction";
        TransactionManager manager = TransactionManager.getInstance();
        if (manager.existtransactionsForName(name)){
            return false;
        }

        TransactionManager.Transaction check = new TransactionManager.Transaction(name, 10, 120, new Runnable() {
            @Override
            public void run() {
                DnsPrefetcher.getInstance().checkWhetherCachedDnsValid();
            }
        });
        manager.addTransaction(check);
        return true;
    }

}
