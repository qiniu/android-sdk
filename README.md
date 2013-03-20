
# 七牛云存储 Android SDK

此 Android SDK 基于 [七牛云存储官方API](http://docs.qiniutek.com/v3/api/) 构建。使用此 SDK 能够非常方便地将 Android 系统里边的文件快速直接上传到七牛云存储。

上传流程如下：

1. 业务服务器生成并颁发上传授权凭证（uploadToken）给 Android 客户端
2. Android 客户端程序接入此 SDK 将文件直传到七牛云存储

业务服务器生成上传授权凭证（uploadToken）需要搭配 七牛云存储其他编程语言的 SDK 使用。

## 使用

参考文档：[七牛云存储 Android SDK 使用指南](http://docs.qiniutek.com/v3/sdk/android/)

## demo 说明

demo无法直接使用, 需要配置以下信息, 将其填写到 MyActivity 之中:

- UpToken: 这个由业务服务器负责生成, Android端仅仅负责向业务服务器请求UpToken, 如果仅仅想测试的话只需要把token填写到 `String UP_TOKEN`.
- BucketName: 填写bucket的名字, 如果还没有bucket, 可以到[这里](https://dev.qiniutek.com/buckets)申请
- Domain: 当申请bucket之后, 可以绑定对应的域名, 然后就可以通过 `http://<domain>/<key>` 访问对应的资源.

上传成功后, 程序会试图通过浏览器访问其资源.

## 贡献代码

1. Fork
2. 创建您的特性分支 (`git checkout -b my-new-feature`)
3. 提交您的改动 (`git commit -am 'Added some feature'`)
4. 将您的修改记录提交到远程 `git` 仓库 (`git push origin my-new-feature`)
5. 然后到 github 网站的该 `git` 远程仓库的 `my-new-feature` 分支下发起 Pull Request

## 许可证

Copyright (c) 2012 qiniutek.com

基于 MIT 协议发布:

* [www.opensource.org/licenses/MIT](http://www.opensource.org/licenses/MIT)
