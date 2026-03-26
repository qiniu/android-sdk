# 七牛云 Android SDK — 开发维护指南

七牛云官方 Android SDK，Maven 坐标 `com.qiniu:qiniu-android-sdk`，Min SDK 14（Android 4.0+），Java 1.8，MIT 许可。

本文档面向 SDK 维护者和贡献者，帮助理解项目结构、架构模式和开发流程。
SDK 的 API 使用说明请参考 `README.md` 和 `doc/` 目录。

## 项目结构

单模块项目，核心代码在 `library/` 模块中。

```
android-sdk/
├── library/                          # Android Library 模块（AAR）
│   ├── src/main/java/com/qiniu/android/
│   │   ├── storage/                  # 上传管理（核心）
│   │   ├── http/                     # HTTP 客户端与网络层
│   │   ├── common/                   # 区域/Zone 管理、常量定义
│   │   ├── transaction/              # 内部事务调度（定时任务）
│   │   ├── collect/                  # 上传质量数据采集与上报
│   │   ├── utils/                    # 工具类
│   │   └── bigdata/                  # 大数据采集（可选）
│   ├── src/androidTest/              # Android Instrumented 测试
│   └── build.gradle                  # 模块构建配置
├── .github/workflows/                # CI/CD
├── doc/                              # 迁移指南等文档
├── build.gradle                      # 根构建配置
├── settings.gradle                   # 项目设置
├── mvn_push.gradle                   # Maven Central 发布配置
├── CHANGELOG.md                      # 版本变更记录
└── README.md                         # 使用文档
```

### 核心包

| 包 | 职责 |
|---|------|
| `storage` | 上传入口（UploadManager）、配置（Configuration）、表单上传、分片上传、断点续传、并发上传 |
| `http` | HTTP 客户端抽象（IRequestClient）、OkHttp 实现、请求/响应封装、ResponseInfo 错误码 |
| `http.dns` | DNS 预解析（DnsPrefetcher）、HappyDns、SystemDns、UdpDns、HttpDns |
| `http.metrics` | 请求指标采集（UploadTaskMetrics、UploadSingleRequestMetrics） |
| `http.serverRegion` | 上传域名管理、域名冻结与恢复 |
| `http.connectCheck` | 网络连通性检查 |
| `common` | Zone 区域管理（FixedZone 固定区域、AutoZone 自动检测）、Constants 版本号 |
| `transaction` | TransactionManager 单例，定时器驱动的内部事务调度（DNS 刷新、配置同步等） |
| `collect` | UploadInfoReporter 上传质量上报、ReportItem 数据结构 |
| `storage.serverConfig` | 服务端配置拉取与监控（ServerConfigMonitor） |
| `utils` | AsyncRun 异步执行、Etag/Crc32/MD5 校验、Cache 磁盘缓存、SingleFlight 并发合并 |
| `bigdata` | 可选的大数据采集管道（Pipeline、Client） |

## 核心架构模式

### UploadManager 入口模式

所有上传操作通过 `UploadManager` 发起，构造时初始化全局基础设施：

```
UploadManager(Configuration)
  → TransactionManager.start()          // 启动事务调度定时器
  → DnsPrefetchTransaction.addDns...()  // 注册 DNS 预解析事务
  → ServerConfigMonitor.startMonitor()  // 注册服务配置监控事务
```

Deprecated 构造函数（无参、Recorder 参数）均委托到 `UploadManager(Configuration)`。

### 上传策略自动选择

根据文件大小和配置自动选择上传方式：

```
文件大小 <= putThreshold  →  FormUpload（表单上传）
文件大小 > putThreshold   →  PartsUpload 或 ConcurrentResumeUpload（分片上传）
```

选择并发分片还是串行分片由 `Configuration.useConcurrentResumeUpload` 控制。

### TransactionManager 事务调度

单例模式，内部维护一个 1 秒间隔的 Timer，轮询执行注册的事务：

- 事务分两种类型：Normal（执行一次）和 Time（定时重复执行）
- 通过 `start()` / `stop()` 控制定时器生命周期
- 通过 `addTransaction()` 注册事务，`removeTransaction()` 移除
- `destroyResource()` 清空所有事务并停止定时器

### DNS 预解析

`DnsPrefetchTransaction` 注册多个 DNS 相关事务到 TransactionManager：

- `addDnsLocalLoadTransaction` — 从本地缓存加载 DNS 记录
- `setDnsCheckWhetherCachedValidTransactionAction` — 定时检查 DNS 缓存有效性
- `addDnsCheckAndPrefetchTransaction` — 检查并预解析上传域名

### 区域（Zone）管理

- `FixedZone` — 预定义区域（华东 z0、华北 z1、华南 z2、华东二 cn-east-2 等）
- `AutoZone` — 根据 UpToken 自动查询上传区域

### 质量上报

上传完成后通过 `UploadInfoReporter` 上报质量数据（耗时、速度、错误类型等），上报本身也通过 TransactionManager 事务异步执行。

## 构建与依赖

### 构建环境

- Gradle + Android Gradle Plugin 7.3.1
- Java 11（编译工具链）/ Java 1.8（源码兼容性）
- Compile SDK: 33, Min SDK: 14
- NDK: 20.0.5594570

### 核心依赖

| 依赖 | 用途 |
|------|------|
| `com.squareup.okhttp3:okhttp:4.9.1` | HTTP 客户端 |
| `com.qiniu:happy-dns:2.0.1` | 自定义 DNS 解析 |
| `org.conscrypt:conscrypt-android:2.5.3` | SSL/TLS 安全提供者 |

### 常用 Gradle 命令

```bash
# 编译
./gradlew build

# 运行 Instrumented 测试（需要模拟器或设备）
./gradlew connectedCheck

# 生成测试覆盖率报告
./gradlew :library:createDebugAndroidTestCoverageReport

# 打包 JAR
./gradlew releaseJar
```

### 导出 AAR 到本地

```bash
# 方式一：直接构建 AAR
./gradlew :library:assembleRelease
# 产物路径：library/build/outputs/aar/library-release.aar

# Debug 版本
./gradlew :library:assembleDebug
# 产物路径：library/build/outputs/aar/library-debug.aar

# 方式二：发布到本地 Maven 仓库（~/.m2/repository/）
./gradlew :library:publishToMavenLocal
# 其他项目可通过 mavenLocal() 仓库引用：
#   repositories { mavenLocal() }
#   dependencies { implementation 'com.qiniu:qiniu-android-sdk:8.9.2' }
```

本地集成 AAR 的方式：将 `library-release.aar` 复制到目标项目的 `libs/` 目录，然后在 `build.gradle` 中添加：

```gradle
repositories {
    flatDir { dirs 'libs' }
}
dependencies {
    implementation(name: 'library-release', ext: 'aar')
    // 同时需要声明 SDK 的传递依赖
    implementation 'com.squareup.okhttp3:okhttp:4.9.1'
    implementation 'com.qiniu:happy-dns:2.0.1'
    implementation 'org.conscrypt:conscrypt-android:2.5.3'
}
```

## 测试规范

### 测试框架

- JUnit 4 + AndroidJUnit4Runner
- Instrumented 测试（运行在模拟器/设备上）
- 测试基类：`BaseTest`（提供 `wait()` 等辅助方法）

### 测试目录

```
library/src/androidTest/java/com/qiniu/android/
├── storage/          # 上传功能测试（表单、分片、并发、取消、重试等）
├── http/             # HTTP 客户端、DNS、连通性测试
├── common/           # Zone、Token、GlobalConfiguration 测试
├── transaction/      # TransactionManager 测试
├── collect/          # 上报功能测试
├── bigdata/          # 大数据采集测试
└── utils/            # 工具类测试
```

### 测试配置

- `TestConfig` 类中包含测试用的 AK/SK/Token/Bucket 配置
- 测试需要 Android 模拟器（CI 使用 API 22, x86_64, Nexus 6）

## CI 流程

CI 在 push 和 pull_request 时触发（`.github/workflows/ci-test.yml`）：

- 运行环境：macOS-12, Java 11 (Temurin)
- Android 模拟器：API 22, Nexus 6, x86_64
- 执行步骤：`connectedCheck` → `build` → 覆盖率报告 → `connectedAndroidTest`
- 覆盖率上传至 codecov.io

## 版本管理

### 版本号定义

版本号的**唯一来源**是 `Constants.java`：

```java
// library/src/main/java/com/qiniu/android/common/Constants.java
public static final String VERSION = "8.9.2";
```

`library/build.gradle` 通过正则从 `Constants.java` 提取版本号，自动设置 `versionName` 和 `versionCode`。

### 版本发布检查

发布 tag 时（`.github/workflows/version-check.yml`），自动验证三处版本号一致：

1. `Constants.java` — `public static final String VERSION = "<version>";`
2. `CHANGELOG.md` — `## <version>` 条目
3. `README.md` — 版本引用

### 发布

通过 `mvn_push.gradle` 发布到 Maven Central。

## 公开 API 概览

### 核心类

| 类 | 说明 |
|---|------|
| `UploadManager` | 上传入口，提供 `put()` / `syncPut()` 方法 |
| `Configuration` | 上传配置（通过 `Configuration.Builder` 构建） |
| `UploadOptions` | 上传选项（自定义参数、进度回调、取消信号） |
| `ResponseInfo` | 上传响应（状态码、错误信息、请求 ID） |
| `FixedZone` / `AutoZone` | 区域配置 |
| `GlobalConfiguration` | 全局 SDK 配置（DNS、日志等） |

### 回调接口

| 接口 | 说明 |
|------|------|
| `UpCompletionHandler` | `complete(String key, ResponseInfo info, JSONObject response)` |
| `UpProgressHandler` | `progress(String key, double percent)` |
| `UpCancellationSignal` | `isCancelled()` 取消信号 |

## 重要约定

1. **版本号三处同步** — 发布时确保 `Constants.java`、`CHANGELOG.md`、`README.md` 版本一致
2. **所有构造函数委托到主构造函数** — `UploadManager` 的 deprecated 构造函数最终都委托到 `UploadManager(Configuration)`，新增初始化逻辑只需加在主构造函数中
3. **TransactionManager 需显式启动** — 调用 `addTransaction()` 前需确保 `start()` 已被调用，否则事务不会执行
4. **Instrumented 测试** — 所有测试是 Android Instrumented 测试，需要模拟器或真机运行，不支持纯 JVM 运行
5. **最低 SDK 14** — 代码需兼容 Android 4.0+，避免使用高版本 API（如需使用，添加 `@TargetApi` 注解和版本检查）
6. **注释使用中文** — 项目 Javadoc 注释以中文为主
