package com.qiniu.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;


import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;

import com.qiniu.io.IO;
import com.qiniu.resumableio.ResumableIO;
import com.qiniu.resumableio.SliceUploadTask.Block;
import com.qiniu.rs.CallRet;
import com.qiniu.rs.CallBack;
import com.qiniu.rs.UploadCallRet;
import com.qiniu.rs.UploadTaskExecutor;
import com.qiniu.auth.Authorizer;
import com.qiniu.conf.Conf;

import android.content.Context;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

public class UploadTest extends AndroidTestCase {
	private String uptoken = "anEC5u_72gw1kZPSy3Dsq1lo_DPXyvuPDaj4ePkN:zmaikrTu1lgLb8DTvKQbuFZ5ai0=:eyJzY29wZSI6ImFuZHJvaWRzZGsiLCJyZXR1cm5Cb2R5Ijoie1wiaGFzaFwiOlwiJChldGFnKVwiLFwia2V5XCI6XCIkKGtleSlcIixcImZuYW1lXCI6XCIgJChmbmFtZSkgXCIsXCJmc2l6ZVwiOlwiJChmc2l6ZSlcIixcIm1pbWVUeXBlXCI6XCIkKG1pbWVUeXBlKVwiLFwieDphXCI6XCIkKHg6YSlcIn0iLCJkZWFkbGluZSI6MTQ2NjIyMjcwMX0=";
	private Authorizer auth = new Authorizer();
	{
		auth.setUploadToken(uptoken);
	}
	
	private volatile File file;

	private volatile boolean uploading;
	private volatile boolean success;
	private volatile CallBack jsonRet;
	private volatile UploadCallRet resp;
	private volatile Exception e;

	private Context context;
	private volatile Uri uri;
	private volatile com.qiniu.rs.PutExtra extra;
	private String key = IO.UNDEFINED_KEY;

	private com.qiniu.rs.PutExtra rextra;
	private Semaphore sem = new Semaphore(0, true);
	
	private volatile List<Block> lastUploadBlocks;
	
	public void setUp() throws Exception {
		key = UUID.randomUUID().toString();
		uploading = true;
		success = false;

		extra = new com.qiniu.rs.PutExtra();
		extra.params = new HashMap<String, String>();
		extra.params.put("x:a", "测试中文  信息");

		rextra = new com.qiniu.rs.PutExtra();
		rextra.params = new HashMap<String, String>();
		rextra.params.put("x:a", "测试中文  信息");

		context = this.getContext();
		
		jsonRet = new CallBack() {
			@Override
			public void onProcess(long current, long total) {
				int percent = (int)(current*100/total);
				String msg = "上传中: " + current + "/" + total + "  " + current/1024 + "K/" + total/1024 + "K; " + percent + "%";
				Log.d("UploadTest", msg);
			}

			@Override
			public void onSuccess(UploadCallRet ret) {
				uploading = false;
				success = true;
				resp = ret;
				Log.d("UploadTest", "上传成功!  " + ret.toString());
				
				removeBlocks("fileId");
				
				sem.release();
			}

			@Override
			public void onFailure(CallRet ret) {
				uploading = false;
				success = false;
				Log.d("UploadTest", "上传失败!  " + ret.toString());
				sem.release();
			}
			
			public void onBlockSuccess(Block blk){
				saveBlock(blk);
			}
		};
	}

	private List<Block> loadBlocks(String fileId) {
		if(lastUploadBlocks == null){
			lastUploadBlocks = new ArrayList<Block>();
		}
		return lastUploadBlocks; 
	}
	
	private void saveBlock(Block blk){
		if(lastUploadBlocks != null){
			lastUploadBlocks.add(blk);
		}
	}
	
	private void removeBlocks(String fileId){
		lastUploadBlocks = null;
	}

	public void tearDown() throws Exception {
		if(file != null){
			file.delete();
		}
		jsonRet = null;
		resp = null;
		context = null;
		uri = null;
		extra = null;
		lastUploadBlocks = null;
		key = IO.UNDEFINED_KEY;
	}

	@SmallTest
	public void testIOkilo() throws IOException, JSONException, InterruptedException {
		file = createFile(0.2, ".test");
		uri = Uri.fromFile(file);
		IO.putFile(context, auth, key, uri, extra, jsonRet);
		sem.acquire();
		successCheck();
	}
	
	@SmallTest
	public void testIOkiloMutiHost() throws IOException, JSONException, InterruptedException {
		String old = Conf.UP_HOST;
		Conf.UP_HOST = "http://127.0.0.1:1";
		file = createFile(4, ".test");
		uri = Uri.fromFile(file);
		IO.putFile(context, auth, key, uri, extra, jsonRet);
		sem.acquire();
		Conf.UP_HOST = old;
		successCheck();
	}
	
	@SmallTest
	public void testIOCancel() throws IOException, JSONException, InterruptedException {
		file = createFile(8, ".test");
		uri = Uri.fromFile(file);
		long s = System.currentTimeMillis();
		UploadTaskExecutor executor = IO.putFile(context, auth, key, uri, extra, jsonRet);
		sleepThenCancel(1000 * 4, executor);
		CallRet ret = executor.get();
		long end = System.currentTimeMillis();
		System.out.println(end - s);
		System.out.println(ret);
		if(ret != null){
			System.out.println(ret.getResponse());
			if(ret.getException() != null){
				ret.getException().fillInStackTrace();
			}
		}
		Assert.assertTrue((end - s) < 1000 * 5);
	}
	
	private static void sleepThenCancel(final int time, final UploadTaskExecutor executor) {
		Thread t = new Thread() {
			public void run() {
				doSleep(time);
				executor.cancel();
			}
		};
		t.setDaemon(true);
		t.start();
	}
	
	private static void doSleep(int time){
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {

		}
	}
	
	@SmallTest
	public void testRIOkilo() throws IOException, JSONException, InterruptedException {
		file = createFile(0.2, ".test");
		uri = Uri.fromFile(file);
		ResumableIO.putFile(context, auth, key, uri, rextra, jsonRet);
		sem.acquire();
		successCheck();
	}

	@MediumTest
	public void testRIOMega() throws IOException, JSONException, InterruptedException {
		file = createFile(4, ".test");
		uri = Uri.fromFile(file);
		ResumableIO.putFile(context, auth, key, uri, rextra, jsonRet);
		sem.acquire();
		successCheck();
	}

	@MediumTest
	public void testRIOMutiHost() throws IOException, JSONException, InterruptedException {
		String old = Conf.UP_HOST;
		Conf.UP_HOST = "http://127.0.0.1:1";
		file = createFile(4.5, ".test");
		uri = Uri.fromFile(file);
		ResumableIO.putFile(context, auth, key, uri, rextra, jsonRet);
		sem.acquire();
		Conf.UP_HOST = old;
		successCheck();
	}

	 @MediumTest
	 public void testRL() throws IOException, JSONException, InterruptedException {
	 	file = createFile(8, ".test");
	 	ResumableIO.putFile(auth, key, file, rextra, jsonRet);
	 	sem.acquire();
	 	successCheck();
	 }

	 @SmallTest
		public void testRIOCancel() throws IOException, JSONException, InterruptedException {
			file = createFile(8, ".test");
			uri = Uri.fromFile(file);
			long s = System.currentTimeMillis();
			UploadTaskExecutor executor = ResumableIO.putFile(context, auth, key, uri, extra, jsonRet);
			sleepThenCancel(1000 * 4, executor);
			CallRet ret = executor.get(); // 阻塞
			long end = System.currentTimeMillis();
			System.out.println(end - s);
			Assert.assertTrue((end - s) < 1000 * 5);
		}


//	 @MediumTest
//	 public void testRL701() throws IOException, JSONException, InterruptedException {
//	 	file = createFile(30.6, ".test");
//	 	uri = Uri.fromFile(file);
//	 	ResumableIO.putFile(context, uptoken, key, uri, rextra, jsonRet);
//	 	sem.acquire();
//	 }
	 
	 @MediumTest
	 public void testRLRecord() throws IOException, JSONException{
		 //doRLRecord();//    很可能失败，可调整参数测试。断点记录最小单元： 4M
	 }
	 
	 private void doRLRecord() throws IOException, JSONException{
		 Thread t = new Thread() {
			public void run() {
				doSleep(1000 * 450); // 最多运行时间 ms
				uploading = false;
			}
		};
		t.setDaemon(true);
		t.start();
		 
		 file = createFile(15.8, ".test");
		 uri = Uri.fromFile(file);
		 UploadTaskExecutor executor = null;
		 int time = 0;
		 do{
			 if(!uploading){
				 break;
			 }
			 
			 System.out.println("executors.size(): " + (time + 1));
			 List<Block> blks = loadBlocks("fileId");
			 System.out.print("blks.size(): " + blks.size() + " ==> ");
			 for(Block blk : blks ){
				 System.out.print(blk.getIdx() + ", ");
			 }
			 System.out.println();
			 
			 if(executor == null || executor.isUpCancelled()){
				 executor = ResumableIO.putFile(context, auth, key, uri, rextra, blks, jsonRet);
				 time++;
				 sleepThenCancel(1000 * 60, executor);
			 }
			 
			 doSleep(1000 * 3);
		 }while(true);
		 
		 if(e != null){
			 e.printStackTrace();
		 }
		 successCheck();
		 //Assert.assertTrue(time > 1);
	 }
	 

	private void successCheck() throws JSONException{
		if(e != null){
			e.printStackTrace();
		}
		Assert.assertTrue(success);
		Assert.assertNotNull(resp.getHash());
		Assert.assertEquals(key, resp.getKey());
		JSONObject json = new JSONObject(resp.getResponse());
		Assert.assertEquals(file.length(), json.getLong("fsize"));
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
