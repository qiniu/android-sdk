package com.qiniu.utils;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;

import android.os.AsyncTask;

import com.qiniu.auth.Authorizer;
import com.qiniu.conf.Conf;
import com.qiniu.resumableio.SliceUploadTask.Block;
import com.qiniu.rs.CallRet;
import com.qiniu.rs.CallBack;
import com.qiniu.rs.PutExtra;
import com.qiniu.rs.UploadCallRet;

public abstract class UploadTask extends AsyncTask<Object, Object, CallRet>{
	protected volatile HttpPost post;
	private volatile boolean upCancelled = false;
	
	protected final CallBack callback;
	protected final Authorizer auth;
	protected final InputStreamAt orginIsa;
	protected final long contentLength;
	protected final String key;
	protected final PutExtra extra;
	
	public UploadTask(Authorizer auth, InputStreamAt isa, 
			String key, PutExtra extra, CallBack ret) throws IOException{
		this.callback = ret;
		this.auth = auth;
		this.orginIsa = isa;
		this.contentLength = this.orginIsa.length();
		this.key = key;
		this.extra = extra != null ? extra : new PutExtra();
	}
	
	protected HttpClient getHttpClient(){
		return Http.getHttpClient();
	}


	@Override
	protected void onProgressUpdate(Object ...values){
		try{
			if(values[0] instanceof Long){
				long current = (Long)values[0];
				long total = (Long)values[1];
				callback.onProcess(current, total);
			}else if(values[0] instanceof Block){
				callback.onBlockSuccess((Block)values[0]);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	protected void onPostExecute(CallRet ret){
		try{
			if(ret == null){
				callback.onFailure(ret, new QiniuException(Conf.ERROR_CODE, "", "result is null"));
				return;
			}
			if(ret.getException() != null){
				callback.onFailure(ret, ret.getException());
			}else if(ret.isOk()){
					callback.onSuccess(new UploadCallRet(ret));
			}else{
				callback.onFailure(ret, new QiniuException(ret.getStatusCode(), ret.getReqId(), ret.getResponse()));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	
	public void cancel(){
		upCancelled = true;
		abort();
		this.cancel(true);
		abort();
		this.cancel(true);
	}
	
	public boolean isUpCancelled(){
		return upCancelled;
	}
	
	private void abort(){
		if(post != null){
			try{post.abort();}catch(Exception e){}
		}
	}
	
	
}
