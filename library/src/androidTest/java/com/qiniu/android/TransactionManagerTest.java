package com.qiniu.android;

import com.qiniu.android.transaction.TransactionManager;
import com.qiniu.android.utils.LogUtil;

import java.util.Date;

/**
 * Created by yangsen on 2020/6/9
 */
public class TransactionManagerTest extends BaseTest {

    public void testTransaction(){

        TransactionManager.Transaction normal = new TransactionManager.Transaction("1", 0, new Runnable() {
            @Override
            public void run() {
                LogUtil.d("1");
            }
        });
        assertNotNull(normal);


        TransactionManager.Transaction time = new TransactionManager.Transaction("1", 0, 1, new Runnable() {
            @Override
            public void run() {
                LogUtil.d("2");
            }
        });
        assertNotNull(time);
    }

    public void testTransactionManagerAddAndRemove(){

        String normalName = "testNormalTransaction";
        TransactionManager.Transaction normal = new TransactionManager.Transaction(normalName, 0, new Runnable() {
            @Override
            public void run() {
                LogUtil.d("1: thread:" + Thread.currentThread().getId() + new Date().toString());
            }
        });

        String timeName = "testTimeTransaction";
        TransactionManager.Transaction time = new TransactionManager.Transaction(timeName, 3, 2, new Runnable() {
            @Override
            public void run() {
                LogUtil.d("2: thread:" + Thread.currentThread().getId() + new Date().toString());
            }
        });

        TransactionManager manager = TransactionManager.getInstance();
        manager.addTransaction(normal);
        manager.addTransaction(time);


        wait(null, 10);

        boolean exist = manager.existTransactionsForName(normalName);
        assertFalse(exist);
        exist = manager.existTransactionsForName(timeName);
        assertTrue(exist);
    }

}
