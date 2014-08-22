package com.qiniu.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.net.Uri;

import com.qiniu.auth.Authorizer;
import com.qiniu.rs.CallBack;
import com.qiniu.rs.PutExtra;
import com.qiniu.rs.UploadTaskExecutor;
import com.qiniu.utils.InputStreamAt;
import com.qiniu.utils.QiniuException;

public class IO {

	public static String UNDEFINED_KEY = null;
	
	public static UploadTaskExecutor putFile(Context mContext, 
			Authorizer auth, String key, Uri uri, PutExtra extra, CallBack callback) {
		return put(auth, key, InputStreamAt.fromUri(mContext, uri), extra, callback);
	}
	
	public static UploadTaskExecutor putFile(Authorizer auth, 
			String key, File file, PutExtra extra, CallBack callback) throws FileNotFoundException {
		return put(auth, key, InputStreamAt.fromFile(file), extra, callback);
	}

	public static UploadTaskExecutor put(Authorizer auth, 
			String key, InputStreamAt input, PutExtra extra, CallBack callback) {
		try {
			SimpleUploadTask task = new SimpleUploadTask(auth, input, key, extra, callback);
			task.execute();
			return new UploadTaskExecutor(task);
		} catch (IOException e) {
			callback.onFailure(null, new QiniuException(QiniuException.IO, "build multipart", e));
			return null;
		}
	}

}

