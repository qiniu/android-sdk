---
title: Android SDK 使用指南
---

此 Android SDK 基于 [七牛云存储官方API](http://docs.qiniu.com/api/index.html) 构建。在开发者的 Android App 工程项目中使用此 SDK 能够非常方便地将 Android 系统里边的文件快速直传到七牛云存储。

出于安全考虑，使用此 SDK 无需设置密钥（AccessKey / SecretKey）。所有涉及到授权的操作，比如生成上传授权凭证（uploadToken）或下载授权凭证（downloadToken）均在业务服务器端进行。

业务服务器负责生成和颁发授权，此 SDK 只负责实施具体的上传业务。

## 目录

- [上传流程](#upload-flow)
- [下载流程](#download-flow)
- [接入SDK](#load)
- [使用SDK上传文件](#upload)
- [SDK 内置 demo 说明](#demo)
- [并发特性](#concurrency)
- [贡献代码](#contributing)
- [许可证](#license)

<a name="upload-flow"></a>

## 上传流程

1. 业务服务器使用七牛云存储服务端编程语言（如 PHP/Python/Ruby/Java）SDK 生成 uploadToken (上传授权凭证)

2. 客户端 Android 使用该 uploadToken 调用此 Android 封装的上传方法直传文件到七牛云存储

3. 文件直传成功，七牛云存储向 uploadToken 生成之前所指定的业务服务器地址发起回调

4. 业务服务器接收来自七牛云存储回调的 POST 请求，处理相关 POST 参数，最后响应输出一段 JSON

5. 七牛云存储接收业务服务器响应输出的这段 JSON，原封不动地通过 HTTP 返回给 Android 客户端程序


注意事项：

- 此 Android SDK 当前只提供上传方法，即负责上述流程中的第2个步骤。
- 业务服务器响应回调请求后输出 JSON，HTTP Headers 必须输出 `Content-Type` 为 `application/json`。
- 文件上传成功后，业务服务器输出的 JSON 数据，可从所调用SDK上传代码的返回值中获取到。


<a name="download-flow"></a>

## 下载流程

此 Android SDK 没有提供下载文件的方法。所有上传到七牛云存储的文件，都能以如下方式进行访问：

公开资源：

    http://<domain>/<key>

私有资源：

    http://<domain>/<key>?token=<downloadToken>

其中\<domain\>是bucket所对应的域名。七牛云存储为每一个bucket提供一个默认域名。默认域名可以到[七牛云存储开发者平台](https://portal.qiniu.com/)中，空间设置的域名设置一节查询。

出于安全考虑，此 SDK 不提供 `downloadToken` 的生成。除 Android / iOS SDK 以外，七牛云存储其他编程语言的 SDK 都有提供签发私有资源下载授权凭证（downloadToken）的实现。

**注意： key必须采用utf8编码，如使用非utf8编码访问七牛云存储将反馈错误**

<a name="load"></a>

## 接入SDK

本SDK的开发环境是 [Intellij IDEA](http://www.jetbrains.com/idea/)，如果开发者使用的编辑器同为 IDEA, 直接打开项目即可，对于使用 [Eclipse](http://www.eclipse.org/) 编辑器的开发者，可以尝试导入项目。

导入后，填写相关必要参数即可运行SDK自带的 demo 程序，配置方法见 [SDK 内置 demo 说明](#demo) 。


<a name="upload"></a>

## 使用SDK上传文件

在 Android 中选择文件一般是通过 uri 作为路径, 一般调用以下代码

```{java}
@gist(../src/com/qiniu/demo/MyActivity.java#upload_arg)

@gist(../src/com/qiniu/demo/MyActivity.java#upload)
```


<a name="demo"></a>

## SDK 内置 demo 说明

注意：demo 程序无法直接运行，需要配置 `UpToken`, `BucketName`, `Domain`信息, 将其填写到 MyActivity 之中。`key`值可以在操作界面修改。当文件上传成功时，会试图跳转到浏览器访问已经上传的资源。如果失败，会toast提示。

<a name="concurrency"></a>

## 并发特性

此 Android SDK 不是线程安全的，请勿在没有保护的情况下跨线程使用。


<a name="contributing"></a>

## 贡献代码

1. Fork
2. 创建您的特性分支 (`git checkout -b my-new-feature`)
3. 提交您的改动 (`git commit -am 'Added some feature'`)
4. 将您的修改记录提交到远程 `git` 仓库 (`git push origin my-new-feature`)
5. 然后到 github 网站的该 `git` 远程仓库的 `my-new-feature` 分支下发起 Pull Request


<a name="license"></a>

## 许可证

Copyright (c) 2013 www.qiniu.com

基于 MIT 协议发布:

* [www.opensource.org/licenses/MIT](http://www.opensource.org/licenses/MIT)
