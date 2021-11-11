package com.qiniu.android.transaction;

import com.qiniu.android.BaseTest;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.Utils;

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

        final boolean[] executedTransaction = {false};
        String normalName = "testNormalTransaction";
        TransactionManager.Transaction normal = new TransactionManager.Transaction(normalName, 0, new Runnable() {
            @Override
            public void run() {
                LogUtil.d("1: thread:" + Thread.currentThread().getId() + new Date().toString());
                executedTransaction[0] = true;
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

        int transactionCountBefore = manager.transactionList.size();
        long actionCountBefore = manager.actionCount;
        manager.addTransaction(normal);
        manager.addTransaction(time);


        // 由于事务堆积，执行可能会延后
        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                return !executedTransaction[0];
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
