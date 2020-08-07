package com.qiniu.android.transaction;


import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by yangsen on 2020/6/9
 */
public class TransactionManager {

    /// 事务链表
    private ConcurrentLinkedQueue<Transaction> transactionList = new ConcurrentLinkedQueue<>();
    /// 定时器执行次数
    private long time = 0;
    /// 事务定时器
    private Timer timer;

    private static final TransactionManager transactionManager = new TransactionManager();
    private TransactionManager(){
    }

    public static TransactionManager getInstance(){
        return transactionManager;
    }

    /// 根据name查找事务
    public ArrayList<Transaction> transactionsForName(String name){
        ArrayList<Transaction> arrayList = new ArrayList<>();
        for (Transaction transaction : transactionList){
            if ((name == null && transaction.name == null)
                    || (name != null && transaction.name != null && transaction.name.equals(name))) {
                arrayList.add(transaction);
            }
        }
        return arrayList;
    }

    /// 是否存在某个名称的事务
    public boolean existTransactionsForName(String name){
        boolean isExist = false;
        for (Transaction transaction : transactionList){
            if ((name == null && transaction.name == null)
                    || (name != null && transaction.name != null && transaction.name.equals(name))) {
                isExist = true;
                break;
            }
        }
        return isExist;
    }

    /// 添加一个事务
    public void addTransaction(Transaction transaction){
        if (transaction == null) {
            return;
        }
        transactionList.add(transaction);
        createTimer();
    }

    /// 移除一个事务
    public void removeTransaction(Transaction transaction){
        if (transaction == null) {
            return;
        }
        transactionList.remove(transaction);
    }

    /// 在下一次循环执行事务, 该事务如果未被添加到事务列表，会自动添加
    public synchronized void performTransaction(Transaction transaction){
        if (transaction == null) {
            return;
        }

        if (! transactionList.contains(transaction)){
            transactionList.add(transaction);
        }

        transaction.actionTime = time;
    }

    /// 销毁资源 清空事务链表 销毁常驻线程
    public synchronized void destroyResource(){
        invalidateTimer();
        transactionList.clear();
    }


    private void handleAllTransaction(){
        for (Transaction transaction : transactionList){
            handleTransaction(transaction);
            if (transaction.maybeCompleted(time)){
                removeTransaction(transaction);
            }
        }
    }

    private void handleTransaction(Transaction transaction){
        transaction.handleAction(time);
    }


    private synchronized void createTimer(){
        if (timer != null){
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

    private void invalidateTimer(){
        timer.cancel();
        timer = null;
    }

    private void timerAction(){
        time += 1;
        handleAllTransaction();
    }



    public static class Transaction  {

        // 事务名称
        public final String name;
        // 事务延迟执行时间 单位：秒
        public final int after;
        // 事务内容
        public final Runnable actionHandler;


        // 普通类型事务，事务体仅会执行一次
        private static int TransactionTypeNormal = 0;
        // 定时事务，事务体会定时执行
        private static int TransactionTypeTime = 1;
        private final int type;

        // 事务延后时间 单位：秒
        private final int interval;

        // 事务执行时间 与事务管理者定时器时间相关联
        private long actionTime;


        public Transaction(String name,
                           int after,
                           Runnable actionHandler){

            this.type = TransactionTypeNormal;
            this.name = name;
            this.after = after;
            this.interval = 0;
            this.actionHandler = actionHandler;
        }


        public Transaction(String name,
                           int after,
                           int interval,
                           Runnable actionHandler){

            this.type = TransactionTypeTime;
            this.name = name;
            this.after = after;
            this.interval = interval;
            this.actionHandler = actionHandler;
        }

        private boolean shouldAction(long time){
            return time >= actionTime;
        }

        private boolean maybeCompleted(long time){
            return shouldAction(time) && type == TransactionTypeNormal;
        }

        private void handleAction(long time){
            if (! shouldAction(time)){
                return;
            }
            if (actionHandler != null){
                actionHandler.run();
            }
            if (type == TransactionTypeNormal){
                actionTime = 0;
            } else if (type == TransactionTypeTime){
                actionTime = time + interval;
            }
        }
    }

}
