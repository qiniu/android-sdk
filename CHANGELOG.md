## CHANGE LOG

### v6.1.0
2014-09-02 issue [#67](https://github.com/qiniu/android-sdk/pull/67) 

-  [#64] 1.中断上传；2.uri不能直接转换为File时，转换为InputStream处理； 3.重新分片上传，可设置断点记录；4.重构InputStreamAt,MultipartEntity,CallBack等；


### v6.0.5
2014-07-20 issue [#61](https://github.com/qiniu/android-sdk/pull/61)

- [#60] bug 成功返回时body 如果是null 会出错

### v6.0.4
2014-07-20 issue [#58](https://github.com/qiniu/android-sdk/pull/58)

- [#50] 统一UA
- [#53] 使用自定义的QiniuException
- [#56] [#57] 多host上传重试

### v6.0.3
2014-07-11 issue [#49](https://github.com/qiniu/android-sdk/pull/49)

- [#45] block count 计数修正
- [#47] file Uri 修正
- [#48] 调整上传默认为upload.qiniu.com

### v6.0.2
2014-04-15 issue [#43](https://github.com/qiniu/android-sdk/pull/43)

- [#41] gradle build, travis


### v6.0.1
2014-04-03 issue [#40](https://github.com/qiniu/android-sdk/pull/40)

- [#35] fix bugs and close idle connection
- [#36] 增加连接超时处理


### v6.0.0

增加 SDK 实现规范
增加 docs.qiniu.com 实现规范
=======
2013-07-04 issue [#20](https://github.com/qiniu/android-sdk/pull/20)

- 遵循 [sdkspec v6.0.3](https://github.com/qiniu/sdkspec/tree/v6.0.3)

### v1.3.0

2013-03-27 issue [#9](https://github.com/qiniu/android-sdk/pull/9)

- 修正 HttpClient 类的不正确使用。
