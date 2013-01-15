
# 七牛云存储 Android SDK

此 Android SDK 基于 [七牛云存储官方API](http://docs.qiniutek.com/v3/api/) 构建。使用此 SDK 能够非常方便地将 Android 系统里边的文件快速直接上传到七牛云存储。

上传流程如下：

1. 业务服务器生成并颁发上传授权凭证（uploadToken）给 Android 客户端
2. Android 客户端程序接入此 SDK 将文件直传到七牛云存储

业务服务器生成上传授权凭证（uploadToken）需要搭配 七牛云存储其他编程语言的 SDK 使用。

## 使用

参考文档：[七牛云存储 Android SDK 使用指南](http://docs.qiniutek.com/v3/sdk/android/)

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

