package com.qiniu.android;

import com.qiniu.android.transaction.TransactionManager;
import com.qiniu.android.utils.LogUtil;

import java.lang.reflect.Field;
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

        final int[] executedTransaction = {0};
        String normalName = "testNormalTransaction";
        TransactionManager.Transaction normal = new TransactionManager.Transaction(normalName, 0, new Runnable() {
            @Override
            public void run() {
                LogUtil.d("1: thread:" + Thread.currentThread().getId() + new Date().toString());
                executedTransaction[0] += 1;
            }
        });

        try {
            Field executedCountField = TransactionManager.Transaction.class.getField("executedCount");
            long executedCount = executedCountField.getLong(normal);
            assertEquals("Transaction executedCount was not 0", 0, executedCount);
        } catch (Exception e) {
            e.printStackTrace();
        }

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


        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                return executedTransaction[0] > 0;
            }
        }, 60);
        
        wait(null, 6);

        try {
            Field executedCountField = TransactionManager.Transaction.class.getField("executedCount");
            long executedCount = executedCountField.getLong(normal);
            assertEquals("Transaction executedCount was not 1", 1, executedCount);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertEquals("executed time was not 1", 1, executedTransaction[0]);

        boolean exist = manager.existTransactionsForName(normalName);
        assertFalse(exist);
        exist = manager.existTransactionsForName(timeName);
        assertTrue(exist);
    }

}
