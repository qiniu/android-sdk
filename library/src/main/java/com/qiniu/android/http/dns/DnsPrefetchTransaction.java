package com.qiniu.android.http.dns;

import com.qiniu.android.common.Zone;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.transaction.TransactionManager;

/**
 * Created by yangsen on 2020/6/4
 */
public class DnsPrefetchTransaction {

    private static boolean isDnsLoaded = false;

    public static synchronized boolean addDnsLocalLoadTransaction() {
        if (isDnsLoaded) {
            return false;
        }

        if (!DnsPrefetcher.getInstance().isDnsOpen()) {
            return false;
        }

        isDnsLoaded = true;

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


    public static synchronized boolean addDnsCheckAndPrefetchTransaction(final Zone currentZone, final UpToken token) {
        if (!DnsPrefetcher.getInstance().isDnsOpen()) {
            return false;
        }

        if (token == null || token.token == null || token.token.length() == 0) {
            return false;
        }

        TransactionManager manager = TransactionManager.getInstance();
        if (manager.existTransactionsForName(token.token)) {
            return false;
        }

        TransactionManager.Transaction loadDns = new TransactionManager.Transaction(token.token, 0, new Runnable() {
            @Override
            public void run() {
                DnsPrefetcher.getInstance().checkAndPrefetchDnsIfNeed(currentZone, token);
            }
        });
        manager.addTransaction(loadDns);
        return true;
    }

    public static synchronized boolean addDnsCheckAndPrefetchTransaction(final String[] hosts) {
        if (!DnsPrefetcher.getInstance().isDnsOpen()) {
            return false;
        }

        if (hosts == null || hosts.length == 0) {
            return false;
        }

        TransactionManager manager = TransactionManager.getInstance();
        TransactionManager.Transaction loadDns = new TransactionManager.Transaction(null, 0, new Runnable() {
            @Override
            public void run() {
                DnsPrefetcher.getInstance().addPreFetchHosts(hosts);
            }
        });
        manager.addTransaction(loadDns);
        return true;
    }


    public static synchronized boolean setDnsCheckWhetherCachedValidTransactionAction() {
        if (!DnsPrefetcher.getInstance().isDnsOpen()) {
            return false;
        }

        String name = "dnsCheckWhetherCachedValidTransaction";
        TransactionManager manager = TransactionManager.getInstance();
        if (manager.existTransactionsForName(name)) {
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
