package com.qiniu.android.http.dns;

import com.qiniu.android.common.Zone;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.transaction.TransactionManager;

/**
 * Created by yangsen on 2020/6/4
 */
public class DnsPrefetchTransaction {

    private static boolean isDnsLoaded = false;

    private DnsPrefetchTransaction() {
    }

    /**
     * 将 SDK 内部使用域名的 Dns 预解析操作添加到周期性的事务中
     *
     * @return 添加是否成功
     */
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

    /**
     * 将 zone 中使用域名的 Dns 预解析操作添加到周期性的事务中
     *
     * @param currentZone zone
     * @param token       上传 token
     * @return 添加是否成功
     */
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

    /**
     * 将 hosts 中域名的 Dns 预解析操作添加到周期性的事务中
     *
     * @param hosts 域名
     * @return 添加是否成功
     */
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

    /**
     * 将检查缓存是否有效的操作添加到周期性事务中，无效则重新拉取
     *
     * @return 添加是否成功
     */
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
