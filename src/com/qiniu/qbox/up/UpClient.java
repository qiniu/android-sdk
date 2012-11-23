package com.qiniu.qbox.up;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.qiniu.qbox.auth.CallRet;
import com.qiniu.qbox.auth.Client;

public class UpClient {
	private static final String MIME_TYPE = "mimeType" ;
	private static final String CUSTOM_META = "customMeta" ;
	private static final String CALL_BACK_PARMS = "callbackParams" ;
	private static final String PROGRESS_FILE = "progressFile" ;
	private static final String ROTATE = "rotate" ;
	
	public static PutFileRet resumablePutFile(
			UpService c, String[] checksums, BlockProgress[] progresses, 
			ProgressNotifier progressNotifier, BlockProgressNotifier blockProgressNotifier,
			String bucketName, String key, String mimeType,
			RandomAccessFile file, long fsize, String customMeta, String callbackParams) {
		
		ResumablePutRet ret = c.resumablePut(file, fsize, checksums, progresses, progressNotifier, blockProgressNotifier);
		if (!ret.ok()) {
			return new PutFileRet(ret);
		}
		
		if (mimeType == null || mimeType.length() == 0) {
			mimeType = "application/octet-stream";
		}
		
		String params = "/mimeType/" + Client.urlsafeEncodeString(mimeType.getBytes());
		if (customMeta != null && customMeta.length() != 0) {
			params += "/meta/" + Client.urlsafeEncodeString(customMeta.getBytes()) ;
		}

		String entryUri = bucketName + ":" + key;
		CallRet callRet = c.makeFile("/rs-mkfile/", entryUri, fsize, params, callbackParams, checksums);
		
		return new PutFileRet(callRet);
	}
	
	/**
	 * 
	 * @param c
	 * @param bucketName	
	 * @param key	
	 * @param localFile	
	 * @param progressFile 
	 * @return PutFileRet
	 */
	
	/**
	 * We provide another way to put a local file to the qiniu cloud server easily.
	 * @param c
	 * @param bucketName
	 * @param key	An unique key.
	 * @param localFile	The file that you want to upload.
	 * @param optParams The temporary file that saves the uploading progress.
	 * @return
	 */
	
	public static PutFileRet resumablePutFile(UpService c, String bucketName, String key, String localFile, Map<String, Object> optParams) {
		// set all the optional parameter to legal empty value.
		String mimeType = "" ;
		String customMeta = "" ;
		String progressFile = "";
		String callbackParams = "" ;
		
		if (optParams != null) {	// get the parameter value passed by the caller.
			mimeType = (String)optParams.get(MIME_TYPE) ;
			customMeta = (String)optParams.get(CUSTOM_META) ;
			progressFile = (String)optParams.get(PROGRESS_FILE) ;
			callbackParams = (String)optParams.get(CALL_BACK_PARMS) ;
		}
		
		RandomAccessFile f = null;
		PutFileRet putFileRet = null;
		
		try {
			f = new RandomAccessFile(localFile, "r");

			long fsize = f.length();
			int blockCount = UpService.blockCount(fsize);
			if (progressFile == null || "".equals(progressFile)) {
				progressFile = localFile + ".progress" + fsize;
			}

			String[] checksums = new String[(int) blockCount];
			BlockProgress[] progresses = new BlockProgress[(int) blockCount];

			readProgress(progressFile, checksums, progresses, blockCount);
			ResumableNotifier notif = new ResumableNotifier(progressFile);
					
			putFileRet = resumablePutFile(c, checksums, progresses,
					(ProgressNotifier) notif, (BlockProgressNotifier) notif,
					bucketName, key, mimeType, f, fsize, customMeta, callbackParams);
			
			// upload file successfully, remove the progress file.
			if (putFileRet.ok()) { 
				File del = new File(progressFile) ;
				if (!del.delete()) {
					throw new Exception("Fail to remove the progress file : " + progressFile) ;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return putFileRet;
	}
	
	private static class ResumableNotifier implements ProgressNotifier,
			BlockProgressNotifier {
		private PrintStream os;

		public ResumableNotifier(String progressFile) throws Exception {
			OutputStream out = new FileOutputStream(progressFile, true);
			this.os = new PrintStream(out, true);
		}

		// notify that a block has been uploaded successfully.
		public void notify(int blockIndex, String checksum) {
			try {
				HashMap<String, Object> doc = new HashMap<String, Object>();
				doc.put("block", blockIndex);
				doc.put("checksum", checksum);
				JSONObject obj = new JSONObject(doc) ;
				String json = obj.toString() ;
				// save the upload progress
				os.println(json);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// notify if a chunck has been uploaded sucessfully.
		public void notify(int blockIndex, BlockProgress progress) {
			try {
				HashMap<String, Object> doc = new HashMap<String, Object>();
				doc.put("block", blockIndex);

				Map<String, String> map = new HashMap<String, String>();
				map.put("context", progress.context);
				map.put("offset", progress.offset + "");
				map.put("restSize", progress.restSize + "");
				doc.put("progress", map);

				JSONObject obj = new JSONObject(doc) ;
				String json = obj.toString() ;
				// save the upload progress
				os.println(json);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void readProgress(String file, String[] checksums,
			BlockProgress[] progresses, int blockCount) throws Exception {
		File fi = new File(file);
		if (!fi.exists()) {
			return;
		}
		FileReader f = new FileReader(file);
		BufferedReader is = new BufferedReader(f);

		for (;;) {
			String line = is.readLine();
			if (line == null) // has no content any more
				break;

			JSONObject o = new JSONObject(line);
			Object block = o.get("block");
			if (block == null) { // invalid content
				break;
			}
			int blockIdx = (Integer) block;
			if (blockIdx < 0 || blockIdx >= blockCount) { // invalid blockIndex
				break;
			}

			Object checksum = null;
			if (o.has("checksum")) {
				checksum = o.get("checksum");
			}

			// each block has a checksum value
			if (checksum != null) {
				checksums[blockIdx] = (String) checksum;
				continue;
			}

			JSONObject progress = null;
			if (o.has("progress")) {
				progress = (JSONObject) o.get("progress");
			}

			if (progress != null) {
				BlockProgress bp = new BlockProgress();
				bp.context = progress.getString("context");
				bp.offset = progress.getInt("offset");
				bp.restSize = progress.getInt("restSize");
				progresses[blockIdx] = bp;
				continue;
			}
			break; // error ...
		}

		if (is != null) {
			is.close();
			is = null;
		}
	}
	
	
	public static PutFileRet putFile(String upToken, String bucketName, String key,
				String localFile, Map<String, Object> optParams) throws Exception {
		String mimeType = "";
		String customMeta = "";
		Object callbackParam = "";
		String rotate = "";
		
		if (optParams != null) {
			mimeType = (String)optParams.get(MIME_TYPE) ;
			customMeta = (String)optParams.get(CUSTOM_META) ;
			callbackParam = optParams.get(CALL_BACK_PARMS) ;
			rotate = (String)optParams.get(ROTATE) ;
		}
		
		File file = new File(localFile);
		if (!file.exists() || !file.canRead()) {
			return new PutFileRet(new CallRet(400, new Exception(
					"File does not exist or not readable.")));
		}

		if (mimeType == null || mimeType.length() == 0) {
			mimeType = "application/octet-stream";
		}

		String entryURI = bucketName + ":" + key;
		String action = "/rs-put/" + Client.urlsafeEncode(entryURI) + "/mimeType/" 
				+ Client.urlsafeEncode(mimeType) ;
		
		if (customMeta != null && customMeta.length() != 0) {
			action += "/meta/" + Client.urlsafeEncode(customMeta);
		}
		
		if (rotate != null && rotate.length() != 0) { 
			action += "/rotate/" + rotate ;
		}
		
		MultipartEntity requestEntity = new MultipartEntity();
		requestEntity.addPart("auth", new StringBody(upToken)) ;
		requestEntity.addPart("action", new StringBody(action));
		FileBody fileBody = new FileBody(new File(localFile));
		requestEntity.addPart("file", fileBody);

		if (callbackParam != null) {
			String callbackParam1 = Client.encodeParams(callbackParam);
			if (callbackParam1 != null) {
				requestEntity.addPart("params", new StringBody(callbackParam1));
			}
		}
		HttpPost postMethod = new HttpPost(Config.UP_HOST + "/upload");
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

}
