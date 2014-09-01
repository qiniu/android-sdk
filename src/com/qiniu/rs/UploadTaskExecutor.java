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
	
	public CallRet get(){
		if(task != null){
			try {return task.get();} catch (Exception e) {} 
		}
		return null;
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
