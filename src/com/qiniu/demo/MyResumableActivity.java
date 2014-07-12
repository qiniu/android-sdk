package com.qiniu.demo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import com.qiniu.R;
import com.qiniu.auth.JSONObjectRet;
import com.qiniu.resumableio.PutExtra;
import com.qiniu.resumableio.ResumableIO;
import com.qiniu.utils.QiniuException;

public class MyResumableActivity extends Activity implements View.OnClickListener {
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
			if (uploadUri == null) {
				Toast.makeText(this, "还没开始任务", 20).show();
				return;
			}
			if (taskId >= 0) {
				ResumableIO.stop(taskId);
				stop.setText("开始");
				hint.setText("暂停");
				taskId = -1;
				return;
			}
			stop.setText("暂停");
			hint.setText("连接中");
			doResumableUpload(uploadUri, mExtra);
			return;
		}
	}
	int taskId = -1;
	Uri uploadUri;
	PutExtra mExtra;

	public void doResumableUpload(final Uri uri, PutExtra extra) {
		hint.setText("连接中");
		String key = null;
		String token = "<token>";
		extra.params = new HashMap<String, String>();
		extra.params.put("x:a", "bb");
		taskId = ResumableIO.putFile(this, token, key, uri, extra, new JSONObjectRet() {
			@Override
			public void onSuccess(JSONObject obj) {
				hint.setText("上传成功: " + obj.optString("key", ""));
			}

			@Override
			public void onProcess(long current, long total) {
				float percent = (float) (current*10000/total) / 100;
				hint.setText("上传中: " + percent + "%");
				pb.setProgress((int) percent);
			}

			@Override
			public void onPause(Object tag) {
				uploadUri = uri;
				mExtra = (PutExtra) tag;
			}

			@Override
			public void onFailure(QiniuException ex) {
				hint.setText(ex.getMessage());
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) return;
		PutExtra e = new PutExtra();
		e.notify = new PutExtra.INotify() {
			@Override
			public void onSuccessUpload(PutExtra ex) {
				if (ex.isFinishAll()) return;
				JSONObject json;
				try {
					json = ex.toJSON();
				} catch (JSONException e1) {
					e1.printStackTrace();
					return;
				}
				// store to disk
				// restore PutExtra by new PutExtra(JSONObject);
			}
		};
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
}
