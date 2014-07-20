package com.qiniu.demo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.HashMap;

import com.qiniu.R;
import com.qiniu.auth.JSONObjectRet;
import com.qiniu.io.IO;
import com.qiniu.io.PutExtra;
import com.qiniu.utils.QiniuException;

public class MyActivity extends Activity implements View.OnClickListener{

	public static final int PICK_PICTURE_RESUMABLE = 0;

	// @gist upload_arg
	// 在七牛绑定的对应bucket的域名. 默认是bucket.qiniudn.com
	public static String bucketName = "<bucketName>";
	public static String domain = bucketName + ".qiniudn.com";
	// upToken 这里需要自行获取. SDK 将不实现获取过程. 当token过期后才再获取一遍
	public String uptoken = "<token>";
	// @endgist

	private Button btnUpload;
	private Button btnResumableUpload;
	private TextView hint;

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
		hint = (TextView) findViewById(R.id.textView1);
		btnUpload = (Button) findViewById(R.id.button1);
		btnUpload.setOnClickListener(this);
		btnResumableUpload = (Button) findViewById(R.id.button);
		btnResumableUpload.setOnClickListener(this);
	}

	// @gist upload
	boolean uploading = false;
	/**
	 * 普通上传文件
	 * @param uri
	 */
	private void doUpload(Uri uri) {
		if (uploading) {
			hint.setText("上传中，请稍后");
			return;
		}
		uploading = true;
		String key = IO.UNDEFINED_KEY; // 自动生成key
		PutExtra extra = new PutExtra();
		extra.params = new HashMap<String, String>();
		extra.params.put("x:a", "测试中文信息");
		hint.setText("上传中");
		IO.putFile(this, uptoken, key, uri, extra, new JSONObjectRet() {
			@Override
			public void onProcess(long current, long total) {
				hint.setText(current + "/" + total);
			}

			@Override
			public void onSuccess(JSONObject resp) {
				uploading = false;
				String hash = resp.optString("hash", "");
				String value = resp.optString("x:a", "");
				String redirect = "http://" + domain + "/" + hash;
				hint.setText("上传成功! " + hash);
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(redirect));
				startActivity(intent);
			}

			@Override
			public void onFailure(QiniuException ex) {
				uploading = false;
				hint.setText("错误: " + ex.getMessage());
			}
		});
	}
	// @endgist

	@Override
	public void onClick(View view) {
		if (view.equals(btnUpload)) {
			Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(i, PICK_PICTURE_RESUMABLE);
			return;
		}
		if (view.equals(btnResumableUpload)) {
			startActivity(new Intent(this, MyResumableActivity.class));
			return;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) return;
		if (requestCode == PICK_PICTURE_RESUMABLE) {
			doUpload(data.getData());
			return;
		}
	}
}
