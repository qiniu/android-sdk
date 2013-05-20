package com.qiniu.resumable;

public class QueueTask {
	int finished;
	int taskCount;
	public QueueTask(int taskCount) {
		this.taskCount = taskCount;
		finished = 0;
	}

	public boolean isFinishAll() {
		finished ++;
		return finished == taskCount;
	}
}
