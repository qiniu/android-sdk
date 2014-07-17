package com.qiniu.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Semaphore;


import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;

import com.qiniu.auth.JSONObjectRet;
import com.qiniu.io.IO;
import com.qiniu.resumableio.ResumableIO;
import com.qiniu.utils.QiniuException;
import com.qiniu.conf.Conf;

import android.content.Context;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

public class UploadTest extends AndroidTestCase {
	private String uptoken = "anEC5u_72gw1kZPSy3Dsq1lo_DPXyvuPDaj4ePkN:zmaikrTu1lgLb8DTvKQbuFZ5ai0=:eyJzY29wZSI6ImFuZHJvaWRzZGsiLCJyZXR1cm5Cb2R5Ijoie1wiaGFzaFwiOlwiJChldGFnKVwiLFwia2V5XCI6XCIkKGtleSlcIixcImZuYW1lXCI6XCIgJChmbmFtZSkgXCIsXCJmc2l6ZVwiOlwiJChmc2l6ZSlcIixcIm1pbWVUeXBlXCI6XCIkKG1pbWVUeXBlKVwiLFwieDphXCI6XCIkKHg6YSlcIn0iLCJkZWFkbGluZSI6MTQ2NjIyMjcwMX0=";

	private File file;

	private boolean uploading;
	private boolean success;
	private JSONObjectRet jsonRet;
	private JSONObject resp;
	private Exception e;

	private Context context;
	private Uri uri;
	private com.qiniu.io.PutExtra extra;
	private String key = IO.UNDEFINED_KEY;

	private com.qiniu.resumableio.PutExtra rextra;
	private Semaphore sem = new Semaphore(0, true);

	public void setUp() throws Exception {
		key = UUID.randomUUID().toString();
		uploading = true;
		success = false;

		extra = new com.qiniu.io.PutExtra();
		extra.params = new HashMap<String, String>();
		extra.params.put("x:a", "测试中文  信息");

		rextra = new com.qiniu.resumableio.PutExtra();
		rextra.params = new HashMap<String, String>();
		rextra.params.put("x:a", "测试中文  信息");

		context = this.getContext();
		jsonRet = new JSONObjectRet() {
			@Override
			public void onProcess(long current, long total) {
				Log.d("UploadTest", current + "/" + total);
			}

			@Override
			public void onSuccess(JSONObject res) {
				uploading = false;
				success = true;
				resp = res;
				Log.d("UploadTest", "上传成功!  " + resp.toString());
				sem.release();
			}

			@Override
			public void onFailure(QiniuException ex) {
				uploading = false;
				success = false;
				e = ex;
				Log.d("UploadTest", "上传失败!  " + ex.getMessage());
				sem.release();
			}
		};
	}


	public void tearDown() throws Exception {
		if(file != null){
			file.delete();
		}
	}

	@SmallTest
	public void testS() throws IOException, JSONException, InterruptedException {
		file = createFile(0.2, ".test");
		uri = Uri.fromFile(file);
		IO.putFile(context, uptoken, key, uri, extra, jsonRet);
		sem.acquire();
		successCheck();
	}

	@SmallTest
	public void testIOMultiHost() throws IOException, JSONException, InterruptedException {
		String old = Conf.UP_HOST;
		Conf.UP_HOST = "http://127.0.0.1:1";
		file = createFile(0.1, "-mup.test");
		uri = Uri.fromFile(file);
		IO.putFile(context, uptoken, key, uri, extra, jsonRet);
		Conf.UP_HOST = old;
		sem.acquire();
		successCheck();
	}

	 @MediumTest
	 public void testM() throws IOException, JSONException, InterruptedException {
	 	file = createFile(0.1, "--—— 中   文   .test");
	 	uri = Uri.fromFile(file);
	 	IO.putFile(context, uptoken, key, uri, extra, jsonRet);
	 	sem.acquire();
	 	successCheck();
	 }

	@SmallTest
	public void testRS() throws IOException, JSONException, InterruptedException {
		file = createFile(0.2, ".test");
		uri = Uri.fromFile(file);
		ResumableIO.putFile(context, uptoken, key, uri, rextra, jsonRet);
		sem.acquire();
		successCheck();
	}

	@MediumTest
	public void testRM() throws IOException, JSONException, InterruptedException {
		file = createFile(4, ".test");
		uri = Uri.fromFile(file);
		ResumableIO.putFile(context, uptoken, key, uri, rextra, jsonRet);
		sem.acquire();
		successCheck();
	}

	// @MediumTest
	// public void testRL() throws IOException, JSONException, InterruptedException {
	// 	file = createFile(8.6, ".test");
	// 	uri = Uri.fromFile(file);
	// 	ResumableIO.putFile(context, uptoken, key, uri, rextra, jsonRet);
	// 	sem.acquire();
	// 	successCheck();
	// }


	private void successCheck() throws JSONException{
		Assert.assertTrue(success);
		Assert.assertNotNull(resp.optString("hash"));
		Assert.assertEquals(file.length(), resp.getLong("fsize"));
	}

	private File createFile(double fileSize, String suf) throws IOException {
		FileOutputStream fos = null;
		try{
			long size = (long)(1024 * 1024 * fileSize);
			File f = File.createTempFile("qiniu_", suf);
			f.createNewFile();
			fos = new FileOutputStream(f);
			byte [] b = getByte();
			long s = 0;
			while(s < size){
				int l = (int)Math.min(b.length, size - s);
				fos.write(b, 0, l);
				s += l;
			}
			fos.flush();
			return f;
		}finally{
			if(fos != null){
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private byte[] getByte(){
		byte [] b = new byte[1024 * 4];
		b[0] = 'A';
		for(int i=1; i < 1024 * 4 ; i++){
			b[i] = 'b';
		}
		b[1024 * 4 - 2] = '\r';
		b[1024 * 4 - 1] = '\n';
		return b;
	}

}
