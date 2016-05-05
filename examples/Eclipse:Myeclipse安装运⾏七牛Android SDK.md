使⽤Eclipse/Myeclipse运行Android程序⾸先都需要安装配置Android SDK的,最新版的 Android SDK为Android 6.0版本,为了⽅方便测试,还必要安配置虚拟机AVD,关于这部分配置⺴上都有详细的教程,就直接跳过这一步。

可以参考打包好的[**Android Demo**](http://7xm540.com1.z0.glb.clouddn.com/AndroidDemo.zip)

以下以Android API 22为例⼦介绍整个安装运行过程。<br/>
1. ⾸先新建⼀一个Android项⺫:

![1](http://7xn15i.com1.z0.glb.clouddn.com/1)

2.下载[qiniu-android-sdk-VERSION.jar/aar](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.qiniu%22%20AND%20a%3A%22qiniu-android-sdk%22)包、下载[happy-dns-VERSION.jar/aar](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.qiniu%22%20AND%20a%3A%22happy-dns%22)(qiniu- android-sdk:7.0.7开始依赖此包)包、下载[android-async-http 1.4.6](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.qiniu%22%20AND%20a%3A%22happy-dns%22)及以上版本(_注1.4.9的版本jar包有兼容性问题,需要编译运行在Android SDK 23版本及以上,所以建议使⽤1.4.6~1.4.8版本_)导⼊到项⺫中。

**注意**:导⼊入的路径是Android的libs⺫录下,直接从外⾯复制进去就可以了。

![2](http://7xn15i.com1.z0.glb.clouddn.com/2)

3.编辑MainActivity.java主⻚面⽂件以及activity_main.xml布局⽂文件和AndroidMainfest.xml, 这个Demo运⾏行逻辑是先在Android⼿手机里面从网上下载两张图⽚片保存到Android虚拟机⾥面,然后上传到七牛空间。

附：[MainActivity.java](http://7xm540.com1.z0.glb.clouddn.com/MainActivity.java)和[activity_main.xml](http://7xm540.com1.z0.glb.clouddn.com/activity_main.xml)

![3](http://7xn15i.com1.z0.glb.clouddn.com/3)

* 注：因为该程序需要网络权限，所以需要在AndroidMainfest.xml配置uses-permission节点授予网络权限：
加一条 <uses-permission android:name="android.permission.INTERNET”/>就可以了，不然会报错：
![4](http://7xn15i.com1.z0.glb.clouddn.com/4)

修改如下：

![5](http://7xn15i.com1.z0.glb.clouddn.com/5)

4.修改完后运行程序：

![6](http://7xn15i.com1.z0.glb.clouddn.com/6)
运行成功后的主界面是这样的：

![7](http://7xn15i.com1.z0.glb.clouddn.com/7)
点击上传,可以看到上传成功并返回hash，key等信息

![8](http://7xn15i.com1.z0.glb.clouddn.com/8)

在七牛空间可以正常访问到这个文件：<br/>
http://7xm540.com1.z0.glb.clouddn.com/new-thread_86290dbd-4131-4ec6-8d37-e3a4793c42db


