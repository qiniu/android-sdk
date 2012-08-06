package com.qiniu.qbox.rs;

import java.io.File;
import java.io.RandomAccessFile;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.qiniu.qbox.auth.CallRet;
import com.qiniu.qbox.auth.Client;
import com.qiniu.qbox.up.BlockProgress;
import com.qiniu.qbox.up.BlockProgressNotifier;
import com.qiniu.qbox.up.ProgressNotifier;
import com.qiniu.qbox.up.ResumablePutRet;
import com.qiniu.qbox.up.UpService;

public class RSClient {
	
	/**
	 * func PutFile(url, bucketName, key, mimeType, localFile, customMeta, callbackParams string)
	 * 匿名上传一个文件(上传用的临时 url 通过 $rs->PutAuth 得到)
	 * @throws Exception 
	 */
	public static PutFileRet putFile(
		String url, String bucketName, String key, String mimeType, String localFile,
		String customMeta, Object callbackParams1) throws Exception {

		File file = new File(localFile);
		if (!file.exists() || !file.canRead()) {
			return new PutFileRet(new CallRet(400, new Exception("File does not exist or not readable.")));
		}

		if (mimeType == null || mimeType.isEmpty()) {
			mimeType = "application/octet-stream";
		}

		String entryURI = bucketName + ":" + key;
		String action = "/rs-put/" + Base64.encodeBase64String(entryURI.getBytes()) + 
				"/mimeType/" + Base64.encodeBase64String(mimeType.getBytes());
		if (customMeta != null && !customMeta.isEmpty()) {
			action += "/meta/" + Base64.encodeBase64String(customMeta.getBytes());
		}

		MultipartEntity requestEntity = new MultipartEntity();
		requestEntity.addPart("action", new StringBody(action));

		FileBody fileBody = new FileBody(new File(localFile));
		requestEntity.addPart("file", fileBody);

		if (callbackParams1 != null) {
			String callbackParams = Client.encodeParams(callbackParams1);
			if (callbackParams != null) {
				requestEntity.addPart("params", new StringBody(callbackParams));
			}
		}
		HttpPost postMethod = new HttpPost(url);
		postMethod.setEntity(requestEntity);
		
		DefaultHttpClient client = new DefaultHttpClient();
		try {
			HttpResponse response = client.execute(postMethod);
			return handleResult(response);
		} catch (Exception e) {
			e.printStackTrace();
			return new PutFileRet(new CallRet(400, e));
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	private static PutFileRet handleResult(HttpResponse response) {
		
		if (response == null || response.getStatusLine() == null) {
			return new PutFileRet(new CallRet(400, "No response"));
		}
		
		try {
			String responseBody = EntityUtils.toString(response.getEntity());
			
			StatusLine status = response.getStatusLine();
			int statusCode = (status == null) ? 400 : status.getStatusCode();
			
			return new PutFileRet(new CallRet(statusCode, responseBody));
		} catch (Exception e) {
			e.printStackTrace();
			return new PutFileRet(new CallRet(400, e));
		}
	}

	public static PutFileRet resumablePutFile(
			UpService c, String[] checksums, BlockProgress[] progresses, 
			ProgressNotifier progressNotifier, BlockProgressNotifier blockProgressNotifier,
			String bucketName, String key, String mimeType,
			RandomAccessFile f, long fsize, String customMeta, String callbackParams) {
		
		ResumablePutRet ret = c.resumablePut(f, fsize, checksums, progresses, progressNotifier, blockProgressNotifier);
		if (!ret.ok()) {
			return new PutFileRet(ret);
		}
		
		if (mimeType == null || mimeType.isEmpty()) {
			mimeType = "application/octet-stream";
		}
		
		String params = "/mimeType/" + Base64.encodeBase64String(mimeType.getBytes());
		if (customMeta != null && !customMeta.isEmpty()) {
			params += "/meta/" + Base64.encodeBase64String(customMeta.getBytes());
		}

		String entryUri = bucketName + ":" + key;
		CallRet callRet = c.makeFile("/rs-mkfile/", entryUri, fsize, params, callbackParams, checksums);
		
		return new PutFileRet(callRet);
	}
}
