# Qiniu Resource Storage SDK for Android

[![@qiniu on weibo](http://img.shields.io/badge/weibo-%40qiniutek-blue.svg)](http://weibo.com/qiniutek)
[![Software License](https://img.shields.io/badge/license-MIT-brightgreen.svg)](LICENSE.md)
[![Build Status](https://travis-ci.org/qiniu/android-sdk.svg?branch=master)](https://travis-ci.org/qiniu/android-sdk)
[![Latest Stable Version](http://img.shields.io/maven-central/v/com.qiniu/qiniu-android-sdk.svg)](https://github.com/qiniu/android-sdk/releases)

## 演示代码
https://github.com/qiniudemo/qiniu-lab-android

## 安装

### 运行环境

| Qiniu SDK 版本 | 最低 Android版本   |       依赖库版本           | 
|------------ |-----------------|------------------------|
|  7.2.x        |  Android 2.3+     |        okhttp 3+         |
|  7.1.x        |  Android 2.3+     |        okhttp 2.6+       |
| 7.0.8,7.0.9   |  Android 2.2+     | android-async-http 1.4.9 |
|  7.0.7        |  Android 2.2+     | android-async-http 1.4.8 |

### 直接安装
将sdk jar文件 复制到项目中去，[下载地址](http://search.maven.org/remotecontent?filepath=com/qiniu/qiniu-android-sdk/) 还有对应的依赖库
 还有 happy-dns [下载地址](https://repo1.maven.org/maven2/com/qiniu/happy-dns/)

### 通过maven
* 如果在Adroid Studio中使用，添加dependencies `compile 'com.qiniu:qiniu-android-sdk:7.2.+'` 或在项目中添加maven依赖
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

## 常见问题
* 混淆处理
对七牛的SDK不需要做特殊混淆处理，混淆时将七牛相关的包都排除就可以了。 

1. 在Android Studio中，混淆配置在 proguard-rules.pro 里面加上下面几行混淆代码就行：
```
-keep class com.qiniu.**{*;}
-keep class com.qiniu.**{public <init>();}
-ignorewarnings
```

注意：-ignorewarnings这个也是必须加的，如果不加这个编译的时候可能是可以通过的，但是release的时候还是会出现错误。 

2. 对于Eeclipse中也是一样的，在proguard-project.txt 文件附件同样的排除代码就可以了
```
-keep class com.qiniu.**{*;}
-keep class com.qiniu.**{public <init>();}
-ignorewarnings
```

* 为什么进度会在95% 停很久
因为上传进度是用sdk写入socket的 字节数/总字节数 作为进度，但写入socket不等于服务器收到并且处理完成，中间还有一段时间，如果只是用字节数就会出现更怪异的情况，在100% 停留很久，所以综合考虑就使用了 95%这个值

* 如何才能得到下载的url
上传这边没有域名概念，只有bucket，一个bucket可以绑定多个域名。 下载的url 可以用bucket里的域名+key 拼接就成，私有的还要加上token

## 运行环境

Android 最低要求 2.3

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
