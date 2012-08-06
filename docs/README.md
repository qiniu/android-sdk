---
title: Java SDK | 七牛云存储
---

# Java SDK 使用指南


此SDK适用于Java 6及以上版本。

SDK下载地址：[https://github.com/qiniu/java-sdk/tags](https://github.com/qiniu/java-sdk/tags)


**环境准备**

**应用接入**

- [获取Access Key 和 Secret Key](#acc-appkey)
- [签名认证](#acc-auth)

**云存储接口**

- [新建资源表](#rs-NewService)
- [获得上传授权](#rs-PutAuth)
- [上传文件](#rs-PutFile)
- [获取已上传文件信息](#rs-Stat)
- [下载文件](#rs-Get)
- [发布公开资源](#rs-Publish)
- [取消资源发布](#rs-Unpublish)
- [删除已上传的文件](#rs-Delete)
- [删除整张资源表](#rs-Drop)

## 环境准备

需要在Eclipse工程中，导入七牛云存储的 SDK。目前，七牛云存储的 SDK 依赖于一下第三方包：

- commons-codec-1.6.jar
- commons-logging-1.1.1.jar
- fluent-hc-4.2.jar
- httpclient-4.2.jar
- httpclient-cache-4.2.jar
- httpcore-4.2.1.jar
- httpcore-4.2.jar
- httpcore-ab-4.2.1.jar
- httpcore-nio-4.2.1.jar
- httpmime-4.2.jar

七牛云存储 SDK 中的 qbox/lib 目录默认已经包含这些第三方包，您直接使用就行。但是，也有可能因为你本地编译环境问题，需要重新载入这些包。

## 应用接入

<a name="acc-appkey"></a>

### 1. 获取Access Key 和 Secret Key

要接入七牛云存储，您需要拥有一对有效的 Access Key 和 Secret Key 用来进行签名认证。可以通过如下步骤获得：

1. [开通七牛开发者帐号](https://dev.qiniutek.com/signup)
2. [登录七牛开发者自助平台，查看 Access Key 和 Secret Key](https://dev.qiniutek.com/account/keys) 。

<a name="acc-auth"></a>

### 2. 签名认证

首先，到 [https://github.com/qiniu/java-sdk/tags](https://github.com/qiniu/java-sdk/tags) 下载SDK源码。

然后，将SDK导入到您的 Eclipse 项目中，并编辑 com.qiniu.qbox 这个包下的 Config.java 文件，确保其包含您从七牛开发者平台所获取的 [Access Key 和 Secret Key](#acc-appkey)：

    public static String ACCESS_KEY	= "<Please apply your access key>";
	public static String SECRET_KEY	= "<Dont change here>";

在完成 Access Key 和 Secret Key 配置后，为了正常使用该 SDK 提供的功能，您还需要使用你获得的 Access Key 和 Secret Key 向七牛云存储服务器发出认证请求：

	DigestAuthClient conn = new DigestAuthClient();

请求成功后得到的 conn 即可用于您正常使用七牛云存储的一系列功能，接下来将一一介绍。

## 云存储接口

<a name="rs-NewService"></a>

### 1. 新建资源表

新建资源表的意义在于，您可以将所有上传的资源分布式加密存储在七牛云存储服务端后还能保持相应的完整映射索引。

    // 首先定义资源表名
    String bucketName = "bucketName";
    
    // 然后获得签名认证
    DigestAuthClient conn = new DigestAuthClient();
    
    // 签名认证完成后，即可使用该认证来新建资源表
    RSService rs = new RSService(conn, bucketName);


<a name="rs-PutAuth"></a>

### 2. 获得上传授权

建完资源表，在上传文件并和该资源表建立关联之前，还需要取得上传授权。所谓上传授权，就是获得一个可匿名直传的且离客户端应用程序最近的一个云存储节点的临时有效URL。

要取得上传授权，只需调用已经实例化好的资源表对象的 putAuth() 方法。实例代码如下：

    // 获得上传授权之前需要通过签名认证的方式来实例化一个资源表对象
    String bucketName = "bucketName";
    DigestAuthClient conn = new DigestAuthClient();
    RSService rs = new RSService(conn, bucketName);
    
    // 然后，调用该资源表对象的 putAuth() 方法来获得上传授权
    PutAuthRet putAuthRet = rs.putAuth();


如果请求成功，putAuthRet 会包含 url 和 expires_in 两个字段。url 字段对应的值为匿名上传的临时URL，expires_in 对应的值则是该临时URL的有效期。

<a name="rs-PutFile"></a>

### 3. 上传文件

一旦建立好资源表和取得上传授权，就可以开始上传文件了。七牛云存储上传文件的方式分为服务器端上传和客户端上传两种。

##### 1. 服务器端上传

Java SDK 目前提供了两种类型的服务器端数据上传方式，一种是直接上传某个本地文件 ，另一种是通过某个带有特定文件信息的文件类上传文件。这两种上传方式都是以直传的方式进行。

要上传某个本地文件，只需调用实例化好的资源表对象rs的 putFile() 方法，示例代码如下：

	// 服务器端上传文件之前需要获得针对某个资源表名的签名认证
	String bucketName = "bucketName";
	DigestAuthClient conn = new DigestAuthClient();
    RSService rs = new RSService(conn, bucketName);
    
    // 通过该实例化的资源表对象来进行文件上传
	PutFileRet putFileRet = rs.putFile(key, mimeType, localFile, customMeta);
	

##### 2. 客户端上传
	    
客户端上传跟服务器端上传不太一样，客户的文件无需经过您服务器即可上传到七牛云存储服务器。这类上传方式常见于面向首次设备终端用户的应用中，其客户通过向服务器请求的方式得到一个匿名 URL 进行上传。当终端用户上传成功后，七牛云存储服务端会向您指定的 callback_url 发送回调数据。

客户端使用一个实例化好的资源表对象返回的匿名 URL 进行文件上传，示例代码如下：

	// 在客户端上传文件之前，需要向服务器端获取上传认证，并通过该上传认证获取一个供上传文件的临时 URL
	String bucketName = "bucketName";
	DigestAuthClient conn = new DigestAuthClient();
    RSService rs = new RSService(conn, bucketName);
    PutAuthRet putAuthRet = rs.putAuth();
    uploadUrl = putAuthRet.getUrl();
    
    // 通过该临时 URL 进行文件上传
	PutFileRet putFileRet = RSClient.putFile(uploadUrl, bucketName, key, "", path + key, "", null);


<a name="rs-Stat"></a>

### 4. 获取已上传文件信息

您可以调用资源表对象的 Stat() 方法并传入一个 Key（类似ID）来获取指定文件的相关信息。

	// 实例化一个资源表对象，并获得一个相应的授权认证
	String bucketName = "bucketName";
	DigestAuthClient conn = new DigestAuthClient();
    RSService rs = new RSService(conn, bucketName);
    
    // 获取资源表中特定文件信息
    StatRet statRet = rs.stat(key);

如果请求成功，得到的 statRet 数组将会包含如下几个字段：

    hash: <FileETag>
    fsize: <FileSize>
    putTime: <PutTime>
    mimeType: <MimeType>


<a name="rs-Get"></a>

### 5. 下载文件

要下载一个文件，首先需要取得下载授权，所谓下载授权，就是取得一个临时合法有效的下载链接，只需调用资源表对象的 Get() 方法并传入相应的 文件ID 和下载要保存的文件名 作为参数即可。示例代码如下：

	// 实例化一个资源表对象，并获得一个下载已上传文件信息的授权认证
	String bucketName = "bucketName";
	DigestAuthClient conn = new DigestAuthClient();
    RSService rs = new RSService(conn, bucketName);
    
    // 下载资源表中的特定文件
    GetRet getRet = rs.get(key, key);


注意，这并不会直接将文件下载并命名为一个 example.jpg 的文件。当请求执行成功，Get() 方法返回的 getRet 变量将会包含如下字段：

    url: <DownloadURL> # 获取文件内容的实际下载地址
    hash: <FileETag>
    fsize: <FileSize>
    mimeType: <MimeType>
    expires:<Seconds> ＃下载url的实际生命周期，精确到秒

这里所说的断点续传指断点续下载，所谓断点续下载，就是已经下载的部分不用下载，只下载基于某个“游标”之后的那部分文件内容。相对于资源表对象的 Get() 方法，调用断点续下载方法 GetIfNotModified() 需额外再传入一个 $baseVersion 的参数作为下载的内容起点。示例代码如下：

    // 以断点续下载的方式下载资源表中的某个文件
    GetRet getIfNotModifiedRet = rs.getIfNotModified(key, key, getRet.getHash());

GetIfNotModified() 方法返回的结果包含的字段同 Get() 方法一致。

<a name="rs-Publish"></a>

### 6. 发布公开资源

使用七牛云存储提供的资源发布功能，您可以将一个资源表里边的所有文件以静态链接可访问的方式公开发布到您自己的域名下。

要公开发布一个资源表里边的所有文件，只需调用改资源表对象的 Publish() 方法并传入 域名 作为参数即可。如下示例：

	// 实例化一个资源表对象，并获得一个发布公开资源的授权认证
	String bucketName = "bucketName";
	DigestAuthClient conn = new DigestAuthClient();
    RSService rs = new RSService(conn, bucketName);
    
    // 公开发布某个资源表
    PublishRet publishRet = rs.publish(Config.DEMO_DOMAIN + "/" + bucketName);

<a name="rs-Unpublish"></a>

### 7. 取消资源发布

调用资源表对象的 Unpublish() 方法可取消该资源表内所有文件的静态外链。

    // 实例化一个资源表对象，并获得一个取消发布公开资源的授权认证
	String bucketName = "bucketName";
	DigestAuthClient conn = new DigestAuthClient();
    RSService rs = new RSService(conn, bucketName);
    
    // 取消公开发布某个资源表
    PublishRet unpublishRet = rs.unpublish(Config.DEMO_DOMAIN + "/" + bucketName);

<a name="rs-Delete"></a>

### 8. 删除已上传的文件

要删除指定的文件，只需调用资源表对象的 Delete() 方法并传入 文件ID（key）作为参数即可。如下示例代码：

    // 实例化一个资源表对象，并获得一个删除资源表中特定文件的授权认证
	String bucketName = "bucketName";
	DigestAuthClient conn = new DigestAuthClient();
    RSService rs = new RSService(conn, bucketName);
    
    // 删除资源表中的某个文件
    DeleteRet deleteRet = rs.delete(key);


<a name="rs-Drop"></a>

### 9. 删除整张资源表

要删除整个资源表及该表里边的所有文件，可以调用资源表对象的 Drop() 方法。
需慎重，这会删除整个表及其所有文件

    // 实例化一个资源表对象，并获得一个删除整张资源表的授权认证
	String bucketName = "bucketName";
	DigestAuthClient conn = new DigestAuthClient();
    RSService rs = new RSService(conn, bucketName);
    
    // 删除资源表中的某个文件
    DropRet dropRet = rs.drop();
