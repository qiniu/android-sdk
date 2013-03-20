package com.qiniu.demo;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.qiniu.R;
import com.qiniu.up.PutFileRet;
import com.qiniu.up.Up;
import com.qiniu.up.UpOption;
import com.qiniu.utils.Utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MyActivity extends Activity implements View.OnClickListener{

	public static final int PICK_PICTURE = 0;

	// 在七牛绑定的对应bucket的域名. 可以到这里绑定 https://dev.qiniutek.com/buckets
	public static String Domain = "http://cheneya.qiniudn.com/";
	public static String BucketName = "a";

	// upToken 这里需要自行获取. SDK 将不实现获取过程.
	public static final String UP_TOKEN = "tGf47MBl1LyT9uaNv-NZV4XZe7sKxOIa9RE2Lp8B:Zn0uzHfVosbE7duq0VcNTyCy3-M=:eyJzY29wZSI6ImEiLCJkZWFkbGluZSI6MTM2Mzc2MzY4MCwiZGV0ZWN0TWltZSI6MH0=";

	private Button btnUpload;
	private EditText editKey;

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
		editKey.setText("mykey1");
	}

	/**
	 * 执行上传
	 * @param uri
	 */
	private void doUpload(Uri uri) {
		final String key = editKey.getText().toString();
		Up up = new Up(UP_TOKEN);
		UpOption opts = new UpOption();
		opts.EntryUri = BucketName + ":" + key;
		opts.MimeType = "image/png";
		opts.Rotate = 2;
		up.PutFile(this, uri, null, opts, new PutFileRet() {
			@Override
			public void onSuccess(String hash) {
				toast("上传成功! 正在跳转到浏览器查看效果 \nhash:" + hash);
				String redirect = Domain + key;
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
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICK_PICTURE) {
			if (resultCode != RESULT_OK) return;

			doUpload(data.getData());
			return;
		}

	}

	private void toast(String str) {
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
	}
}
