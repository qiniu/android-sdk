package com.qiniu.io;

import java.io.File;

import android.content.Context;
import android.net.Uri;

import com.qiniu.auth.Authorizer;
import com.qiniu.conf.Conf;
import com.qiniu.rs.CallBack;
import com.qiniu.rs.CallRet;
import com.qiniu.rs.PutExtra;
import com.qiniu.rs.UploadTaskExecutor;
import com.qiniu.utils.InputStreamAt;

public class IO {

	public static String UNDEFINED_KEY = null;
	
	public static UploadTaskExecutor putFile(Context mContext, 
			Authorizer auth, String key, Uri uri, PutExtra extra, CallBack callback) {
		try {
			return put(auth, key, InputStreamAt.fromUri(mContext, uri), extra, callback);
		} catch (Exception e) {
			callback.onFailure(new CallRet(Conf.ERROR_CODE, "", e));
			return null;
		}
	}
	
	public static UploadTaskExecutor putFile(Authorizer auth, String key, 
			File file, PutExtra extra, CallBack callback) {
		try {
			return put(auth, key, InputStreamAt.fromFile(file), extra, callback);
		} catch (Exception e) {
			callback.onFailure(new CallRet(Conf.ERROR_CODE, "", e));
			return null;
		}
	}

	public static UploadTaskExecutor put(Authorizer auth, 
			String key, InputStreamAt input, PutExtra extra, CallBack callback) {
		try {
			SimpleUploadTask task = new SimpleUploadTask(auth, input, key, extra, callback);
			task.execute();
			return new UploadTaskExecutor(task);
		} catch (Exception e) {
			callback.onFailure(new CallRet(Conf.ERROR_CODE, "", e));
			return null;
		}
	}

}

