package com.qiniu.demo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.qiniu.R;
import com.qiniu.auth.JSONObjectRet;
import com.qiniu.io.IO;
import com.qiniu.io.PutExtra;
import com.qiniu.resumable.BlkputRet;
import com.qiniu.resumable.ResumableIO;
import com.qiniu.resumable.RputExtra;
import com.qiniu.resumable.RputNotify;
import com.qiniu.utils.Utils;
import org.json.JSONObject;

public class MyActivity extends Activity implements View.OnClickListener{

	public static final int PICK_PICTURE = 0;
	public static final int PICK_PICTURE_RESUMABLE = 1;

	// 在七牛绑定的对应bucket的域名. 可以到这里绑定 https://dev.qiniutek.com/buckets
	public static String domain = "";
	public static String bucketName = "";

	// upToken 这里需要自行获取. SDK 将不实现获取过程.

	public static final String UP_TOKEN = "";

	private Button btnUpload;
	private Button btnResumableUpload;
	private EditText editKey;
	private ProgressBar progressBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		initWidget();
	}

	/**
	 * 初始化控件
	 */
	private void initWidget() {
		btnUpload = (Button) findViewById(R.id.button);
		btnUpload.setOnClickListener(this);

		editKey = (EditText) findViewById(R.id.editText);
		editKey.setText("android_sdk_demo");

		btnResumableUpload = (Button) findViewById(R.id.button1);
		btnResumableUpload.setOnClickListener(this);

		progressBar = (ProgressBar) findViewById(R.id.progressBar);
	}

	/**
	 * 执行上传
	 * @param uri
	 */
	private void doUpload(Uri uri) {
		final String key = editKey.getText().toString();
		PutExtra extra = new PutExtra(bucketName);
		extra.mimeType = "image/png";

		IO.putFile(this, UP_TOKEN, key, uri, extra, new JSONObjectRet() {
			@Override
			public void onSuccess(JSONObject resp) {
				String hash;
				try {
					hash = resp.getString("hash");
				} catch (Exception ex) {
					toast(ex.getMessage());
					return;
				}
				toast("上传成功! 正在跳转到浏览器查看效果 \nhash:" + hash);
				String redirect = domain + "/" + key;
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(redirect));
				startActivity(intent);
			}

			@Override
			public void onFailure(Exception ex) {
				toast("错误: " + ex.getMessage());
			}
		});
	}

	/**
	 * 断点许上传
	 * @param uri
	 */
	private void doResumableUpload(Uri uri) {
		final String key = editKey.getText().toString();
		RputExtra extra = new RputExtra(bucketName);
		extra.mimeType = "application/png";
		final long fsize = Utils.getSizeFromUri(this, uri);
		progressBar.setMax(100);
		progressBar.setProgress(0);
		extra.notify = new RputNotify() {
			long uploaded = 0;
			@Override
			public synchronized void onNotify(int blkIdx, int blkSize, BlkputRet ret) {
				uploaded += blkSize;
				int progress = (int) (uploaded * 100 / fsize);
				progressBar.setProgress(progress);
			}
		};


		ResumableIO.putFile(this, UP_TOKEN, key, uri, extra, new JSONObjectRet() {
			@Override
			public void onSuccess(JSONObject resp) {
				String hash;
				try {
					hash = resp.getString("hash");
				} catch (Exception ex) {
					toast(ex.getMessage());
					return;
				}
				toast("上传成功! 正在跳转到浏览器查看效果 \nhash:" + hash);
				String redirect = domain + "/" + key;
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(redirect));
				startActivity(intent);
			}

			@Override
			public void onFailure(Exception ex) {
				toast("错误: " + ex.getMessage());
			}
		});
	}

	@Override
	public void onClick(View view) {
		if (view.equals(btnUpload)) {
			Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(i, PICK_PICTURE);
			return;
		}

		if (view.equals(btnResumableUpload)) {
			Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(i, PICK_PICTURE_RESUMABLE);
			return;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) return;

		if (requestCode == PICK_PICTURE) {
			doUpload(data.getData());
			return;
		}

		if (requestCode == PICK_PICTURE_RESUMABLE) {
			doResumableUpload(data.getData());
			return;
		}
	}

	private void toast(String str) {
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
	}
}
