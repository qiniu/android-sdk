package com.qiniu.android.utils;

import java.util.ArrayList;

/**
 * 组任务线程
 *
 * @hidden
 */
public class GroupTaskThread extends Thread {

    /**
     * 组任务执行完成回调
     */
    public final GroupTaskCompleteHandler completeHandler;

    private ArrayList<GroupTask> tasks = new ArrayList<GroupTask>();

    /**
     * 构造函数
     *
     * @param completeHandler 组任务执行完成回调
     */
    public GroupTaskThread(GroupTaskCompleteHandler completeHandler) {
        this.completeHandler = completeHandler;
    }

    /**
     * 组任务开始执行
     */
    @Override
    public void run() {
        super.run();

        while (!isInterrupted()) {
            boolean isAllTasksCompleted = false;
            synchronized (this) {
                isAllTasksCompleted = isAllTasksCompleted();
            }

            if (isAllTasksCompleted) {
                completeAction();
                break;
            }

            GroupTask task = getNextWaitingTask();
            if (task != null) {
                task.state = GroupTask.State.Running;
                task.run(task);
            } else {
                try {
                    sleep(10);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
     * 添加任务
     *
     * @param task 任务
     */
    public void addTask(GroupTask task) {
        synchronized (this) {
            if (!isAllTasksCompleted()) {
                tasks.add(task);
            }
        }
    }

    private GroupTask getNextWaitingTask() {
        GroupTask task = null;
        for (int i = 0; i < tasks.size(); i++) {
            GroupTask taskP = tasks.get(i);
            if (taskP.state == GroupTask.State.Waiting) {
                task = taskP;
                break;
            }
        }
        return task;
    }

    private boolean isAllTasksCompleted() {
        if (tasks.size() == 0) {
            return false;
        }
        boolean ret = true;
        for (int i = 0; i < tasks.size(); i++) {
            GroupTask task = tasks.get(i);
            if (task.state != GroupTask.State.Complete) {
                ret = false;
                break;
            }
        }
        return ret;
    }

    private void completeAction() {
        if (completeHandler != null) {
            completeHandler.complete();
        }
    }


    /**
     * 组任务
     *
     * @hidden
     */
    public abstract static class GroupTask {

        /**
         * task 状态
         *
         * @hidden
         */
        protected enum State {
            /**
             * 等待执行
             */
            Waiting,

            /**
             * 执行中
             */
            Running,

            /**
             * 执行完成
             */
            Complete
        }

        /**
         * task 状态
         */
        protected State state = State.Waiting;

        /**
         * 任务 ID
         */
        public final String id;

        /**
         * 构造函数
         */
        public GroupTask() {
            this.id = null;
        }

        /**
         * 构造函数
         *
         * @param id 任务 ID
         */
        public GroupTask(String id) {
            this.id = id;
        }

        /**
         * 执行任务
         *
         * @param task 任务
         */
        public abstract void run(GroupTask task);

        /**
         * 任务执行结束
         */
        public void taskComplete() {
            state = State.Complete;
        }
    }


    /**
     * 任务执行结束回调
     *
     * @hidden
     */
    public interface GroupTaskCompleteHandler {

        /**
         * 任务执行结束回调
         */
        void complete();
    }
}


