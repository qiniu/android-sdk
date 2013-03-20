
# 七牛云存储 Android SDK

此 Android SDK 基于 [七牛云存储官方API](http://docs.qiniutek.com/v3/api/) 构建。使用此 SDK 能够非常方便地将 Android 系统里边的文件快速直接上传到七牛云存储。

上传流程如下：

1. 业务服务器生成并颁发上传授权凭证（uploadToken）给 Android 客户端
2. Android 客户端程序接入此 SDK 将文件直传到七牛云存储

业务服务器生成上传授权凭证（uploadToken）需要搭配 七牛云存储其他编程语言的 SDK 使用。

## 目录
- [1. 使用](#1-)
- [2. 上传文件](#2-)
- [3. demo说明](#3-demo-)
- [4. 贡献代码](#4-)
- [5. 许可证](#5-)

## 1. 使用

参考文档：[七牛云存储 Android SDK 使用指南](http://docs.qiniutek.com/v3/sdk/android/)   
本SDK的开发环境是 `Intellij IDEA` , 如果开发者使用的是IDEA, 直接打开项目即可, 对于 `eclipse` 用户, 可以尝试导入项目.
导入后, 填写相关必要参数即可运行demo, 配置方法见[这里](#3-demo-)

## 2. 上传文件

在Android中选择文件一般是通过Uri作为路径, 一般调用以下代码
```java
UpOption opts = new UpOption();
opts.EntryUri = bucketName + ":" + key;
opts.MimeType = "image/png";
opts.CustomMeta = "自定义数据";

Up up = new Up(UpToken);
up.PutFile(context, uri, filename, opts, new PutFileRet() {
	@Override
	public void onSuccess(String hash) {
		// 成功
	}
	
	@Override
	public void onFailure(Exception ex) {
		// 失败
	}
})
```

- `UpToken`: 上传授权凭证，由业务服务端使用七牛云存储相关SDK（比如[PHP](https://github.com/qiniu/php5-sdk)/[Python](https://github.com/qiniu/python-sdk)/[Go](https://github.com/qiniu/api)/[Java](https://github.com/qiniu/java-sdk) SDK 等）生成，Android 端将此 upToken 作为参数传递给具体负责上传文件的执行对象，即可向七牛云存储直传文件。
- `filename`: 是指上传的文件命名, 如果填null将会生成一个6位随机字符串.
- `UpOption`: 这个是上传数据相关参数
	- `EntryUri`: *必填*, bucketName + ":" + key, 用于表示要上传对应文件的路径.
	- `MimeType`: 上传文件的类型, 如果不指定, 默认是 `application/octet-stream`. 七牛服务器在接收到该资源的HTTP请求时将使用这个字段作为 `Content-Type` 的值.
	- `CustomMeta`: 自定义说明.
	- `Crc32`: 文件的 crc32 校验值，十进制整数，可选项。若不传此参数则不执行数据校验。
	- `Rotate`: 上传图片时专用，可针对图片上传后进行旋转。该参数值为:
		- 值为 0: 表示根据图像EXIF信息自动旋转;
		- 值为 1: 右转90度;
		- 值为 2: 右转180度;
		- 值为 3: 右转270度。
- BucketName: 填写bucket的名字, 如果还没有 bucket，可以登录[七牛云存储开发者自助网站](https://dev.qiniutek.com/buckets/new)创建
- Key: 该文件的标识, 如果绑定对应的域名, 然后就可以通过 `http://<domain>/<key>` 访问对应的资源.

> 相关API文档可以参考[这里](http://docs.qiniutek.com/v3/api/io/#apimultipartform-data)

## 3. demo 说明

demo无法直接使用, 需要配置`UpToken`, `BucketName`, `Domain`信息, 将其填写到 MyActivity 之中.
`key`值可以在操作界面修改.   
当文件上传成功时, 会试图跳转到浏览器访问已经上传的资源. 如果失败, 会toast提示.


## 4. 贡献代码

1. Fork
2. 创建您的特性分支 (`git checkout -b my-new-feature`)
3. 提交您的改动 (`git commit -am 'Added some feature'`)
4. 将您的修改记录提交到远程 `git` 仓库 (`git push origin my-new-feature`)
5. 然后到 github 网站的该 `git` 远程仓库的 `my-new-feature` 分支下发起 Pull Request

## 5. 许可证

Copyright (c) 2012 qiniutek.com

基于 MIT 协议发布:

* [www.opensource.org/licenses/MIT](http://www.opensource.org/licenses/MIT)
