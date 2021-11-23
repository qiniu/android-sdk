package com.qiniu.android.transaction;

import com.qiniu.android.utils.Utils;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by yangsen on 2020/6/9
 */
public class TransactionManager {

    /// 事务链表
    protected final ConcurrentLinkedQueue<Transaction> transactionList = new ConcurrentLinkedQueue<>();
    /// 事务定时器
    private Timer timer;

    protected long actionCount = 0;

    private static final TransactionManager transactionManager = new TransactionManager();

    private TransactionManager() {
    }

    public static TransactionManager getInstance() {
        return transactionManager;
    }

    /// 根据name查找事务
    public ArrayList<Transaction> transactionsForName(String name) {
        ArrayList<Transaction> arrayList = new ArrayList<>();
        for (Transaction transaction : transactionList) {
            if ((name == null && transaction.name == null) || (transaction.name != null && transaction.name.equals(name))) {
                arrayList.add(transaction);
            }
        }
        return arrayList;
    }

    /// 是否存在某个名称的事务
    public boolean existTransactionsForName(String name) {
        boolean isExist = false;
        for (Transaction transaction : transactionList) {
            if ((name == null && transaction.name == null) || (transaction.name != null && transaction.name.equals(name))) {
                isExist = true;
                break;
            }
        }
        return isExist;
    }

    /// 添加一个事务
    public void addTransaction(Transaction transaction) {
        if (transaction == null) {
            return;
        }
        transactionList.add(transaction);
        createTimer();
    }

    /// 移除一个事务
    public void removeTransaction(Transaction transaction) {
        if (transaction == null) {
            return;
        }
        transactionList.remove(transaction);
    }

    /// 在下一次循环执行事务, 该事务如果未被添加到事务列表，会自动添加
    public synchronized void performTransaction(Transaction transaction) {
        if (transaction == null) {
            return;
        }

        if (!transactionList.contains(transaction)) {
            transactionList.add(transaction);
        }
        transaction.nextExecutionTime = Utils.currentSecondTimestamp();
    }

    /// 销毁资源 清空事务链表 销毁常驻线程
    public synchronized void destroyResource() {
        invalidateTimer();
        transactionList.clear();
    }


    private void handleAllTransaction() {
        for (Transaction transaction : transactionList) {
            handleTransaction(transaction);
            if (transaction.maybeCompleted()) {
                removeTransaction(transaction);
            }
        }
    }

    private void handleTransaction(Transaction transaction) {
        transaction.handleAction();
    }


    private synchronized void createTimer() {
        if (timer != null) {
            return;
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timerAction();
            }
        }, 0, 1000);
    }

    private void invalidateTimer() {
        timer.cancel();
        timer = null;
    }

    private void timerAction() {
        actionCount += 1;
        handleAllTransaction();
    }


    public static class Transaction {

        // 事务名称
        public final String name;
        // 事务延迟执行时间 单位：秒
        public final int after;
        // 事务内容
        public final Runnable actionHandler;


        // 普通类型事务，事务体仅会执行一次
        private static final int TransactionTypeNormal = 0;
        // 定时事务，事务体会定时执行
        private static final int TransactionTypeTime = 1;
        private final int type;

        // 事务延后时间 单位：秒
        private final int interval;
        // 创建时间
        private long createTime;
        // 下一次需要执行的时间
        protected long nextExecutionTime;

        // 已执行次数
        protected long executedCount = 0;
        private boolean isExecuting = false;


        public Transaction(String name,
                           int after,
                           Runnable actionHandler) {

            this.type = TransactionTypeNormal;
            this.name = name;
            this.after = after;
            this.interval = 0;
            this.actionHandler = actionHandler;
            this.createTime = Utils.currentSecondTimestamp();
            this.nextExecutionTime = this.createTime + after;
        }


        public Transaction(String name,
                           int after,
                           int interval,
                           Runnable actionHandler) {

            this.type = TransactionTypeTime;
            this.name = name;
            this.after = after;
            this.interval = interval;
            this.actionHandler = actionHandler;
            this.createTime = Utils.currentSecondTimestamp();
            this.nextExecutionTime = this.createTime + after;
        }

        protected boolean shouldAction() {
            long currentTime = Utils.currentSecondTimestamp();
            if (this.type == TransactionTypeNormal) {
                return executedCount < 1 && currentTime >= nextExecutionTime;
            } else if (this.type == TransactionTypeTime) {
                return currentTime >= nextExecutionTime;
            } else {
                return false;
            }
        }

        protected boolean maybeCompleted() {
            if (this.type == TransactionTypeNormal) {
                return executedCount > 0;
            } else if (this.type == TransactionTypeTime) {
                return false;
            } else {
                return false;
            }
        }

        private synchronized void handleAction() {
            if (!shouldAction()) {
                return;
            }
            if (actionHandler != null) {
                isExecuting = true;
                executedCount += 1;

                try {
                    actionHandler.run();
                } catch (Exception ignored) {
                }

                nextExecutionTime = Utils.currentSecondTimestamp() + interval;
                isExecuting = false;
            }
        }

        public boolean isExecuting() {
            return isExecuting;
        }
    }

}
