# Qiniu Resource Storage SDK for Android

[![@qiniu on weibo](http://img.shields.io/badge/weibo-%40qiniutek-blue.svg)](http://weibo.com/qiniutek)
[![Software License](https://img.shields.io/badge/license-MIT-brightgreen.svg)](LICENSE.md)
[![Build Status](https://travis-ci.org/qiniu/android-sdk.svg?branch=master)](https://travis-ci.org/qiniu/android-sdk)
[![Latest Stable Version](http://img.shields.io/maven-central/v/com.qiniu/qiniu-android-sdk.svg)](https://github.com/qiniu/android-sdk/releases)


## 安装

### 直接安装
将realease 目录中的jar 复制到项目中去，此版本sdk依赖 http://loopj.com/android-async-http/ 1.4.6及以上版本

### 通过maven
* 如果在Adroid Studio中使用，添加dependencies `compile 'com.qiniu:qiniu-android-sdk:7.0.+'` 或在项目中添加maven依赖
* 如果是eclipse, 也可以直接添加依赖来处理。

## 使用方法
UploadManager 可以创建一次，一直使用。
```java
import com.qiniu.android.storage.UploadManager;
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

## 运行环境

Android 最低要求 2.2

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
