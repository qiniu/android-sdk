# Qiniu Resource Storage SDK for Android

[![@qiniu on weibo](http://img.shields.io/badge/weibo-%40qiniutek-blue.svg)](http://weibo.com/qiniutek)
[![Software License](https://img.shields.io/badge/license-MIT-brightgreen.svg)](LICENSE.md)
[![Build Status](https://github.com/qiniu/android-sdk/workflows/Run%20Test%20Cases/badge.svg)](https://github.com/qiniu/android-sdk/actions)
[![codecov](https://codecov.io/gh/qiniu/android-sdk/branch/master/graph/badge.svg)](https://codecov.io/gh/qiniu/android-sdk)
[![Latest Stable Version](http://img.shields.io/maven-central/v/com.qiniu/qiniu-android-sdk.svg)](https://search.maven.org/search?q=a:qiniu-android-sdk)

## 演示代码
https://github.com/qiniudemo/qiniu-lab-android

## 安装

### 运行环境

| Qiniu SDK 版本 | 最低 Android版本   |       依赖库版本           |
|------------ |-----------------|------------------------|
|  8.5.2        |  Android 4.0+     |        okhttp 4+         |
|  8.5.1        |  Android 4.0+     |        okhttp 4+         |
|  8.5.0        |  Android 4.0+     |        okhttp 4+         |
|  8.4.*        |  Android 4.0+     |        okhttp 4+         |
|  8.3.2        |  Android 4.0+     |        okhttp 4+         |
|  8.3.1        |  Android 4.0+     |        okhttp 4+         |
|  8.3.0        |  Android 5.0+     |        okhttp 4+         |
|  8.2.x        |  Android 5.0+     |        okhttp 4+         |
|  8.1.x        |  Android 5.0+     |        okhttp 4+         |
|  8.0.x        |  Android 5.0+     |        okhttp 4+         |
|  7.7.x        |  Android 5.0+     |        okhttp 4+         |
|  7.6.x        |  Android 5.0+     |        okhttp 4+         |
|  7.5.x        |  Android 5.0+     |        okhttp 4+         |
|  7.4.6        |  Android 4.0+     |        okhttp 3.12.6     |
|  7.3.x        |  Android 2.3+     |        okhttp 3.11.0     |
|  7.2.x        |  Android 2.3+     |        okhttp 3+         |
|  7.1.x        |  Android 2.3+     |        okhttp 2.6+       |
| 7.0.8,7.0.9   |  Android 2.2+     | android-async-http 1.4.9 |
|  7.0.7        |  Android 2.2+     | android-async-http 1.4.8 |

### 注意
* 推荐使用最新版：8.5.2
* 7.6.2 ~ 8.3.2 AndroidNetwork.getMobileDbm()可以获取手机信号强度，需要如下权限(API>=18时生效)
```
  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```
* 从7.5.0开始增加了DNS预取和缓存策略，减少dns解析错误
* 如果可以明确 区域 的话，最好指定固定区域，这样可以少一步网络请求，少一步出错的可能。
* 如果使用 Android 4.x ，对应 okhttp 版本请调整至 3.12.+

### 直接安装
将sdk jar文件 复制到项目中去，[jar包下载地址](http://search.maven.org/#search%7Cga%7C1%7Ccom%2Fqiniu%2Fqiniu-android-sdk) , 下载对应的jar包，以及搜索下载对应的依赖库

[happy-dns下载地址](https://repo1.maven.org/maven2/com/qiniu/happy-dns/)

### 通过maven
* Android Studio中添加dependencies 或者 在项目中添加maven依赖
```
// 1. 直接导入
implementation 'com.qiniu:qiniu-android-sdk:8.5.+'

// 2. 如果要修改okhttp依赖的版本，可采用以下方式（强烈建议使用七牛库依赖的okhttp版本）
implementation ('com.qiniu:qiniu-android-sdk:8.5.+'){
    exclude (group: 'com.squareup.okhttp3', module: 'okhttp')
}
implementation 'com.squareup.okhttp3:okhttp:4.9.1'

```
* 如果是eclipse, 也可以直接添加依赖来处理。

## 使用方法
UploadManager 可以创建一次，一直使用。
7.6.2 ~ 8.3.2 会调用AndroidNetwork.getMobileDbm可以获取网络信号强度
需要Manifest.permission.ACCESS_FINE_LOCATION和Manifest.permission.ACCESS_COARSE_LOCATION权限
```java
import com.qiniu.android.storage.UploadManager;
...
String token = "从服务端SDK获取";
UploadManager uploadManager = new UploadManager();
uploadManager.put("Hello, World!".getBytes(), "hello", token,
new UpCompletionHandler() {
    @Override
    public void complete(String key, ResponseInfo info, JSONObject response) {
        LogUtil.i(info);
    }
}, null);
...
```

### 支持使用 HTTP/3 协议发起请求

导入 [HTTP/3 client 插件](https://github.com/qiniu/qiniu-android-curl-plugin)，http3 client 插件依赖于 Android SDK v8.5.0及以上版本

安装导入
```java
// 移除 qiniu-android-sdk 依赖 : implementation 'com.qiniu:qiniu-android-sdk:x.x.+' 
implementation 'com.qiniu:qiniu-android-curl-plugin:1.0.0'
```

使用 CurlClient
```java
import com.qiniu.client.curl.CurlClient;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UploadManager;

// @param caPath: SSL 证书本地路径；如果想自定义 CA 可设置此选项，此处为 CA 文件的本地路径。
// 				  如果未定义（caPath 配置 null）则使用 SDK 内部提供的 CA 证书，证书来源：https://curl.se/ca/cacert.pem
CurlClient client = new CurlClient(caPath);
Configuration config = new Configuration.Builder()
                .requestClient(client)
                .build();
UploadManager manager = new UploadManager(config);
```


## 测试

``` bash
$ ./gradlew connectedAndroidTest
```

## 常见问题


1).有关Android Studio以及Eclipse安装运行Android Demo步骤，这里以Android Studio为实例:
1.修改 build.gradle
双击打开您的工程目录下的build.gradle，在dependencies中添加一条依赖compile 'com.qiniu:qiniu-android-sdk:7.2.+'，如下所示：
```
dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])
    testCompile 'junit:junit:4.12'
    compile 'com.android.support:appcompat-v7:23.1.1'
    compile 'com.qiniu:qiniu-android-sdk:7.6.+'
}
```
当然也可以将jar包下载到本地导入到项目中

2.添加相关权限，在 app/src/main 目录中的 AndroidManifest.xml 中增加如下 uses-permission 声明
```
 <uses-permission android:name="android.permission.INTERNET"/>
 <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

3.布局，在res/layout/activity_main.xml添加相应的上传的按钮以及相关控件，以下以一个简单的布局为例:
```
<TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_centerHorizontal="true"
        android:textSize="20sp"
        android:layout_marginTop="10dp"
        android:text="七牛云存储 SDK"
        android:id="@+id/textView"
        android:layout_alignParentTop="true"
        />

    <Button
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="上传"
        android:id="@+id/button"
        android:layout_centerHorizontal="true"
        android:layout_centerVertical="true"/>
```
效果如下:
![这里写图片描述](http://7xl4cb.com1.z0.glb.clouddn.com/QQ20160606-1.png)

4.逻辑代码, 为按钮添加上传事件，这里以上传一个byte数组为例，另外，上传的数据可以是文件路径或者文件，以下给出具体的代码:
```
package com.dxy.cloud.myapplication;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import org.json.JSONObject;

public class MainActivity extends Activity implements View.OnClickListener {

    //指定upToken, 强烈建议从服务端提供get请求获取, 这里为了掩饰直接指定key
    public static String uptoken = "xxxxx:xxxxx:xxxxx";
    private Button btnUpload;
    private TextView textView;
    private UploadManager uploadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.textView);
        btnUpload = (Button) findViewById(R.id.button);
        btnUpload.setOnClickListener(this);
        //new一个uploadManager类
        uploadManager = new UploadManager();

    }

    @Override
    public void onClick(View v) {
        LogUtil.i("starting......");
        byte[] data=new byte[]{ 0, 1, 2, 3};
        //设置上传后文件的key
        String upkey = "uploadtest.txt";
        uploadManager.put(data, upkey, uptoken, new UpCompletionHandler() {
            public void complete(String key, ResponseInfo rinfo, JSONObject response) {
                btnUpload.setVisibility(View.INVISIBLE);
                String s = key + ", " + rinfo + ", " + response;
                LogUtil.i(s);
                textView.setTextSize(10);
                String o = textView.getText() + "\r\n\r\n";
                //显示上传后文件的url
                textView.setText(o + s + "\n" + "http://xm540.com1.z0.glb.clouddn.com/" + key);
            }
        }, new UploadOptions(null, "test-type", true, null, null));

    }
}

```
运行的效果图如下:
![这里写图片描述](http://7xl4cb.com1.z0.glb.clouddn.com/QQ20160606-0.png)

**2).有关断点续传，暂停上传，设置自定义变量的用法，这里以一个简单的Demo给出下实现的方法，这里的Demo主要实现从相册选择一张图片上传来说明。**

可以参考放GitHub(Android Studio)上的源码: https://github.com/clouddxy/AndroidDemo
这里直接给出MainActivity中的代码:
```
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
            LogUtil.d(f.getAbsolutePath().toString());
            dirPath = f.getParent();
            //设置记录断点的文件的路径
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
                LogUtil.d(path);
                File f = new File(dirPath1, UrlSafeBase64.encodeToString(path));
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(f));
                    String tempString = null;
                    int line = 1;
                    try {
                        while ((tempString = reader.readLine()) != null) {
//							System.out.println("line " + line + ": " + tempString);
                            LogUtil.d("line " + line + ": " + tempString);
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
            LogUtil.d(picturePath);
            cursor.close();

            imageview.setVisibility(View.VISIBLE);
            imageview.setImageBitmap(BitmapFactory.decodeFile(picturePath));

            //自定义参数returnbody
            //"returnBody":"{\"key\":$(key),\"hash\":$(etag),\"fname\":$(fname),\"phone\":$(x:phone)}
            final String token = "xxxxx";
            button2.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

					//设定需要添加的自定义变量为Map<String, String>类型 并且放到UploadOptions第一个参数里面
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("x:phone", "12345678");

                    LogUtil.d("click upload");
                    isCancelled = false;
                    uploadManager.put(picturePath, null, token,
                            new UpCompletionHandler() {
                                public void complete(String key,
                                                     ResponseInfo info, JSONObject res) {
                                    LogUtil.i(key + ",\r\n " + info
                                            + ",\r\n " + res);

                                    if(info.isOK()==true){
                                        textview.setText(res.toString());
                                    }
                                }
                            }, new UploadOptions(map, null, false,
                                    new UpProgressHandler() {
                                        public void progress(String key, double percent){
                                            LogUtil.i(key + ": " + percent);
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
```
上传过程进度条:
![这里写图片描述](http://7xrnxn.com2.z0.glb.qiniucdn.com/QQ20160402-1.png)
上传后自定义变量回调：
![这里写图片描述](http://7xrnxn.com2.z0.glb.qiniucdn.com/QQ20160402-3.png)

**3).关于从服务器获取Token上传:**
Android SDK为客户端SDK，没有包含token生成实现，为了安全，token都建议通过网络从服务端获取，具体生成代码可以参考"java/python/php/ruby/go"等服务端sdk, 其实也比较简单，可以使用okhttp发送一个简单的get请求就可以了，这里以使用SyncHttpClient为例来说明:
```
package com.example.androidupload;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.loopj.android.http.SyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.UrlSafeBase64;
import org.apache.http.Header;
import org.json.JSONObject;
import java.io.File;
import java.util.UUID;

public class MainActivity extends Activity {

    private UploadManager uploadManager;
    private final String tag = "MainActivity";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //设置本地上传的文件的路径
        String path ="/storage/emulated/0/Download/DSC_0770.JPG";
        String aaa = UrlSafeBase64.encodeToString(path);
        File file =new File(path);
    }

    private void upload(final String localPath,final String key)
    {
        Thread thread = new Thread()
        {
            @Override
            public void run() {
                super.run();
                SyncHttpClient client = new SyncHttpClient();
                //获取服务端的token的url
                String url = "http://xxxx/xxxx/xxx";
                client.get(url, new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int i, Header[] headers, String s, Throwable throwable) {
                    }
                    @Override
                    public void onSuccess(int i, Header[] headers, String s) {
                        LogUtil.i("请求七牛token："+s);
                        JsonParser jsonParser = new JsonParser();
                        JsonElement jsonElement =jsonParser.parse(s);
                        String token = jsonElement.getAsJsonObject().get("uptoken").toString();
                        LogUtil.i("七牛开始上传"+localPath+"\n"+key+"\n"+token);
                        if(uploadManager==null)
                        {
                            uploadManager = new UploadManager();
                        }
                        UploadOptions uploadOptions = new UploadOptions(null, null, false,
                                new UpProgressHandler() {
                                    @Override
                                    public void progress(String key, double percent) {
                                        LogUtil.i("a 七牛上传progress:"+percent+"\n"+key);
                                    }
                                }, null);

                        //调用uploadManager上传
                        uploadManager.put(localPath, key, token, new UpCompletionHandler() {
                            @Override
                            public void complete(String key, com.qiniu.android.http.ResponseInfo info, JSONObject response) {
                                LogUtil.i("a 七牛上传complete:"+key + ",\r\n " + info + ",\r\n " + response);
                            }
                        },uploadOptions);
                    }
                });
            }
        };
        thread.start();
    }
}
```

**4).关于for循环上传:**
可以参考demo:
```
package com.dxy.cloud.myapplication;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import org.json.JSONObject;

public class MainActivity extends Activity implements View.OnClickListener {

    //指定upToken, 强烈建议从服务端提供get请求获取, 这里为了掩饰直接指定key
    public static String uptoken = "xxxxxxxxx:xxxxxxx:xxxxxxxxxx";
    private Button btnUpload;
    private TextView textView;
    private UploadManager uploadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.textView);
        btnUpload = (Button) findViewById(R.id.button);
        btnUpload.setOnClickListener(this);
        //new一个uploadManager类
        uploadManager = new UploadManager();
    }

    @Override
    public void onClick(View v) {
        LogUtil.i("starting......");
        byte[] data=new byte[]{ 0, 1, 2, 3, 3, 4, 5, 6,0, 1, 2, 3, 4, 5, 6,0, 1, 2, 3, 4, 5, 6,0, 1, 2, 3, 4, 5, 6,0, 1, 2, 3, 4, 5, 6,0, 1, 2, 3,};

       for(int i=0;i<data.length;i++){
        	String expectKey =UUID.randomUUID().toString();
            uploadManager.put(data, expectKey, uptoken, new UpCompletionHandler() {
                public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                    String s = k + ", "+ rinfo + ", " + response;
                    LogUtil.i(s);
                    String key = getKey(k, response);
                    String o = hint.getText() + "\r\n\r\n";
                    hint.setText(o + s + "http://xm540.com1.z0.glb.clouddn.com/" + key);
                }
            }, new UploadOptions(null, "test-type", true, null, null));
        }
    }
}
```
**5).常见的返回的状态码:**
使用七牛 Android sdk 的时候， 如果使用方式不正确，会返回一些对应的错误码。
具体可以参考源码：
https://github.com/qiniu/android-sdk/blob/c4cd1437aa1f2a0d68122e46b83580facdf1b74a/library/src/main/java/com/qiniu/android/http/ResponseInfo.java
```
public static final int MaliciousResponseError = -8;
public static final int LocalIOError = -7;
public static final int ZeroSizeFile = -6;
public static final int InvalidToken = -5;
public static final int InvalidArgument = -4;
public static final int InvalidFile = -3;
public static final int Cancelled = -2;
public static final int NetworkError = -1;
public static final int TimedOut = -1001;
public static final int UnknownHost = -1003;
public static final int CannotConnectToHost = -1004;
public static final int NetworkConnectionLost = -1005;
```

**6).关于混淆配置:**

混淆处理 对七牛的SDK不需要做特殊混淆处理，混淆时将七牛相关的包都排除就可以了。

在Android Studio中，混淆配置在 proguard-rules.pro 里面加上下面几行混淆代码就行：
```
-keep class com.qiniu.**{*;}
-keep class com.qiniu.**{public <init>();}
-ignorewarnings
```
注意：-ignorewarnings这个也是必须加的，如果不加这个编译的时候可能是可以通过的，但是release的时候还是会出现错误。

对于Eeclipse中也是一样的，在proguard-project.txt 文件附件同样的排除代码就可以了
```
-keep class com.qiniu.**{*;}
-keep class com.qiniu.**{public <init>();}
-ignorewarnings
```

**7).为什么进度会在95% 停很久:**
因为上传进度是用sdk写入socket的 字节数/总字节数 作为进度，但写入socket不等于服务器收到并且处理完成，中间还有一段时间，如果只是用字节数就会出现更怪异的情况，在100% 停留很久，所以综合考虑就使用了 95%这个值.

**8).如何对文件名字做模糊查询:**
如果前缀查询满足不了需求，想做多维度的查询，比如日期，userid, 文件名中的字段进行查询，可以通过开通Pandora 日志查询服务来满足。下面的例子里面需要在工作流上创建key, user, etag, mime, 四个字段的repo, 具体客户端做法如下，在上传结束后，再把结果打点到pandora 服务
```java
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.bigdata.pipeline.Pipeline;
...
    String token = "从服务端SDK获取";
    UploadManager uploadManager = new UploadManager();
    String pipelineToken = "从服务端获取 或者 生成一个长时间的token";
    Pipeline pipe = new Pipeline();
    uploadManager.put("Hello, World!".getBytes(), "hello", token,
    new UpCompletionHandler() {
        @Override
        public void complete(String key, ResponseInfo info, JSONObject response) {
            Map<String, Object> inf = new HashMap<String, Object>();
            inf.put("key", key);
            inf.put("user", "userid");
            inf.put("etag", resp.getString("hash"));
            inf.put("mime", resp.getString("mimeType"));
            pipe.pump("keysearchRepo", inf, pipelineToken, new Pipeline.PumpCompleteHandler() {
                @Override
                public void complete(ResponseInfo inf2) {
                    ...
                }
            });
        }
    }, null);
...
```

**9).如何理解上传返回 code :**
详情 [code 注释说明](https://github.com/qiniu/android-sdk/blob/master/library/src/main/java/com/qiniu/android/http/ResponseInfo.java)

## 代码贡献

详情参考[代码提交指南](https://github.com/qiniu/android-sdk/blob/master/CONTRIBUTING.md)。

## 贡献记录

- [所有贡献者](https://github.com/qiniu/android-sdk/contributors)

## 联系我们

- 如果需要帮助，请提交工单（在portal右侧点击咨询和建议提交工单，或者直接向 support@qiniu.com 发送邮件）
- 如果有什么问题，可以到问答社区提问，[问答社区](http://qiniu.segmentfault.com/)
- 更详细的文档，见[官方文档站](http://developer.qiniu.com/)
- 如果发现了bug， 欢迎提交 [issue](https://github.com/qiniu/android-sdk/issues)
- 如果有功能需求，欢迎提交 [issue](https://github.com/qiniu/android-sdk/issues)
- 如果要提交代码，欢迎提交 pull request
- 欢迎关注我们的[微信](http://www.qiniu.com/#weixin) [微博](http://weibo.com/qiniutek)，及时获取动态信息。


## 代码许可

The MIT License (MIT).详情见 [License文件](https://github.com/qiniu/android-sdk/blob/master/LICENSE).
