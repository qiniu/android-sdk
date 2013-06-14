package com.qiniu.resumable;

public class QueueTask {
	int finished;
	int taskCount;
	boolean failed = false;
	public QueueTask(int taskCount) {
		this.taskCount = taskCount;
		finished = 0;
	}

	public synchronized void addFinish() {
		finished++;
	}

	public synchronized boolean isFinishAll() {
		return finished == taskCount;
	}

	public synchronized boolean addFinishAndCheckIsFinishAll() {
		addFinish();
		return isFinishAll();
	}

	public synchronized boolean isFailure() {
		return failed;
	}

	public synchronized void setFailure() {
		failed = true;
	}
}
