package com.qiniu.qbox.demo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.qiniu.qbox.R;
import com.qiniu.qbox.auth.DigestAuthClient;
import com.qiniu.qbox.auth.UpTokenClient;
import com.qiniu.qbox.up.BlockProgress;
import com.qiniu.qbox.up.BlockProgressNotifier;
import com.qiniu.qbox.up.Config;
import com.qiniu.qbox.up.GetRet;
import com.qiniu.qbox.up.ProgressNotifier;
import com.qiniu.qbox.up.PutFileRet;
import com.qiniu.qbox.up.RSService;
import com.qiniu.qbox.up.UpClient;
import com.qiniu.qbox.up.UpService;

@SuppressLint("HandlerLeak")
public class MainActivity extends Activity {
	private static final int UPLOAD_CONTINUE_NOTIFIER = 0x01;
	private static final int UPLOAD_FINISED_NOTIFIER = 0x02;

	private Button chooseButton;
	private ProgressBar progressBar;

	private long current;
	private long fsize;
	
	private PutFileRet putFileRet ;

	public class FileProgressNotifier implements ProgressNotifier,
			BlockProgressNotifier {

		@Override
		public void notify(int blockIndex, String checksum) {
			System.out.println("Progress Notify:" + "\n\tBlockIndex: "
					+ String.valueOf(blockIndex) + "\n\tChecksum: " + checksum);
		}

		@Override
		public void notify(int blockIndex, BlockProgress progress) {
			current = progress.offset + Config.BLOCK_SIZE * blockIndex;
			Message msg = new Message();
			msg.what = (current >= fsize ? UPLOAD_FINISED_NOTIFIER : UPLOAD_CONTINUE_NOTIFIER);
			MainActivity.this.myMessageHandler.sendMessage(msg); // notify the progress bar to update

			System.out.println("BlockProgress Notify:" + "\n\tBlockIndex: "
					+ String.valueOf(blockIndex) + "\n\tContext: "
					+ progress.context + "\n\tOffset: "
					+ String.valueOf(progress.offset) + "\n\tRestSize: "
					+ String.valueOf(progress.restSize));
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		this.chooseButton = (Button) this.findViewById(R.id.chooseButton);
		this.chooseButton.setText("Click to upload a file!!!") ;
		
		progressBar = (ProgressBar) findViewById(R.id.uploadProgressBar);
		progressBar.setVisibility(View.VISIBLE);
		progressBar.setProgress(0);
		
		Config.ACCESS_KEY = "";
		Config.SECRET_KEY = "";
		// 处理事件
		this.chooseButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread(new Runnable() { 
					public void run() {
						try {
							String bucketName = "bucket";
							String key = "localread20120709-3.jpg";

							AuthPolicy policy = new AuthPolicy("bucket", 3600);
							String token = policy.makeAuthTokenString();
							UpTokenClient upTokenClient = new UpTokenClient(token);
							UpService upClient = new UpService(upTokenClient);

							RandomAccessFile f = new RandomAccessFile("/mnt/sdcard/mm.jpg", "r");
							fsize = f.length();
							progressBar.setMax((int) fsize);

							long blockCount = UpService.blockCount(fsize);
							String[] checksums = new String[(int) blockCount];
							BlockProgress[] progresses = new BlockProgress[(int) blockCount];
							FileProgressNotifier notif = new FileProgressNotifier();

							putFileRet = UpClient.resumablePutFile(
									upClient, checksums, progresses,
									(FileProgressNotifier) notif,
									(BlockProgressNotifier) notif, bucketName,
									key, "", f, fsize, "CustomMeta", "");

							if (putFileRet.ok()) {
								System.out.println("Successfully put file resumably: "+ putFileRet.getHash());
							} else {
								System.out.println("Failed to put file resumably: "+ putFileRet);
							}
							
							DigestAuthClient conn = new DigestAuthClient() ;
							RSService rs = new RSService(conn, bucketName) ;
							GetRet getRet = rs.get(key, key) ;
							System.out.println("  GetRet : " + getRet);
							System.out.println("  url : " + getRet.getUrl());
						} catch (FileNotFoundException e1) {
							e1.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					};
				}).start();
			}
		});
	}
	
	@SuppressWarnings("unused")
	// upload a file to qiniu cloud server directly. Demo
	private void uploadWithToken() {
		String localFile = "/mnt/sdcard/mm.jpg" ;
		Config.ACCESS_KEY = "";
		Config.SECRET_KEY = "";
		String bucketName = "bucketName";
		String key = "wjl.test";

		AuthPolicy policy = new AuthPolicy(bucketName, 3600);
		String token = policy.makeAuthTokenString();
		// 可选参数
		Map<String, Object> optParams = new HashMap<String, Object>() ;
		optParams.put("mimeType", "") ;
		optParams.put("customMeta", "") ;
		optParams.put("callbackParms", "") ;
		optParams.put("rotate", "2") ;
		try {
			putFileRet = UpClient.putFile(token, bucketName, key, localFile, optParams);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (putFileRet.ok()) {
			System.out.println("Token successfully upload a file : " + putFileRet.getHash());
		} else {
			System.out.println("Token Fail to upload a file : " + putFileRet);
		}

		try {
			DigestAuthClient conn = new DigestAuthClient() ;
			RSService rs = new RSService(conn, bucketName) ;
			GetRet getRet = rs.get(key, key) ;
			System.out.println(" ** GetRet : " + getRet);
			System.out.println(" ** url : " + getRet.getUrl());
		} catch (Exception e) {
			e.printStackTrace() ;
		}
	}
	
	// another way to upload a file in resumable way . Demo
	@SuppressWarnings("unused")
	private void resumablePutFile() {
		String localFile = "/mnt/sdcard/mm.jpg" ;
		Config.ACCESS_KEY = "";
		Config.SECRET_KEY = "";
		String bucketName = "bucket";
		String key = "localread2012070.txt";

		AuthPolicy policy = new AuthPolicy("bucket", 3600);
		String token = policy.makeAuthTokenString();
		UpTokenClient upTokenClient = new UpTokenClient(token);
		UpService upClient = new UpService(upTokenClient);
		
		Map<String, Object> optParams = new HashMap<String, Object>() ;
		optParams.put("mimeType", "") ;
		optParams.put("callbakParam", "") ;
		optParams.put("progressFile", "") ;
		optParams.put("customMeta", "") ;
		PutFileRet putFileRet = UpClient.resumablePutFile(upClient, bucketName, key, localFile, optParams) ;
		if (putFileRet.ok()) {
			System.out.println("|--> successfully upload a file : " + putFileRet.getHash());
		} else {
			System.out.println("|--> Fail to upload a file : " + putFileRet);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	private Handler myMessageHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPLOAD_CONTINUE_NOTIFIER: // the file is uploading
				if (!Thread.currentThread().isInterrupted()) {
					progressBar.setProgress((int) current);
					setProgress((int) current);
					setSecondaryProgress((int) current);
				}
				break;
			case UPLOAD_FINISED_NOTIFIER: // uploading completed
				//progressBar.setVisibility(View.GONE);
				progressBar.setProgress((int) fsize);
				setProgress((int) fsize);
				setSecondaryProgress((int) fsize);
				Thread.currentThread().interrupt();
				break;
			default:
				break;
			}
		}
	};
}
