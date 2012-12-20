
# Android SDK | 七牛云存储

**环境准备**

- [工程依赖](#dependencies)

**应用接入**

- [获取Access Key 和 Secret Key](#acc-appkey)
- [配置Access Key 和 Secret Key](#appkeycfg)

**上传文件**

- [生成上传授权凭证](#uploadToken)
- [上传文件](#uploadfile)
    - [普通上传方式](#normalUpload)
    - [断点续上传方式（一）](#resumable1)
    - [断点续传上接方式（二）](#resumable2)


## 环境准备

<a name="dependencies"></a>

### 1. 工程依赖

需要在Eclipse工程中，导入七牛云存储的 SDK。目前，七牛云存储的 SDK 依赖于以下第三方包：

- commons-logging-1.1.1.jar
- fluent-hc-4.2.jar
- httpclient-4.2.jar
- httpclient-cache-4.2.jar
- httpcore-4.2.1.jar
- httpcore-4.2.jar
- httpcore-ab-4.2.1.jar
- httpcore-nio-4.2.1.jar
- httpmime-4.2.jar

七牛云存储 SDK 中的 qbox/lib 目录默认已经包含这些第三方包，开发者直接使用就行。但是，也有可能因为你本地编译环境问题，需要重新载入这些包。

在开发过程中本 SDK 选用的 Android2.2，以及Java1.6。

## 应用接入

<a name="acc-appkey"></a>

### 1. 获取Access Key 和 Secret Key

要接入七牛云存储，您需要拥有一对有效的 Access Key 和 Secret Key 用来进行签名认证。可以通过如下步骤获得：

1. [开通七牛开发者帐号](https://dev.qiniutek.com/signup)
2. [登录七牛开发者自助平台，查看 Access Key 和 Secret Key](https://dev.qiniutek.com/account/keys) 。

<a name="appkeycfg"></a>

### 2. 配置Access Key 和 Secret Key

首先，到 [https://github.com/qiniu/android-sdk](https://github.com/qiniu/android-sdk) 下载Android SDK源码。

然后，将SDK导入到您的 Eclipse 项目中，并编辑当前工程目录下Config.java文件，确保其包含您从七牛开发者平台所获取的 [Access Key 和 Secret Key](#acc-appkey)：

    ACCESS_KEY = "<Please apply your access key>";
    SECRET_KEY = "<Dont send your secret key to anyone>";


<a name="uploadfile"></a>

## 上传文件

Android SDK 目前支持从本地上传某个文件，上传方式分为直传和断点续传。所谓断点续传，即：用户在某次上传过程中失败，再重新上传的时候只需要从上次失败的点开始续传即可。用户可以根据需要选择相应的上传方式。

<a name="uploadToken"></a>

### 1. 生成上传授权凭证

在完成 Access Key 和 Secret Key 配置后，为了正常使用该 SDK 提供的功能需要根据配置文件进行初始化，您还需要使用你获得的 Access Key 和 Secret Key 向七牛云存储服务器发出认证请求：

    AuthPolicy policy = new AuthPolicy(bucketName, 3600);
    String token = policy.makeAuthTokenString();
    UpTokenClient upTokenClient = new UpTokenClient(token);

请求成功后得到的 upTokenClien 即可用于您正常使用七牛云存储的一系列功能，接下来将一一介绍。

<a name="normalUpload"></a>

#### 2.1 普通上传方式

示例代码 ：

    Config.ACCESS_KEY = "YOUR ACCESS_KEY";
    Config.SECRET_KEY = "YOUR SECRET_KEY, Dont send to anyone";
    String localFile = "YOUR LOCAL FILE TO UPLOAD";

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


参数详解：

token
: 必须，字符串类型，上传授权凭证。

bucketName
: 必须，字符串类型。

key
: 必须，字符串类型。

localFile
: 必须，字符串类型。

optParams
: 可选，可选参数，键值对形式的Map，目前支持的可选参数如下

字段名称   | 类型 | 说明 | 缺省值
---------|---------|---------|-------
mimeType | string | 资源类型 | application/octet-stream
customeMeta | string | 自定义说明 | 空
callbackParams | string | 文件上传成功后七牛云存储回调业务服务器所发送的数据 | 空
rotate | int | 上传图片时专用，可针对图片上传后进行旋转。该参数值为 0 ：表示根据图像EXIF信息自动旋转；值为 1 : 右转90度；值为 2 :右转180度；值为 3 : 右转270度。| 0

如果上传成功，得到的 putFileRet 会包含对应的 hash 值，否则返回对应的错误。

<a name="resumable1"></a>

#### 2.2 断点续上传方式（一）：

为了方便用户使用，我们将断点续上传的API进行了友好的封装。在此用户不必对保存文件上传进度而费心，因为我们已经帮您做了。如果您想自己实现上传文件的持久化方式请参考 [断点续上传方式（一）](#resumable1)。

示例代码：

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

`UpClient.resumablePutFile()` 参数详解：

upClient
:必须，通过uptoken认证之后得到的连接

bucketName
: 必须，对应的资源表的名称

key
: 必须，唯一标识

localFile
: 必须，字符串类型，上传文件的全路径名

optParams
: 可选参数，键值对形式的Map，目前支持的可选参数如下

字段名称   | 类型 | 说明 | 缺省值
---------|---------|---------|-------
mimeType | string | 资源类型 | application/octet-stream
customeMeta | string | 自定义说明 | 空
callbackParams | string | 文件上传成功后七牛云存储回调业务服务器所发送的数据 | 空
progressFile | string | SDK用保存上传进度的文件 | 0


客户端在上传文件的时候也可以根据需求选择断点续上传的方式，此处所说的断点上传是指用户在某次上传过程中出现错误，再重新上传的时候只需要从上次上传失败处上传即可。用户可以根据通过修改配置文件改变上传块（Config文件中的`PUT_CHUNK_SIZE` 对应的值）的大小来适应用户所处的网络环境。为了提供一个简单的接口，我们将上传进度持久化的相关工作内置在了 SDK 中，当然用户也可以根据需要自己实现文件上传进度的持久化工作。

如果上传成功，得到的 putFileRet 会包含对应的 hash 值，否则返回对应的错误。

<a name="resumable2"></a>

#### 2.3 断点续传上接方式（二）：

为了给用户更大的灵活性，我们提供了如下的接口。通过该接口用户可以取得更多的信息，比如文件上传的进度，以及自由选择上传进度的持久方式等等。

示例代码参照 SDK 自带的 demo: `qiniu-android-sdk/src/com/qiniu/qbox/demo` 。
