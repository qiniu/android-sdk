package com.qiniu.qbox.demo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

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
		
		// Notice here : Please apply your access/secret keys here.
		Config.ACCESS_KEY = "ttXNqIvhrYu04B_dWM6GwSpcXOZJvGoYFdznAWnz";
		Config.SECRET_KEY = "rX-7Omdag0BIBEtOyuGQXzx4pmTUTeLxoPEw6G8d";
		// 处理事件
		this.chooseButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread(new Runnable() { 
					RandomAccessFile f = null ;
					@SuppressLint("SdCardPath")
					public void run() {
						try {
							// Notice here : make sure you have a bucket called "bucket"
							// you can change it.
							String bucketName = "bucket";
							String key = "localredad2012070-3.jpg";

							AuthPolicy policy = new AuthPolicy("bucket", 3600);
							String token = policy.makeAuthTokenString();
							UpTokenClient upTokenClient = new UpTokenClient(token);
							UpService upClient = new UpService(upTokenClient);
							
							// Notice here : the upload file. 
							// you should make sure that the file is exist!
							f = new RandomAccessFile("/mnt/sdcard/mm.jpg", "r");
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
							// The download url.
							System.out.println("  url : " + getRet.getUrl());
							
						} catch (FileNotFoundException e1) {
							e1.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							if (f != null) {
								try {
									f.close() ;
								} catch (IOException e) {
									e.printStackTrace();
								}
								f = null ;
							}
						}
						
					};
				}).start();
			}
		});
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
