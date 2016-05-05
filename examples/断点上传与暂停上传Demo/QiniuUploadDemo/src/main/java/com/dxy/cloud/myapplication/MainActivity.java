package com.dxy.cloud.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.KeyGenerator;
import com.qiniu.android.storage.Recorder;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.storage.persistent.FileRecorder;
import com.qiniu.android.utils.UrlSafeBase64;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class MainActivity extends Activity {
    private Button button1;
    private Button button2;
    private Button button3;
    private ImageView imageview;
    private Uri imageUri;
    private TextView textview;
    private ProgressBar progressbar;
    public static final int RESULT_LOAD_IMAGE = 1;
    private volatile boolean isCancelled = false;

    UploadManager uploadManager;


    public MainActivity() {
        //断点上传
        String dirPath = "/storage/emulated/0/Download";
        Recorder recorder = null;
        try{
            File f = File.createTempFile("qiniu_xxxx", ".tmp");
            Log.d("qiniu", f.getAbsolutePath().toString());
            dirPath = f.getParent();
            recorder = new FileRecorder(dirPath);
        } catch(Exception e) {
            e.printStackTrace();
        }

        final String dirPath1 = dirPath;
        //默认使用 key 的url_safe_base64编码字符串作为断点记录文件的文件名。
        //避免记录文件冲突（特别是key指定为null时），也可自定义文件名(下方为默认实现)：
        KeyGenerator keyGen = new KeyGenerator(){
            public String gen(String key, File file){
                // 不必使用url_safe_base64转换，uploadManager内部会处理
                // 该返回值可替换为基于key、文件内容、上下文的其它信息生成的文件名
                String path = key + "_._" + new StringBuffer(file.getAbsolutePath()).reverse();
                Log.d("qiniu", path);
                File f = new File(dirPath1, UrlSafeBase64.encodeToString(path));
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(f));
                    String tempString = null;
                    int line = 1;
                    try {
                        while ((tempString = reader.readLine()) != null) {
//							System.out.println("line " + line + ": " + tempString);
                            Log.d("qiniu", "line " + line + ": " + tempString);
                            line++;
                        }

                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } finally {
                        try{
                            reader.close();
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }




                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return path;
            }
        };

        Configuration config = new Configuration.Builder()
                // recorder 分片上传时，已上传片记录器
                // keyGen 分片上传时，生成标识符，用于片记录器区分是那个文件的上传记录
                .recorder(recorder, keyGen)
                .build();
        // 实例化一个上传的实例
        uploadManager = new UploadManager(config);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button1 = (Button) findViewById(R.id.bt1);
        button2 = (Button) findViewById(R.id.bt2);
        button3 = (Button) findViewById(R.id.bt3);
        imageview = (ImageView) findViewById(R.id.iv);
        textview = (TextView) findViewById(R.id.tv);
        progressbar = (ProgressBar) findViewById(R.id.pb);

        // final String token = edittext.getText().toString();

        button1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK
                && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            final String picturePath = cursor.getString(columnIndex);
            Log.d("PICTUREPATH", picturePath);
            cursor.close();

            imageview.setVisibility(View.VISIBLE);
            imageview.setImageBitmap(BitmapFactory.decodeFile(picturePath));
            //永久使用的returnbody测试
//            final String token = "um6IEH7mtwnwkGpjImD08JdxlvViuELhI4mFfoeL:gXv-=:eyJzY29wZSI6ImFuZHJvaWRkZW1vIiwiZGVhZGxpbmUiOjIwMDk5MjE5MTIsInJldHVybkJvZHkiOiJ7IFwiaGFzaFwiOiQoZXRhZyksXCJrZXlcIjokKGtleSksXCJtaW1lVHlwZVwiOiQobWltZVR5cGUpLCBcImZuYW1lXCI6JChmbmFtZSl9In0=";

            //自定义参数returnbody
//            final String token = "um6IEH7mtwnwkGpjImD08JdxlvViuELhI4mFfoeL:CJy8mWhEBn5qxhZIyPAg9eHH4iA=:eyJzY29wZSI6ImphdmFkZW1vIiwicmV0dXJuQm9keSI6IntcImtleVwiOiQoa2V5KSxcImhhc2hcIjokKGV0YWcpLFwiZm5hbWVcIjokKGZuYW1lKSxcInBob25lXCI6JCh4OnBob25lKX0iLCJkZWFkbGluZSI6MTQ1OTg0NjQyOH0=";
            //自定义参数callbackbody
            final String token = "um6IEH7mtwnwkGpjImD08JdxlvViuELhI4mFfoeL:6LM7earlaBgxpLbRApzF_xOMCAk=:eyJjYWxsYmFja1VybCI6Imh0dHA6Ly9hM2VmMDc4YS5uZ3Jvay5pby9TZXJ2bGV0RGVtby9zZXJ2bGV0L0NhbGxiYWNrRGVtbyIsInNjb3BlIjoiamF2YWRlbW8iLCJjYWxsYmFja0JvZHkiOiJuYW1lXHUwMDNkJChmbmFtZSlcdTAwMjZoYXNoXHUwMDNkJChldGFnKVx1MDAyNmtleVx1MDAzZCQoa2V5KVx1MDAyNnBob25lXHUwMDNkJCh4OnBob25lKSIsImRlYWRsaW5lIjoxNDU5ODQ4NDEyfQ==";
            button2.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("x:phone", "12345678");

                    Log.d("qiniu", "click upload");
                    isCancelled = false;
                    uploadManager.put(picturePath, null, token,
                            new UpCompletionHandler() {
                                public void complete(String key,
                                                     ResponseInfo info, JSONObject res) {
                                    Log.i("qiniu", key + ",\r\n " + info
                                            + ",\r\n " + res);

                                    if(info.isOK()==true){
                                        textview.setText(res.toString());
                                    }
                                }
                            }, new UploadOptions(map, null, false,
                                    new UpProgressHandler() {
                                        public void progress(String key, double percent){
                                            Log.i("qiniu", key + ": " + percent);
                                            progressbar.setVisibility(View.VISIBLE);
                                            int progress = (int)(percent*1000);
//											Log.d("qiniu", progress+"");
                                            progressbar.setProgress(progress);
                                            if(progress==1000){
                                                progressbar.setVisibility(View.GONE);
                                            }
                                        }

                                    }, new UpCancellationSignal(){
                                @Override
                                public boolean isCancelled() {

                                    return isCancelled;
                                }
                            }));
                }
            });

            button3.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    isCancelled = true;
                }
            });


        }
    }
}
