package com.qiniu.android.http.networkCheck;

import com.qiniu.android.storage.GlobalConfiguration;
import com.qiniu.android.transaction.TransactionManager;

import java.util.ArrayList;

public class NetworkCheckTransaction {

    private static boolean isAddedCheckCachedIPListNetworkStatusTransaction = false;


    public static synchronized void addCheckCachedIPListNetworkStatusTransaction(){

        if (isAddedCheckCachedIPListNetworkStatusTransaction || !GlobalConfiguration.getInstance().isCheckOpen) {
            return;
        }
        isAddedCheckCachedIPListNetworkStatusTransaction = true;

        int interval = (int)Math.random()*3600 + 1800;
        TransactionManager.Transaction transaction = new TransactionManager.Transaction("CheckCachedIPListNetworkStatus",
                0, interval,
                new Runnable() {
                    @Override
                    public void run() {
                        NetworkCheckManager.getInstance().checkCachedIPListNetworkStatus();
                    }
                });
        TransactionManager.getInstance().addTransaction(transaction);
    }

    public static Long lock = 0l;
    public static void addCheckSomeIPNetworkStatusTransaction(final String[] ipArray, final String host){

        if (!GlobalConfiguration.getInstance().isCheckOpen || host == null || host.length() == 0) {
            return;
        }

        addCheckCachedIPListNetworkStatusTransaction();

        synchronized (lock) {
            String transactionName = "CheckSomeIPNetworkStatus" + host;
            TransactionManager transactionManager = TransactionManager.getInstance();

            ArrayList<TransactionManager.Transaction> transactionList = transactionManager.transactionsForName(transactionName);

            if (transactionList == null || transactionList.size() == 0) {
                TransactionManager.Transaction transaction = new TransactionManager.Transaction(transactionName, 0,
                        new Runnable() {
                            @Override
                            public void run() {
                                NetworkCheckManager.getInstance().preCheckIPNetworkStatus(ipArray, host);
                            }
                        });
                TransactionManager.getInstance().addTransaction(transaction);
            }
        }
    }

}
