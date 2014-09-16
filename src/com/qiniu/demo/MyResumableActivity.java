package com.qiniu.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import com.qiniu.utils.Util;

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
		
		hint.setText("连接中");
		String key = null;
		if(extra != null){
			extra.params = new HashMap<String, String>();
			extra.params.put("x:a", "bb");
		}
		
//		String id = key + "_" + uri.getPath();
		String id = MyActivity.bucketName + ":" + key + "_" + uri.getPath() + "<other>"; // 应用中唯一确定一个上传记录
//		final BlockRecord record = new MyBlockRecord(this, id);
		final BlockRecord record = new MyFileBlockRecord(this, id);
		
		List<Block> blks = record.load();
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
				record.remove();
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
				record.serialize(blk);
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
	
	static abstract class BlockRecord{
		protected final String id;
		
		public BlockRecord(Context context, String id){
			this.id = Util.urlsafeBase64(id);
		}
		
		public String getId(){
			return id;
		}
		
		public abstract List<Block> load();
		
		/**
		 * @param blk 断点记录， 以4M为一个断点单元
		 */
		public abstract void serialize(Block blk);
		
		public abstract void remove();
	}
	
	static class MyBlockRecord extends BlockRecord{
		private static HashMap<String, List<Block>> records = new HashMap<String, List<Block>>();
		
		private List<Block> lastUploadBlocks;
		public MyBlockRecord(Context context, String id){
			super(context, id);
		}
		
		public List<Block> load() {
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
		public void serialize(Block blk){
			if(lastUploadBlocks != null){
				lastUploadBlocks.add(blk);
			}
		}
		
		public void remove(){
			records.remove(id);
		}
	}

	static class MyFileBlockRecord extends BlockRecord{
		private static String split = ",";
		private File file;

		public MyFileBlockRecord(Context context, String id) {
			super(context, id);
			initDir(context);
		}
		
		private void initDir(Context context){
			File cdir = context.getCacheDir();
			file = new File(cdir, "_qiniu_");
			if(!file.exists()){
				file.mkdirs();
			}
			file = new File(file, id);
		}
		
		private void initFile(){
			if(!file.exists()){
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public List<Block> load(){
			if(!file.exists()){
				return null;
			}
			FileReader freader = null;
			BufferedReader reader = null;
			try{
				ArrayList<Block> blks = new ArrayList<Block>();
				freader = new FileReader(file);
				reader = new BufferedReader(freader);
				String line = null;
				while ((line = reader.readLine()) != null) {
					Block b = fromCsv(line);
					if(b != null){
						blks.add(b);
					}
					Log.d("File Load", line);
				}
				return blks;
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				if(reader != null){
					try{reader.close();}catch(Exception e){}
				}
				if(freader != null){
					try{freader.close();}catch(Exception e){}
				}
			}
			return null;
		}

		@Override
		public void serialize(Block blk) {
			initFile();
			String l = toCsv(blk);
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(file, true));
				writer.newLine();
				writer.write(l);
				Log.d("File Write", l);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (writer != null) {
					try {writer.close();} catch (Exception e) {}
				}
			}
		}

		@Override
		public void remove() {
			if(file != null && file.isFile()){
				file.delete();
			}
		}
		
		private Block fromCsv(String csv) {
			String[] s = csv.split(split);
			if(s.length >= 4){
				try{
					int idx = Integer.parseInt(s[0]);
					String ctx = s[1];
					long length = Long.parseLong(s[2]);
					String host = s[3];
					Block block = new Block(idx, ctx, length, host);
					return block;
				}catch(Exception e){
					e.printStackTrace();
					return null;
				}
			}else{
				return null;
			}
		}
		
		private String toCsv(Block b){
			return b.getIdx() + split + b.getCtx() + split + b.getLength() + split + b.getHost();
		}
		
	}
	
}
