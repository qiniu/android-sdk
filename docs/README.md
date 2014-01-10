---
title: Android SDK使用文档
---

# Android SDK使用文档

## 目录

- [概述](#overview)
- [使用场景](#use-scenario)
- [接入SDK](#integration)
- [上传文件](#simple-upload)
- [分片上传（断点续上传）](#resumable-upload)
- [线程安全性](#thread-safety)

<a name="overview"></a>
## 概述

Android SDK只包含了最终用户使用场景中的必要功能。相比服务端SDK而言，客户端SDK不会包含对云存储服务的管理和配置功能。

该SDK支持不低于2.2的Android版本（api8）。

<a name="use-scenario"></a>
## 使用场景

在使用Android SDK开发基于七牛云存储的应用之前，请注意理解合适的开发场景。客户端属于不可控的场景，非一般用户在拿到客户端后可能会对其进行反向工程，因此客户端程序中不可包含任何可能导致安全漏洞的业务逻辑和关键信息。

我们推荐的安全模型如下所示：

![安全模型](http://developer.qiniu.com/docs/v6/api/overview/img/token.png)

开发者需要合理划分客户端程序和业务服务器的职责范围。分发给最终用户的客户端程序中不应有需要使用管理凭证及SecretKey的场景。这些可能导致安全风险的使用场景均应被设计为在业务服务器上进行。

更多的相关内容请查看[编程模型](http://developer.qiniu.com/docs/v6/api/overview/programming-model.html)和[安全机制](http://developer.qiniu.com/docs/v6/api/overview/security.html)。

<a name="security"></a>
## 安全性

该SDK未包含凭证生成相关的功能。开发者对安全性的控制应遵循[安全机制](http://developer.qiniu.com/docs/v6/api/overview/security.html)中建议的做法，即客户端应向业务服务器请求上传和下载凭证，而不是直接在客户端使用AccessKey/SecretKey生成对应的凭证。在客户端使用SecretKey会导致严重的安全隐患。

<a name="load"></a>
## 接入SDK

该SDK没有包含工程文件，这时需要自己新建一个工程，然后将src里面的代码复制到代码目录里面。

<a name="upload"></a>
## 上传文件

开发者可以选择SDK提供的两种上传方式：表单上传和分片上传。表单上传使用一个HTTP POST请求完成文件的上传，因此比较适合较小的文件和较好的网络环境。相比而言，分片上传更能适应不稳定的网络环境，也比较适合上传比较大的文件（数百MB或更大）。

若需深入了解上传方式之间的区别，请查看[上传类型](http://developer.qiniu.com/docs/v6/api/overview/up/upload-models.html#upload-types)，[表单上传接口说明](http://developer.qiniu.com/docs/v6/api/overview/up/form-upload.html)，[分片上传接口说明（断点续上传）](http://developer.qiniu.com/docs/v6/api/overview/up/chunked-upload.html)。

<a name="form-upload"></a>
### 表单上传

开发者可以通过调用`IO.put()`方法来以表单形式上传一个文件。该方法的详细说明如下：

```
public void put(String key, 
				InputStreamAt isa, 
				com.qiniu.io.PutExtra extra, 
				JSONObjectRet ret);
```

参数说明：

参数 | 类型 | 说明 
:---: | :----: | :---
key | String | 将保存为的资源唯一标识。请参见[关键概念：键值对](http://developer.qiniu.com/docs/v6/api/overview/concepts.html#key-value)。 
isa | InputStreamAt | 待上传的本地文件。 
extra | PutExtra | 额外配置项，用于精确控制上传行为。请参见[高级设置](#upload-config)。 
ret | JSONObjectRet | 开发者需实现该接口以获取上传进度和上传结果。<br>若上传成功，该接口中的`onSuccess()`方法将被调用。否则`onFailure()`方法将被调用。 `onProgress()`会在文件上传量发生更改的时候被调用，而且处于MainThread环境之中，可以直接操作ProgressBar之类的进度提示控件。

表单上传的示例代码请参见SDK示例中[MyActivity.doUpload()](https://github.com/qiniu/android-sdk/blob/develop/src/com/qiniu/demo/MyActivity.java)方法的实现。

<a name="chunked-upload"></a>
### 分片上传

顾名思义，分片上传会将一个文件划分为多个指定大小的数据块，分别上传。分片上传的关键价值在于可更好的适应不稳定的网络环境，以及成功上传超大的文件。分片上传功能也是实现断点续上传功能的基础。

开发者可以通过调用`ResumableIO.put()`方法以分片形式上传一个文件。该方法签名和`IO.put()`一致。

分片上传的示例代码请参见SDK示例中[MyResumableActivity.doResumableUpload()](https://github.com/qiniu/android-sdk/blob/develop/src/com/qiniu/demo/MyResumableActivity.java)方法的实现。

<a name="resumable-upload"></a>
### 断点续上传

开发者可以基于分片上传机制实现断点续上传功能。

```
class ResumableIO {
    public static void put(String key, 
				InputStreamAt isa, 
				com.qiniu.resumableio.PutExtra extra, 
				JSONObjectRet ret);
}
```
具体用法和`IO.put`的类似。

<a name="upload-concurrency"></a>
### 上传中的并发性

分片上传机制也提供了对一个文件并发上传的能力。

目前本SDK的实现采用AsyncTask来进行异步操作，而Android系统底层默认是使用单线程来串行运行所有的AsyncTask，所以如果需要真正意义上的多线程上传，需要将AsyncTask放入线程池, 详细操作请参考[这里](http://developer.android.com/reference/android/os/AsyncTask.html)。

<a name="upload-config"></a>
### 高级设置

几种不同的上传类型都支持上传时的参数配置，使用一个统一的`PutExtra`类型来管理。除了需要指定几个最基本的上传参数（哪个文件以及上传到哪里等）外，开发者还可以通过制定一系列高级参数来灵活的控制上传的后续动作和通过变量来传递一些特定信息。

设置方法请参见[`PutExtra`](https://github.com/qiniu/android-sdk/blob/develop/src/com/qiniu/resumableio/PutExtra.java)。开发者可以在调用`ResumableIO.put()`前往`PutExtra.params`中添加对应的参数即可，例如：

```
extra.params = new HashMap<String, String>();
extra.params.put("x:a", "bb"); // 设置一个自定义变量
```
<a name="response"></a>
#### 上传后续动作

关于上传后可以进行哪些后续动作，请查看[上传后续动作](http://developer.qiniu.com/docs/v6/api/overview/up/response/)。上传的后续动作的设置通过在`PutExtra`中设置相应的参数来进行。对于Android开发者而言，这些后续动作都有各自的合适使用场景：[自定义响应内容](http://developer.qiniu.com/docs/v6/api/overview/up/response/response-body.html)，[变量](http://developer.qiniu.com/docs/v6/api/overview/up/response/vars.html)，[数据预处理](http://developer.qiniu.com/docs/v6/api/overview/up/response/persistent-op.html)，[回调](http://developer.qiniu.com/docs/v6/api/overview/up/response/callback.html)。对这些后续动作的合理组合使用可以大幅降低业务流程复杂度，并提升业务的健壮性。

开发者可以在生成上传凭证前通过配置上传策略以控制上传后续动作，该工作在业务服务器端进行，因此非本SDK的功能范畴。完整的内容请参见[上传策略规格](http://developer.qiniu.com/docs/v6/api/reference/security/put-policy.html)。

<a name="var"></a>
#### 变量

变量分为魔法变量和自定义变量，可帮助开发者快速的在客户端、业务服务器、云存储服务之间传递资源元信息。关于变量的作用，请参见[变量](http://developer.qiniu.com/docs/v6/api/overview/up/response/vars.html)。

如同上面已经给出的示例，如果开发者需要配置变量，只需在调用上传方法前在`PutExtra.params`中添加相应的参数即可。

<a name="download"></a>
## 下载文件

该SDK并未提供下载文件相关的功能接口，因为文件下载是一个标准的HTTP GET过程。开发者只需理解资源URI的组成格式即可非常方便的构建资源URI，并在必要的时候加上下载凭证，即可使用HTTP GET请求获取相应资源。

具体做法请参见[资源下载](http://developer.qiniu.com/docs/v6/api/overview/dn/download.html)和[资源下载的安全机制](http://developer.qiniu.com/docs/v6/api/overview/dn/security.html)。

<a name="thread-safety"></a>
## 线程安全性

Android 一般的情况下会使用一个主线程来控制UI，非主线程无法控制UI，在Android4.0+之后必须不能在主线程完成网络请求，
该SDK是根据以上的使用场景设计，所有网络的操作均使用AsyncTask异步运行，所有回调函数又都回到了主线程（onSuccess, onFailure, onProgress）,在回调函数内可以直接操作UI控件。
如果您没有额外使用`new Thread()`等命令，该SDK将不会发生线程安全性问题。
