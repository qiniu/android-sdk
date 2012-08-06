package com.qiniu.qbox.up;

import java.io.RandomAccessFile;

import android.util.Base64;

import com.qiniu.qbox.auth.CallRet;

public class UpClient {

	public static PutFileRet resumablePutFile(
			UpService c, String[] checksums, BlockProgress[] progresses, 
			ProgressNotifier progressNotifier, BlockProgressNotifier blockProgressNotifier,
			String bucketName, String key, String mimeType,
			RandomAccessFile f, long fsize, String customMeta, String callbackParams) {
		
		ResumablePutRet ret = c.resumablePut(f, fsize, checksums, progresses, progressNotifier, blockProgressNotifier);
		if (!ret.ok()) {
			return new PutFileRet(ret);
		}
		
		if (mimeType == null || mimeType.length() == 0) {
			mimeType = "application/octet-stream";
		}
		
		String params = "/mimeType/" + Base64.encodeToString(mimeType.getBytes(), Base64.URL_SAFE);
		if (customMeta != null && customMeta.length() != 0) {
			params += "/meta/" + Base64.encodeToString(customMeta.getBytes(), Base64.URL_SAFE);
		}

		String entryUri = bucketName + ":" + key;
		CallRet callRet = c.makeFile("/rs-mkfile/", entryUri, fsize, params, callbackParams, checksums);
		
		return new PutFileRet(callRet);
	}
}
