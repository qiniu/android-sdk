# Qiniu Resource (Cloud) Storage SDK demo for Java 

# 关于
这些demo都是基于 Qiniu Resource (Cloud) Storage SDK for Java 开发的。其中RSDemo.java演示了服务端进行资源上传，下载，删除，发布，取消发布等功能的演示。
其中Notifier.java 和 UpDemo.java 演示了客户端进行断点续上传。 
	在ResumableNotifier.java和ResumablePutDemo.java中对客户端在断点续上传过程中的状态进行了序列化，使客户在上传过程中即使程序崩溃或者退出在下次重新启动时还可以接着上次继续上次。
	ResumableGUINotifier.java 和 ResumableGUIputDemo.java 则添加了客户在断点续上传时的UI，使上传更加直观。


## 安装

按照java sdk的步骤去安装。

## 使用
要接入七牛云存储，您需要拥有一对有效的 Access Key 和 Secret Key 用来进行签名认证。可以通过如下步骤获得：
[开通七牛开发者账号](https://dev.qiniutek.com/signup)
[登录七牛开发者自助平台，查看Access Key 和 Secret Key](https://dev.qiniutek.com/account/keys)
然后把得到的Access Key 和 Secret Key 写入每个demo中后。
1.RSDemo可以直接运行。
2.ResumablePutDemo 和 ResumableGUIPutDemo 则需要传入 要上传文件的具体路径加文件名 作为参数。
在eclipse中您可以这样添加：选中您要运行的demo-->右键-->选中 Build Path-->Run/Debug Settings-->双击你要运行的demo-->选中 ‘(x)=Arguments’ 选项卡-->在Program arguments中加入要传入的参数。
在命令窗口下：”java 您要运行的demo 参数”  把参数改成你的实际参数。
（你也可以在demo中把 String inputFile = args[0];中的args[0]换成你的参数）
参考文档：[七牛云存储 Java SDK 使用指南](http://docs.qiniutek.com/v2/sdk/java/)

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


