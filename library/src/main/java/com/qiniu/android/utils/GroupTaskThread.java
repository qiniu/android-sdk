package com.qiniu.android.utils;

import java.util.ArrayList;

public class GroupTaskThread extends Thread {

    public final GroupTaskCompleteHandler completeHandler;

    private ArrayList<GroupTask> tasks = new ArrayList<GroupTask>();

    public GroupTaskThread(GroupTaskCompleteHandler completeHandler) {
        this.completeHandler = completeHandler;
    }

    @Override
    public void run() {
        super.run();

        while (!isInterrupted()){
            boolean isAllTasksCompleted = false;
            synchronized (this) {
                isAllTasksCompleted = isAllTasksCompleted();
            }

            if (isAllTasksCompleted) {
                completeAction();
                break;
            }

            GroupTask task = getNextWaitingTask();
            if (task != null){
                task.state = GroupTask.State.Running;
                task.run(task);
            } else {
                try {
                    sleep(10);
                } catch (InterruptedException e) {}
            }
        }
    }

    public void addTask(GroupTask task) {
        synchronized (this) {
            if (!isAllTasksCompleted()) {
                tasks.add(task);
            }
        }
    }

    private GroupTask getNextWaitingTask(){
        GroupTask task = null;
        for (int i = 0; i < tasks.size(); i++) {
            GroupTask taskP = tasks.get(i);
            if (taskP.state == GroupTask.State.Waiting){
                task = taskP;
                break;
            }
        }
        return task;
    }
    private boolean isAllTasksCompleted(){
        if (tasks.size() == 0){
            return false;
        }
        boolean ret = true;
        for (int i = 0; i < tasks.size(); i++) {
            GroupTask task = tasks.get(i);
            if (task.state != GroupTask.State.Complete){
                ret = false;
                break;
            }
        }
        return ret;
    }
    private void completeAction(){
        if (completeHandler != null){
            completeHandler.complete();
        }
    }



    public abstract static class GroupTask{

        protected enum State {
            Waiting,
            Running,
            Complete
        };

        protected State state = State.Waiting;
        public final String id;

        public GroupTask() {
            this.id = null;
        }
        public GroupTask(String id) {
            this.id = id;
        }

        public abstract void run(GroupTask task);
        public void taskComplete(){
            state = State.Complete;
        }
    }



    public interface GroupTaskCompleteHandler {
        void complete();
    }
}


