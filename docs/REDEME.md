---
title: Android SDK | 七牛云存储
---

**环境准备**

**应用接入**

- [获取Access Key 和 Secret Key](#acc-appkey)
- [签名认证](#acc-auth)

**云存储接口**

- [获得上传授权]
- [上传文件]


## 环境准备

1.需要在Eclipse工程中，导入七牛云存储的 SDK。目前，七牛云存储的 SDK 依赖于一下第三方包：

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

2.在开发过程中本sdk选用的Android2.2，以及Java1.6。

## 应用接入

<a name="acc-appkey"></a>

### 1. 获取Access Key 和 Secret Key

要接入七牛云存储，您需要拥有一对有效的 Access Key 和 Secret Key 用来进行签名认证。可以通过如下步骤获得：

1. [开通七牛开发者帐号](https://dev.qiniutek.com/signup)
2. [登录七牛开发者自助平台，查看 Access Key 和 Secret Key](https://dev.qiniutek.com/account/keys) 。

<a name="acc-auth"></a>

### 2. uptoken认证

首先，到 [https://github.com/qiniu/android-sdk](https://github.com/qiniu/android-sdk) 下载Android SDK源码。

然后，将SDK导入到您的 Eclipse 项目中，并编辑当前工程目录下Config.java文件，确保其包含您从七牛开发者平台所获取的 [Access Key 和 Secret Key](#acc-appkey)：

    ACCESS_KEY	= "<Please apply your access key>";
	SECRET_KEY	= "<Dont send your secret key to anyone>";



在完成 Access Key 和 Secret Key 配置后，为了正常使用该 SDK 提供的功能需要根据配置文件进行初始化，您还需要使用你获得的 Access Key 和 Secret Key 向七牛云存储服务器发出认证请求：

>	AuthPolicy policy = new AuthPolicy(bucketName, 3600);    
String token = policy.makeAuthTokenString();  
UpTokenClient upTokenClient = new UpTokenClient(token);

请求成功后得到的 upTokenClien 即可用于您正常使用七牛云存储的一系列功能，接下来将一一介绍。

## 云存储接口


### 1. 上传文件

Android SDK 目前支持从本地上传某个文件，上传方式分为直传和断点续传。所谓断点续传，即：用户在某次上传过程中失败，再重新上传的时候只需要从上次失败的点开始续传即可。用户可以根据需要选择相应的上传方式。
如果上传成功，得到的 putFileRet 会包含对应的 hash 值，否则返回对应的错误。

##### 客户端直传

参数：　

upToken : 必须，字符串类型。

bucketName : 必须，字符串类型。 

key : 必须，字符串类型。

localFile : 必须，字符串类型。

optParams : 可选，可选参数，键值对形式的Map，目前支持的可选参数如下
>  mimeType :  缺省值为"application/octet-stream"  
customeMeta :  缺省值为空  
callbackParams :  缺省值为空
rotate ： 缺省值为"0",表示自动旋转

示例代码 ：    
  
    Config.ACCESS_KEY = "YOUR ACCESS_KEY";  
    Config.SECRET_KEY = "YOUR SECRET_KEY, Dont send to anyone";   
    String localFile = "YOUR LOCAL FILE TO UPLOAD" ;  
    
	// get uptoken
    String bucketName = "bucketName";   
    String key = "knuth.jpg";   
    AuthPolicy policy = new AuthPolicy(bucketName, 3600);  
    String token = policy.makeAuthTokenString();  

    // your optional parameters here
    Map<String, Object> optParams = new HashMap<String, Object>() ;  
    optParams.put("mimeType", "YOUR MIME_TYPE HERE") ;  
    optParams.put("customMeta", "YOUR CUSTOM_META HERE") ;  
    optParams.put("callbackParms", "YOUR CALLBACK_PARAM HERE") ;  
    optParams.put("rotate", "YOUR ROTATE HERE") ;   

	// upload the local to the qiniu cloud server
    putFileRet = UpClient.putFile(token, bucketName, key, localFile, optParams);  


##### 断点续传接口一：
为了给用户更大的灵活性，我们提供了如下的接口。通过该接口用户可以取得更多的信息，比如文件上传的进度，以及自由选择上传进度的持久方式等等。

参数： 

c : 必须，通过uptoken认证之后得到的连接

checksums : 必须，String类型的数组，为了校验之用的校验和

progresses : 必须，String类型的数组，文件上传的进度

progressNotifier : 必须，每上传完一个chunck的时候，保存此时的进度

blockProgressNotifier : 必须，每上传完一个block的时候，保存此时的进度

bucketName : 必须，对应的资源表的名称
	
key :	必须，唯一标识

mimeType : 可选

file :	必须，RandomAccessFile类型,要上传的文件

fsize : 必须，要上传文件的大小

customMeta : 可选

callBackParams : 可选，回调参数

返回值：
> 若果上传成功，得到的putFileRet会包含对应的hash值，否则返回对应的错误。 

示例代码如下：

    请参照demo。

	
##### 断点续传接口二：
为了方便用户使用，我们将断点续上传的API进行了友好的封装。在此用户不必对保存文件上传进度而费心，因为我们已经帮您做了。如果您想自己实现上传文件的持久化方式请参考"接口一"。

参数：


c : 必须，通过uptoken认证之后得到的连接

bucketName : 必须，对应的资源表的名称

key :	必须，唯一标识

localFile : 必须，字符串类型，上传文件的全路径名

optParams ： 可选参数，键值对形式的Map，目前支持的可选参数如下
>  mimeType :  缺省值为"application/octet-stream" 
customeMeta :  缺省值为空  
progressFile :  SDK用保存上传进度的文件 
callbackParams :  缺省值为null  

客户端在上传文件的时候也可以根据需求选择断点续上传的方式，此处所说的断点上传是指用户在某次上传过程中出现错误，再重新上传的时候只需要从上次上传失败处上传即可。用户可以根据通过修改配置文件改变上传块（Config文件中的PUT_CHUNK_SIZE对应的值）的大小来适应用户所处的网络环境。为了提供一个简单的接口，我们将上传进度持久化的相关工作内置在了 SDK 中，当然用户也可以根据需要自己实现文件上传进度的持久化工作。

如果上传成功，得到的 putFileRet 会包含对应的 hash 值，否则返回对应的错误。

示例代码如下：  

		Config.ACCESS_KEY = "YOUR ACCESS KEY HERE";
		Config.SECRET_KEY = "YOUR SECRET KEY HERE, Dont send to others";
		String bucketName = "bucket";
		String key = "golang.key";
		String localFile = "/mnt/sdcard/rpc.go" ;

		// get the auth conn
		AuthPolicy policy = new AuthPolicy("bucket", 3600);
		String token = policy.makeAuthTokenString();
		UpTokenClient upTokenClient = new UpTokenClient(token);
		UpService upClient = new UpService(upTokenClient);
		// your optional parameters
		Map<String, Object> optParams = new HashMap<String, Object>() ;
		optParams.put("mimeType", "") ;
		optParams.put("callbakParam", "") ;
		optParams.put("progressFile", "") ;
		optParams.put("customMeta", "") ;

		PutFileRet putFileRet = UpClient.resumablePutFile(upClient, bucketName, key, localFile, optParams) ;
