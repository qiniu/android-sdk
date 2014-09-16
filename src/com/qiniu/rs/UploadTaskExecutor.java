package com.qiniu.rs;

import com.qiniu.utils.UploadTask;

public class UploadTaskExecutor {
	private volatile UploadTask task;
	
	public UploadTaskExecutor() {
		
	}
	
	public UploadTaskExecutor(UploadTask task) {
		this.task = task;
	}

	public void setTask(UploadTask task) {
		this.task = task;
	}
	
	public UploadTask getTask(){
		return task;
	}
	
	public boolean isUpCancelled(){
		return task != null && task.isUpCancelled();
	}
	
	public void cancel(){
		if(task != null){
			try{task.cancel();}catch(Exception e){}
		}
	}
	
}
