# Qiniu Android SDK

[![Build Status](https://travis-ci.org/qiniu/android-sdk.svg?branch=master)](https://travis-ci.org/qiniu/android-sdk)

## 安装

在Gradle构建脚本中增加Maven依赖项

```gradle
dependencies {
  compile 'com.qiniu:android-sdk:7.0.0'
}
```

## 使用方法

```java
import com.qiniu.storage.UploadManager;
...
    String token = "从服务端SDK获取";
    UploadManager uploadManager = new UploadManager();
    uploadManager.put("Hello, World!".getBytes(), "hello", token,
    new UpCompletionHandler() {
        @Override
        public void complete(String key, ResponseInfo info, JSONObject response) {
            Log.i("qiniu", info);
        }
    }, null);
...
```


## 测试

``` bash
$ ./gradlew connectedAndroidTest
```


## 代码贡献

详情参考[代码提交指南](https://github.com/qiniu/android-sdk/blob/master/CONTRIBUTING.md)。

## 贡献记录

- [所有贡献者](https://github.com/qiniu/android-sdk/contributors)


## 代码许可

The MIT License (MIT).详情见 [License文件](https://github.com/qiniu/android-sdk/blob/master/LICENSE).
