
# Differences from 7.6.4 to 8.0.0

注：
- 更改dns解析自定义方式：通过GlobalConfiguration进行配置
- 更改并发上传配置方式：通过Configuration配置，useConcurrentResumeUpload配置是否开启并发上传 & concurrentTaskCount配置并发上传任务数量


## 1. com.qiniu.android.storage
### 1.1 change class
**remove:**
- com.qiniu.android.storage.ResumeUploaderFast


### 1.2 change file
#### 1.2.1 com.qiniu.android.storage.UploadManager
**remove:**
- `UploadManager(Configuration config, int multitread)`
- `UploadManager(Recorder recorder, int multitread)`
- `UploadManager(Recorder recorder, KeyGenerator keyGen, int multitread)`

#### 1.2.2 com.qiniu.android.storage.Builder 
**add:**
- `property` : `allowBackupHost`
- `property` : `concurrentTaskCount`
- `property` : `useConcurrentResumeUpload`

**remove:**
- `func` : `UploadManager(Configuration config, int multitread)`
- `func` : `UploadManager(Recorder recorder, int multitread)`
- `func` : `UploadManager(Recorder recorder, KeyGenerator keyGen, int multitread)`
- `property` : `dns`
- `property` : `dnsCacheTimeMs`




## 2. com.qiniu.android.bigdata.client
### 2.1 change class
**add:** 
- com.qiniu.android.bigdata.client.Client
- com.qiniu.android.bigdata.client.PostArgs


## 3. com.qiniu.android.collect
### 3.1 change class
**add:** 
- com.qiniu.android.collect.ReportConfig
- com.qiniu.android.collect.ReportItem
- com.qiniu.android.collect.UploadInfoReporter

**remove:**
- com.qiniu.android.collect.Config
- com.qiniu.android.collect.UploadInfo
- com.qiniu.android.collect.UploadInfoCollector
- com.qiniu.android.collect.UploadInfoCollector.RecordMsg
- com.qiniu.android.collect.UploadInfoElement
- com.qiniu.android.collect.UploadInfoElement.BlockInfo
- com.qiniu.android.collect.UploadInfoElement.ReqInfo
- com.qiniu.android.collect.UploadInfoElement.UploadQuality
- com.qiniu.android.collect.UploadInfoElementCollector


## 4. com.qiniu.android.common
### 4.1 change class
**add:** 
- com.qiniu.android.common.ZoneInfo.UploadServerGroup
- com.qiniu.android.common.Config

### 4.2 change file
#### 4.2.1 com.qiniu.android.common.Zone.QueryHandler
**add:**
- `func` : `void complete(int code, ResponseInfo responseInfo, UploadRegionRequestMetrics metrics)`

**remove:**
- `func` : `void onSuccess()`
- `func` : `void onFailure(int reason)`


#### 4.2.2 com.qiniu.android.common.FixedZone
**add:**
- `func` : `FixedZone(java.lang.String[] upDomains, java.lang.String[] ioDomains)`
- `func` : `FixedZone(ZonesInfo zonesInfo)`
- `func` : `ZonesInfo getZonesInfo(UpToken token)`
- `func` : `static FixedZone    localsZoneInfo()`
- `func` : `void    preQuery(UpToken token, Zone.QueryHandler completeHandler)`

**remove:**
- `property` : arrayzone0
- `property` : arrayzone1
- `property` : arrayzone2
- `property` : arrayzoneAs0
- `property` : arrayzoneNa0
- `func` : `static ZoneInfo createZoneInfo(java.lang.String[] upDomains)`
- `func` : `void    frozenDomain(java.lang.String upHostUrl)`
- `func` : `static java.util.List<ZoneInfo> getZoneInfos()`
- `func` : `boolean preQuery(LogHandler logHandler, java.lang.String token)`
- `func` : `void    preQuery(LogHandler logHandler, java.lang.String token, Zone.QueryHandler complete)`
- `func` : `java.lang.String    upHost(java.lang.String upToken, boolean useHttps, java.lang.String frozenDomain)`



## 5 com.qiniu.android.http
### 5.1 change class
**remove:**
- com.qiniu.android.http.Client
- com.qiniu.android.http.Client.ResponseTag
- com.qiniu.android.http.CountingRequestBody
- com.qiniu.android.http.DnsPrefetcher
- com.qiniu.android.http.HttpEventListener
- com.qiniu.android.http.MultipartBody
- com.qiniu.android.http.MultipartBody.Builder
- com.qiniu.android.http.MultipartBody.Part
- com.qiniu.android.http.PostArgs

### 5.2 change file
#### 5.2.1 com.qiniu.android.http.ResponseInfo
**add:**
- `LocalIOError`
- `static MaliciousResponseError`
- `message`
- `static NetworkSlow`
- `static NetworkSSLError`
- `static PasrseError `
- `responseHeader`
- `static ResquestSuccess `

**remove:**
- `bytes_sent`
- `duration`
- `requests_count`
- `totalSize`


## 6 com.qiniu.android.http.dns
### 6.1 change class
**add:**
- com.qiniu.android.http.dns.Dns
- com.qiniu.android.http.dns.IDnsNetworkAddress
- com.qiniu.android.http.dns.DnsCacheFile
- com.qiniu.android.http.dns.DnsPrefetcher
- com.qiniu.android.http.dns.DnsPrefetchTransaction
- com.qiniu.android.http.dns.HappyDns
- com.qiniu.android.http.dns.SystemDns


## 7 com.qiniu.android.http.metrics
### 7.1 change class
**add:**
- com.qiniu.android.http.metrics.UploadRegionRequestMetrics
- com.qiniu.android.http.metrics.UploadSingleRequestMetrics
- com.qiniu.android.http.metrics.UploadTaskMetrics


## 8 com.qiniu.android.http.request
### 8.1 change class
**add:**
- com.qiniu.android.http.request.IRequestClient
- com.qiniu.android.http.request.IRequestClient.CompleteHandler
- com.qiniu.android.http.request.IRequestClient.Progress
- com.qiniu.android.http.request.IUploadServer
- com.qiniu.android.http.request.RequestTransaction.RequestCompleteHandler
- com.qiniu.android.http.request.IUploadRegion
- com.qiniu.android.http.request.Request
- com.qiniu.android.http.request.RequestTransaction
- com.qiniu.android.storage.UploadFileInfo
- com.qiniu.android.storage.UploadFileInfo.UploadBlock
- com.qiniu.android.storage.UploadFileInfo.UploadData


## 9 com.qiniu.android.http.request.handler
### 9.1 change class
**add:**
- com.qiniu.android.http.request.handler.CheckCancelHandler
- com.qiniu.android.http.request.handler.RequestProgressHandler
- com.qiniu.android.http.request.handler.RequestShouldRetryHandler


## 10 com.qiniu.android.http.request.httpclient
### 10.1 change class
**add:**
- com.qiniu.android.http.request.httpclient.ByteBody
- com.qiniu.android.http.request.httpclient.CountingRequestBody
- com.qiniu.android.http.request.httpclient.MultipartBody
- com.qiniu.android.http.request.httpclient.MultipartBody.Builder
- com.qiniu.android.http.request.httpclient.MultipartBody.Part
- com.qiniu.android.http.request.httpclient.SystemHttpClient


## 11 com.qiniu.android.http.request.serverRegion
### 11.1 change class
**add:**
- com.qiniu.android.http.request.serverRegion.UploadDomainRegion
- com.qiniu.android.http.request.serverRegion.UploadServer
- com.qiniu.android.http.request.serverRegion.UploadServerFreezeManager


## 12 com.qiniu.android.transaction
### 12.1 change class
**add:**
- com.qiniu.android.transaction.TransactionManager
- com.qiniu.android.transaction.TransactionManager.Transaction


## 13 com.qiniu.android.transaction
### 13.1 change class
**add:**
- com.qiniu.android.transaction.GroupTaskThread.GroupTaskCompleteHandler
- com.qiniu.android.transaction.GroupTaskThread
- com.qiniu.android.transaction.LogUtil
- com.qiniu.android.transaction.Utils
- com.qiniu.android.transaction.Wait
