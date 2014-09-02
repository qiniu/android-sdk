package com.qiniu.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;

import com.qiniu.R;
import com.qiniu.auth.Authorizer;
import com.qiniu.io.IO;
import com.qiniu.rs.CallBack;
import com.qiniu.rs.CallRet;
import com.qiniu.rs.PutExtra;
import com.qiniu.rs.UploadCallRet;

/**
 * 也可参考 UploadTest
 */
public class MyActivity extends Activity implements View.OnClickListener{

	public static final int PICK_PICTURE_RESUMABLE = 0;
	
	public static String uptoken = "anEC5u_72gw1kZPSy3Dsq1lo_DPXyvuPDaj4ePkN:zmaikrTu1lgLb8DTvKQbuFZ5ai0=:eyJzY29wZSI6ImFuZHJvaWRzZGsiLCJyZXR1cm5Cb2R5Ijoie1wiaGFzaFwiOlwiJChldGFnKVwiLFwia2V5XCI6XCIkKGtleSlcIixcImZuYW1lXCI6XCIgJChmbmFtZSkgXCIsXCJmc2l6ZVwiOlwiJChmc2l6ZSlcIixcIm1pbWVUeXBlXCI6XCIkKG1pbWVUeXBlKVwiLFwieDphXCI6XCIkKHg6YSlcIn0iLCJkZWFkbGluZSI6MTQ2NjIyMjcwMX0=";
	// upToken 这里需要自行获取. SDK 将不实现获取过程. 隔一段时间到业务服务器重新获取一次
	public static Authorizer auth = new Authorizer();
	{
		auth.setUploadToken(uptoken); 
	}

	// 在七牛绑定的对应bucket的域名. 更换 uptoken 时同时更换为对应的空间名，
	public static String bucketName = "androidsdk";

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
	volatile boolean uploading = false;
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
		// 返回 UploadTaskExecutor ，可执行cancel，见 MyResumableActivity
		Context context = this.getApplicationContext();
		IO.putFile(context, auth, key, uri, extra, new CallBack() {
			@Override
			public void onProcess(long current, long total) {
				int percent = (int)(current*100/total);
				hint.setText("上传中: " + current + "/" + total + "  " + current/1024 + "K/" + total/1024 + "K; " + percent + "%");
			}

			@Override
			public void onSuccess(UploadCallRet ret) {
				uploading = false;
				String key = ret.getKey();
				String redirect = "http://" + bucketName + ".qiniudn.com/" + key;
				String redirect2 ="http://" + bucketName + ".u.qiniudn.com/" + key;
				hint.setText("上传成功! ret: " + ret.toString() + "  \r\n可到" + redirect + " 或  " + redirect2 + " 访问");
			}

			@Override
			public void onFailure(CallRet ret) {
				uploading = false;
				hint.setText("错误: " + ret.toString());
			}
		});
	}
	// @endgist

	@Override
	public void onClick(View view) {
		if (view.equals(btnUpload)) {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("*/*");
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			startActivityForResult(intent, PICK_PICTURE_RESUMABLE);
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
