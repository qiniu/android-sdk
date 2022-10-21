package com.qiniu.android.transaction;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.Utils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.Date;

/**
 * Created by yangsen on 2020/6/9
 */
@RunWith(AndroidJUnit4.class)
public class TransactionManagerTest extends BaseTest {

    @Test
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

    @Test
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
            Field executedCountField = TransactionManager.Transaction.class.getDeclaredField("executedCount");
            executedCountField.setAccessible(true);
            long executedCount = executedCountField.getLong(normal);
            System.out.print("A Transaction executedCount:" + executedCount);
            assertEquals("A Transaction executedCount was not 1", 0, executedCount);
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

        int transactionCountBefore = manager.transactionList.size();
        long actionCountBefore = manager.actionCount;
        manager.addTransaction(normal);
        manager.addTransaction(time);


        // 由于事务堆积，执行可能会延后
        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                return executedTransaction[0] == 0;
            }
        }, 5 * 60);
        
        wait(null, 6);

        String leftTransactionName = "";
        for (TransactionManager.Transaction t : manager.transactionList) {
            leftTransactionName += " " + t.name + " ";
        }
        int transactionCountAfter = manager.transactionList.size();

        long timestamp = Utils.currentSecondTimestamp();
        String assertInfo = "manager action count before:" + actionCountBefore;
        assertInfo += " manager action count after:" + manager.actionCount;
        assertInfo += " manager transaction count before:" + transactionCountBefore;
        assertInfo += " manager transaction count after:" + transactionCountAfter;
        assertInfo += " manager left transaction:" + leftTransactionName;
        assertInfo += " normal.executedCount:" + normal.executedCount;
        assertInfo += " normal nextExecutionTime:" + normal.nextExecutionTime;
        assertInfo += " now:" + timestamp;
        assertInfo += " should action:" + normal.shouldAction();
        assertInfo += " maybeCompleted:" + normal.maybeCompleted();

        assertTrue("timestamp:: " + assertInfo, normal.nextExecutionTime < timestamp);
        assertTrue("maybeCompleted:: " + assertInfo, normal.maybeCompleted());
        assertEquals("executedCount:: " + assertInfo, 1, normal.executedCount);

        boolean exist = manager.existTransactionsForName(normalName);
        assertFalse(exist);
        exist = manager.existTransactionsForName(timeName);
        assertTrue(exist);
    }

}
