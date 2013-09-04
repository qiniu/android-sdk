package com.qiniu.demo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.qiniu.R;
import com.qiniu.auth.JSONObjectRet;
import com.qiniu.resumableio.PutExtra;
import com.qiniu.resumableio.ResumableIO;
import org.json.JSONObject;

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
		// doResumableUpload(Uri.parse("content://media/external/images/media/13"));
		doResumableUpload(Uri.parse("file:///sdcard/update.bin"));
		// doResumableUpload(Uri.parse("file:///sdcard/googlechrome.dmg"));
		// doResumableUpload(Uri.parse("file:///sdcard/py.pdf"));
		// doResumableUpload(Uri.parse("file:///sdcard/mongo.pdf"));
		// doResumableUpload(Uri.parse("file:///sdcard/cocoa.pdf"));
		// doResumableUpload(Uri.parse("file:///sdcard/iPadHIG.pdf"));
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
			return;
		}
	}

	public void doResumableUpload(Uri uri) {
		String key = null;
		String token = "tGf47MBl1LyT9uaNv-NZV4XZe7sKxOIa9RE2Lp8B:8xaK55po0nAkO3rADngxgPUi1XU=:eyJzY29wZSI6InNkayIsImRlYWRsaW5lIjoxMzc4NTM5MjgwfQ==";
		PutExtra extra = new PutExtra();
		ResumableIO.putFile(this, token, key, uri, extra, new JSONObjectRet() {
			@Override
			public void onSuccess(JSONObject obj) {
				String a = "pQkrBWWH2bvFMZ-sj9_StPy-Qhf7";
				String b = obj.optString("key", "");
				if (b != a) {
					String j = b;
					j += "df";
				}
				hint.setText(b);
			}

			@Override
			public void onProcess(long current, long total) {
				int percent = (int) (current*100/total);
				hint.setText(percent + "");
				pb.setProgress(percent);
			}

			@Override
			public void onPause(Object tag) {
				super.onPause(tag);
			}

			@Override
			public void onFailure(Exception ex) {
				hint.setText(ex.getMessage());
				ex = null;
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) return;
		doResumableUpload(data.getData());
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
