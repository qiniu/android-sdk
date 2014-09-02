package com.qiniu.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.qiniu.R;
import com.qiniu.auth.Authorizer;
import com.qiniu.resumableio.ResumableIO;
import com.qiniu.resumableio.SliceUploadTask.Block;
import com.qiniu.rs.CallBack;
import com.qiniu.rs.CallRet;
import com.qiniu.rs.PutExtra;
import com.qiniu.rs.UploadCallRet;
import com.qiniu.rs.UploadTaskExecutor;

public class MyResumableActivity extends Activity implements View.OnClickListener {
	
	 private Authorizer auth = new Authorizer();
	{
			auth.setUploadToken(MyActivity.uptoken); 
	}
	
	private ProgressBar pb;
	private Button start;
	private Button stop;
	private TextView hint;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.resumable);
		initWidget();
	}

	public void initWidget() {
		pb = (ProgressBar) findViewById(R.id.progressBar);
		pb.setMax(100);
		hint = (TextView) findViewById(R.id.textView2);
		start = (Button) findViewById(R.id.button1);
		start.setOnClickListener(this);
		stop = (Button) findViewById(R.id.button2);
		stop.setOnClickListener(this);
	}


	@Override
	public void onClick(View view) {
		if (view.equals(start)) {
			selectFile();
			return;
		}
		if (view.equals(stop)) {
			if(executor != null && uploading){
				executor.cancel();
				uploading = false;
				clean();
				stop.setText("PLAY");
			}else{
				stop.setText("STOP");
				hint.setText("连接中");
				doResumableUpload(uploadUri, mExtra);
			}
		}
	}
	
	private void clean(){
		executor = null;
	}
	
	volatile boolean uploading = false;
	UploadTaskExecutor executor;
	Uri uploadUri;
	PutExtra mExtra = new PutExtra();

	public void doResumableUpload(final Uri uri, PutExtra extra) {
		uploadUri = uri;
		final MyBlockRecord record = MyBlockRecord.genFromUri(this, uri);
		
		hint.setText("连接中");
		String key = null;
		if(extra != null){
			extra.params = new HashMap<String, String>();
			extra.params.put("x:a", "bb");
		}
		List<Block> blks = record.loadBlocks();
		String s = "blks.size(): " + blks.size() + " ==> ";
		 for(Block blk : blks ){
			 s += blk.getIdx() + ", ";
		 }
		 final String pre = s + "\r\n";
		 uploading = true;
		executor = ResumableIO.putFile(this, auth, key, uri, extra, blks, new CallBack() {
			@Override
			public void onSuccess(UploadCallRet ret) {
				uploading = false;
				String key = ret.getKey();
				String redirect = "http://" + MyActivity.bucketName + ".qiniudn.com/" + key;
				String redirect2 ="http://" + MyActivity.bucketName + ".u.qiniudn.com/" + key;
				hint.setText(pre + "上传成功! ret: " + ret.toString() + "  \r\n可到" + redirect + " 或  " + redirect2 + " 访问");
				record.removeBlocks();
				clean();
			}

			@Override
			public void onProcess(long current, long total) {
				int percent = (int)(current*100/total);
				hint.setText(pre + "上传中: " + current + "/" + total + "  " + current/1024 + "K/" + total/1024 + "K; " + percent + "%");
				//int i = 3/0;
				pb.setProgress((int) percent);
			}

			@Override
			public void onBlockSuccess(Block blk){
				record.saveBlock(blk);
			}

			@Override
			public void onFailure(CallRet ret) {
				uploading = false;
				clean();
				hint.setText(pre + "错误: " + ret.toString());
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) return;
		PutExtra e = new PutExtra();
		doResumableUpload(data.getData(), e);
	}

	public void selectFile() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		try {
			startActivityForResult(Intent.createChooser(intent, "请选择一个要上传的文件"), 1);
		} catch (android.content.ActivityNotFoundException ex) {
			ex.printStackTrace();
		}
	}
	
	static class MyBlockRecord{
		private static HashMap<String, List<Block>> records = new HashMap<String, List<Block>>();
		
		public static MyBlockRecord genFromUri(Context context, Uri uri){
			String id = uri.getPath() + context.toString();
			return new MyBlockRecord(id);
		}
		
		private final String id;
		private List<Block> lastUploadBlocks;
		public MyBlockRecord(String id){
			this.id = id;
		}
		
		public List<Block> loadBlocks() {
			if(lastUploadBlocks == null){
				List<Block> t = records.get(id);
				if(t == null){
					t = new ArrayList<Block>();
					records.put(id, t);
				}
				lastUploadBlocks = t;
			}
			return lastUploadBlocks; 
		}
		
		/**
		 * @param blk 断点记录， 以4M为一个断点单元
		 */
		public void saveBlock(Block blk){
			if(lastUploadBlocks != null){
				lastUploadBlocks.add(blk);
			}
		}
		
		public void removeBlocks(){
			records.remove(id);
		}
	}
	
}
