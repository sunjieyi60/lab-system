# FaceAI 人脸识别 SDK API 文档

> **版本**: Android V20260115  
> **联系方式**: 
> - Email: FaceAISDK.Service@gmail.com  
> - 微信: FaceAISDK

---

## 目录

1. [简要说明](#简要说明)
2. [集成方式](#集成方式)
3. [人脸搜索-硬件配置要求](#人脸搜索-硬件配置要求)
4. [API 分类简要说明](#api-分类简要说明)
5. [人脸录入 API](#人脸录入-api)
6. [人脸识别 API](#人脸识别-api)
7. [人脸搜索 API](#人脸搜索-api)
8. [常见问答说明](#常见问答说明)

---

## 简要说明

FaceAI SDK 包括**人脸识别**、**活体检测**、**人脸录入检测**以及 **1:N 人脸搜索**。可快速集成离线实现 Android 端侧相关功能。

### 核心特性

| 特性 | 说明 |
|------|------|
| 系统支持 | Android [5, 15] |
| 运行模式 | 完全离线，无需联网，不上传不保存任何人脸敏感信息 |
| 活体检测 | 支持张嘴、微笑、眨眼、摇头、点头（随机两种组合验证） |
| 摄像头支持 | 系统前后摄像头、UVC 协议双目摄像头 |
| RGB 静默活体 | 需配备宽动态值 > 105dB 的成像清晰抗逆光摄像头 |

### 性能统计（1 万人脸库）

| 设备型号 | 启动速度 | 搜索速度 |
|---------|---------|---------|
| 小米 13 | 79 ms | 66 ms |
| RK3568-SM5 | 686 ms | 520 ms |
| 华为 P8 | 798 ms | 678 ms |
| 联想 Pad2024 | 245 ms | 197 ms |

> **注意**: SDK 不限制人脸库容量，但强烈建议**分组**以减少人脸搜索匹配误差。

### 资源链接

- **测试人脸图**: [3000 张测试人脸](https://pan.baidu.com/s/1RfzJlc-TMDb0lQMFKpA-tQ?pwd=Face) 提取码: `Face`
- **APK 体验**: [下载地址](https://www.pgyer.com/faceVerify)

---

## 集成方式

### Gradle 集成

SDK 托管在 Maven Central Repository，可通过 Gradle 在线集成：

```groovy
implementation 'io.github.faceaisdk:Android:VERSION'
```

> **权限要求**: SDK 仅需相机权限

### 演示项目

- **GitHub Demo**: [FaceAISDK_Android](https://github.com/FaceAISDK/FaceAISDK_Android)
- **环境要求**: 
  - AGP 版本 8.13（非强制）
  - Java 17+
  - Kotlin 1.9.22+

---

## 人脸搜索-硬件配置要求

### 一、芯片、存储和内存要求

| 项目 | 要求 |
|------|------|
| 芯片架构 | ARM 架构 CPU（不支持 X86） |
| CPU | 4 核 2.0 GHz 的 64 位 CPU 足够 |

### 二、摄像头参数建议（人脸搜索识别功能）

| 参数 | 建议值 | 说明 |
|------|--------|------|
| 分辨率 | 200 万像素（1080P） | 默认使用 640×480 格式分析数据 |
| 宽动态范围 (WDR) | ≥95 dB | 室内环境，均匀充足光 |
| | ≥105 dB | 基础要求，应对日常逆光场景 |
| | ≥120 dB | 严苛环境，强逆光、户外直射 |
| 低照度性能 | ≤0.01 Lux（F1.2） | 彩色模式，支持弱光环境 |
| 帧率 | 30 fps | 确保动态人脸无拖影，不低于 25 fps |

> **提示**: 如无法使用红外双目摄像头，请使用补光感应灯补充脸部光线。

### 三、摄像头硬件等级说明

| 等级 | 说明 |
|------|------|
| `LEVEL_FULL` | 完整手动控制，高帧率录制，性能强劲。**推荐使用本等级以上** |
| `LEVEL_3` | YUV 重处理，多流联动，FULL 的超集，最新旗舰机会配置本类型摄像头 |

> **说明**: 摄像头管理源码 CameraX 已暴露在 SDK 集成 Demo 中，可根据定制硬件平台特性自由修改完善，也可完全自定义。

---

## API 分类简要说明

> **版本更新**: 从 2025.11.24 版本开始，API 大大简化。

### 人脸录入

| API | 功能说明 |
|-----|---------|
| `BaseImageDispose` | 检测摄像头数据流中人脸角度合适且活体后，返回裁剪好的人脸 bitmap |
| `FaceAISDKEngine.croppedBitmap2Feature` | 将裁剪处理好的人脸 bitmap 转人脸特征值字符串 |
| `FaceAISDKEngine.saveCroppedFaceImage` | （可选）保存裁剪处理好的人脸图到本地 |

**人脸特征存储方式**:

| 方式 | API | 说明 |
|------|-----|------|
| 1:1 人脸识别 | `MMKV.encode(faceID, faceFeature)` | Key-Value 存储，key 为个人唯一 ID |
| 1:N 人脸搜索 | `FaceSearchFeatureManger.insertFaceFeature` | 保存在内置数据库，支持分组和标记搜索 |

> **不推荐**: `Image2FaceFeature.getFaceFeatureByBitmap` - 检测一张照片中是否含有人脸并异步返回裁剪矫正好的人脸部分 Bitmap 和对应的人脸特征向量值字符串（因为没有严格检测人脸合规）。

### 人脸识别

| API | 功能说明 |
|-----|---------|
| `MMKV.decodeString(faceID)` | 1:1 人脸识别从 MMKV 取出 faceID 对应人脸特征 |
| `FaceProcessBuilder` | 人脸识别中各种参数设置 Builder |
| `FaceVerifyUtils.goVerifyWithImageProxy` | 通过视频预览流参数 ImageProxy 进行识别 |
| `FaceVerifyUtils.goVerifyWithBitmap` | 自定义管理摄像头视频帧转 bitmap 进行识别 |
| `FaceVerifyUtils.goVerifyWithNV21Bytes` | 自定义摄像头 NV21 Byte[] 流识别 |
| `FaceVerifyUtils.goVerifyWithIR` | 通过红外（可选）和 RGB Bitmap 进行人脸识别 |
| `FaceVerifyUtils.evaluateFaceSimi` | 两张静态人脸图中人脸的相似度 |

### 人脸搜索

| API | 功能说明 |
|-----|---------|
| `SearchProcessBuilder` | 人脸搜索中各种参数设置 Builder |
| `FaceSearchEngine.runSearchWithImageProxy` | 通过摄像头预览流 ImageProxy 进行识别 |
| `FaceSearchEngine.runSearchWithBitmap` | 自定义管理摄像头视频帧转 bitmap 进行识别 |
| `FaceSearchEngine.runSearchWithIR` | 通过红外（可选）和 RGB Bitmap 进行人脸识别 |
| `FaceSearchEngine.getFeatureSearcher().search(faceFeature)` | 判断是否有相似度很高的人脸数据存在 |
| `FaceSearchFeatureManger` | 人脸搜索人脸向量特征库增删改查管理 |
| `FaceSearchImagesManger` | 人脸搜索人脸图片库增删改查管理 |

---

## 人脸录入 API

### 1. BaseImageDispose

**功能**: 处理 SDK 通过摄像头预览流获取角度正常的人脸图

```kotlin
/**
 * @param context 上下文 context
 * @param mode 人脸角度检测匹配模式
 *             2 - 精确模式：人脸要正对摄像头，严格要求
 *             1 - 快速模式：允许人脸方位可以有一定的偏移
 *             0 - 简单模式：允许人脸方位可以「较大」的偏移
 * @param callBack 检测匹配成功回调，返回裁剪矫正好后的人脸图
 */
baseImageDispose(Context mContext, int mode, BaseImageCallBack callBack)
```

**使用示例**:
```kotlin
BaseImageDispose(context, 1, object : BaseImageCallBack {
    override fun onSuccess(croppedBitmap: Bitmap) {
        // 获取到裁剪好的人脸图，可进行后续特征提取
    }
    
    override fun onError(errorCode: Int, errorMsg: String) {
        // 处理错误
    }
})
```

---

### 2. FaceAISDKEngine.croppedBitmap2Feature

**功能**: 将裁剪后的人脸图片直接转换为 Base64 编码的特征字符串

```kotlin
/**
 * @param croppedBitmap 裁剪好的人脸图片
 * @return Base64 格式的特征字符串，可用于存储或网络传输。如果转换失败则返回 null
 */
fun croppedBitmap2Feature(croppedBitmap: Bitmap): String
```

**使用示例**:
```kotlin
val faceFeature = FaceAISDKEngine.croppedBitmap2Feature(croppedBitmap)
if (faceFeature != null) {
    // 保存到 MMKV 或数据库
    MMKV.defaultMMKV().encode("user_001", faceFeature)
}
```

---

### 3. FaceAISDKEngine.saveCroppedFaceImage

**功能**: 将裁剪后的人脸图片保存到本地文件

```kotlin
/**
 * @param croppedBitmap 需要保存的人脸图片
 * @param pathName 存储路径
 * @param fileName 文件名
 */
fun saveCroppedFaceImage(croppedBitmap: Bitmap, pathName: String, fileName: String)
```

**使用示例**:
```kotlin
FaceAISDKEngine.saveCroppedFaceImage(
    croppedBitmap, 
    "/sdcard/FaceAI/faces/", 
    "user_001.jpg"
)
```

---

### 4. FaceSearchFeatureManger.insertFaceFeature

**功能**: 插入或更新单个人脸特征数据（1:N 人脸搜索）

```kotlin
/**
 * 如果具有相同 faceID 的记录已存在，则会覆盖更新
 * 
 * @param faceID 人脸的唯一标识符
 * @param faceFeature Base64 编码的人脸特征字符串
 * @param updateTime 数据更新的时间戳
 * @param tag 可选的标签信息，用于分类或备注
 * @param group 可选的分组信息，用于将人脸划分到不同的组
 */
fun insertFaceFeature(
    faceID: String,
    faceFeature: String,
    updateTime: Long,
    tag: String?,
    group: String?
)
```

**使用示例**:
```kotlin
FaceSearchFeatureManger.getInstance().insertFaceFeature(
    faceID = "user_001",
    faceFeature = faceFeature,
    updateTime = System.currentTimeMillis(),
    tag = "员工",
    group = "技术部"
)
```

---

## 人脸识别 API

### 1. FaceProcessBuilder

**功能**: 人脸识别中各种参数设置的 Builder 模式

> **详细参数请参考**: [FaceVerificationActivity.java](https://github.com/FaceAISDK/FaceAISDK_Android/blob/publish/faceAILib/src/main/java/com/faceAI/demo/SysCamera/verify/FaceVerificationActivity.java)

**常用配置**:
```kotlin
val faceProcessBuilder = FaceProcessBuilder()
    .setThreshold(0.88f)           // 设置识别阈值 [0.75, 0.95]
    .setLivenessType(LivenessType.ACTION)  // 活体检测类型
    .setActions(arrayOf(Action.BLINK, Action.SMILE))  // 动作组合
    // ... 更多参数
```

---

### 2. MMKV.decodeString

**功能**: 1:1 人脸识别从 MMKV 取出 faceID 对应人脸特征

```kotlin
val faceFeature = MMKV.defaultMMKV().decodeString(faceID)
```

---

### 3. FaceVerifyUtils.goVerifyWithImageProxy

**功能**: 通过视频预览流参数 ImageProxy 进行识别

```kotlin
/**
 * @param imageProxy CameraX 的暴露的 ImageProxy
 * @param margin 暂时不用，预留参数
 */
public void goVerifyWithImageProxy(ImageProxy imageProxy, int margin)

/**
 * 简化版本
 * @param imageProxy CameraX 的暴露的 ImageProxy
 */
public void goVerifyWithImageProxy(ImageProxy imageProxy)
```

**使用示例**:
```kotlin
// CameraX 预览回调
imageAnalysis.setAnalyzer(executor) { imageProxy ->
    faceVerifyUtils.goVerifyWithImageProxy(imageProxy)
}
```

---

### 4. FaceAISDKEngine.evaluateFaceSimiByBitmap

**功能**: 比较两张裁剪好的人脸图片的相似度

```kotlin
/**
 * @param croppedBitmap1 第一张人脸图片（Bitmap 格式）
 * @param croppedBitmap2 第二张人脸图片（Bitmap 格式）
 * @return 返回一个浮点数，值域为 [0.0, 1.0]，表示两张人脸的相似度。值越大，表示越相似
 */
fun evaluateFaceSimiByBitmap(croppedBitmap1: Bitmap, croppedBitmap2: Bitmap): Float
```

**使用示例**:
```kotlin
val similarity = FaceAISDKEngine.evaluateFaceSimiByBitmap(bitmap1, bitmap2)
if (similarity > 0.88f) {
    // 认为是同一个人
}
```

---

## 人脸搜索 API

### 1. SearchProcessBuilder

**功能**: 人脸搜索中各种参数设置的 Builder 模式

> **详细参数请参考**: [FaceSearch1NActivity.java](https://github.com/FaceAISDK/FaceAISDK_Android/blob/publish/faceAILib/src/main/java/com/faceAI/demo/SysCamera/search/FaceSearch1NActivity.java)

**常用配置**:
```kotlin
val searchProcessBuilder = SearchProcessBuilder()
    .setThreshold(0.88f)           // 设置搜索阈值
    .setCallBackAllMatch(true)     // 返回所有匹配结果
    .setGroup("技术部")             // 指定搜索分组
    // ... 更多参数
```

---

### 2. FaceSearchEngine.runSearchWithImageProxy

**功能**: 摄像头预览处理搜索（使用 CameraX ImageProxy）

```kotlin
/**
 * @param imageProxy 实时采集的数据流 CameraX 的暴露的 ImageProxy
 * @param margin 预留参数
 */
fun runSearchWithImageProxy(imageProxy: ImageProxy, margin: Int)
```

---

### 3. FaceSearchEngine.runSearchWithBitmap

**功能**: 摄像头预览处理搜索（使用 Bitmap）

```kotlin
/**
 * @param rgbBitmap 摄像头实时采集的数据流转为 Bitmap，用于人脸搜索，活体检测（可选）
 */
fun runSearchWithBitmap(rgbBitmap: Bitmap)
```

---

### 4. FaceSearchEngine.runSearchWithIR

**功能**: 通过红外和 RGB 摄像头进行人脸搜索

```kotlin
/**
 * @param irBitmap 实时 IR 摄像头采集的数据流转为 Bitmap，用于 IR 活体检测
 * @param rgbBitmap 实时采集的数据流转为 Bitmap，用于人脸搜索
 */
fun runSearchWithIR(irBitmap: Bitmap, rgbBitmap: Bitmap)
```

---

### 5. FaceSearchEngine.getFeatureSearcher().search

**功能**: 单次搜索人脸特征数据库中相似度最高的 face 以及对应 ID（仅用于添加数据检验重复）

```kotlin
/**
 * @param faceFeature 人脸特征字符串
 * @return FeatureSearchResult 搜索结果
 */
override fun search(faceFeature: String): FeatureSearchResult
```

**使用示例**:
```kotlin
val result = FaceSearchEngine.getInstance()
    .getFeatureSearcher(context)
    .search(faceFeature)

if (result.faceID != null && result.score > 0.88f) {
    // 发现重复人脸
    Log.d("Search", "重复人脸: ${result.faceID}, 相似度: ${result.score}")
}
```

---

### 6. FaceSearchFeatureManger 人脸特征库管理

#### 6.1 insertFaceFeature

**功能**: 插入或更新单个人脸特征数据

```kotlin
fun insertFaceFeature(
    faceID: String,
    faceFeature: String,
    updateTime: Long,
    tag: String?,
    group: String?
)
```

#### 6.2 insertOrUpdateFaceEmbedding

**功能**: 保存 name (unique id) 对应的人脸搜索特征向量编码到人脸数据库

```kotlin
/**
 * @param name 人脸 FaceID String
 * @param faceEmbedding 人脸特征向量编码（FloatArray）
 */
fun insertOrUpdateFaceEmbedding(name: String, faceEmbedding: FloatArray)
```

#### 6.3 insertFeatures（批量插入）

**功能**: 批量插入或更新人脸搜索特征数据到本地数据库（后台协程执行，不阻塞主线程）

```kotlin
/**
 * @param list 包含多个 FaceSearchFeature 对象的列表
 */
fun insertFeatures(list: List<FaceSearchFeature>)

/**
 * 用于跨平台插件 uniapp RN 传递数据
 * @param jsonStrList JSON 字符串列表
 */
fun insertFeatures(jsonStrList: String)
```

#### 6.4 clearAllFaceFaceFeature

**功能**: 清空数据库中所有的人脸特征数据（后台协程执行）

```kotlin
fun clearAllFaceFaceFeature()
```

#### 6.5 queryFaceFeatureByID

**功能**: 根据 faceID 查询单个人脸的特征数据（同步操作）

```kotlin
/**
 * @param faceID 要查询的人脸的唯一标识符
 * @return 如果找到，则返回对应的 FaceSearchFeature 对象；否则返回 null
 */
fun queryFaceFeatureByID(faceID: String): FaceSearchFeature?
```

#### 6.6 queryAllFaceFaceFeature

**功能**: 查询数据库中所有的人脸特征数据（同步操作）

```kotlin
/**
 * @return 包含数据库中所有人脸数据的 FaceSearchFeature 列表。如果数据库为空，则返回一个空列表
 */
fun queryAllFaceFaceFeature(): List<FaceSearchFeature>
```

---

### 7. 人脸搜索结果回调

#### 7.1 onFaceMatched（返回所有匹配结果）

**功能**: 匹配到的大于 Threshold 的所有结果

> **注意**: 需要 `SearchProcessBuilder.setCallBackAllMatch(true)` 才有数据返回，否则默认是空

```kotlin
/**
 * @param matchedResults 匹配结果列表（已按照相似度降序排列）
 * @param searchBitmap 场景图，可以用来做使用记录 log
 */
@Override
public void onFaceMatched(List<FaceSearchResult> matchedResults, Bitmap searchBitmap) {
    // 已经按照降序排列，可以弹出一个列表框让用户选择
    Log.d("onFaceMatched", matchedResults.toString());
}
```

#### 7.2 onMostSimilar（返回最相似结果）

**功能**: 最相似的人脸搜索识别结果，得分最高

```kotlin
/**
 * @param faceID 人脸 ID
 * @param score 相似度值
 * @param bitmap 场景图，可以用来做使用记录 log
 */
@Override
public void onMostSimilar(String faceID, float score, Bitmap bitmap)
```

---

## 常见问答说明

### 1. 如何提升 1:N 人脸识别的准确率？

参考专题文章: [提升人脸识别准确率](https://mp.weixin.qq.com/s/G2dvFQraw-TAzDRFIgdobA)

> **重要提示**: SDK **不可**用于金融、高安全级别场景。

**避免搜索匹配错误的方法**:

```kotlin
// 匹配到的大于 Threshold 的所有结果，如有多个结果场景允许的话可弹框让用户选择
// SearchProcessBuilder setCallBackAllMatch(true) 才有数据返回，否则默认是空
@Override
public void onFaceMatched(List<FaceSearchResult> matchedResults, Bitmap searchBitmap) {
    // 已经按照相似度降序排列
    Log.d("onFaceMatched", matchedResults.toString());
}
```

---

### 2. uniApp 原生插件支持

参考 UTS demo 项目集成: [FaceAISDK_uniapp_UTS](https://github.com/FaceAISDK/FaceAISDK_uniapp_UTS)

目前已支持:
- iOS / Android 1:1 人脸识别
- 人脸录入等功能

细节可以修改原生部分代码重新打包实现。

---

### 3. 人脸识别的阈值设置说明

```kotlin
.setThreshold(0.88f)  // 阈值范围限制 [0.75, 0.95]
```

| 阈值设置 | 效果 | 建议场景 |
|---------|------|---------|
| 0.75 | 较宽松，可能同性别亲属也能通过 | 低安全性场景 |
| 0.88 | 平衡，推荐值 | 一般场景 |
| 0.95 | 严格，需要高质量录入和摄像头 | 高安全性场景 |

> **注意**: 阈值设置越大结果可信度越高，但不是越高越好。设置越高需要你录入的人脸品质高以及摄像头成像能力也高（宽动态 > 105 dB 可抗逆光）。

---

### 4. FaceAI SDK 版权说明

FaceAI SDK 使用**开源 + 自研封装**实现：
- 非虹软（试用每年要重新激活）
- 非 Face++
- 非商汤商业方案二次包装

**所有功能都可充分体验验证**

更多咨询: 📧 FaceAISDK.Service@gmail.com

---

## 附录：完整 API 速查表

### 人脸录入

| API | 类 | 说明 |
|-----|---|------|
| `baseImageDispose(context, mode, callback)` | BaseImageDispose | 检测并裁剪人脸 |
| `croppedBitmap2Feature(bitmap)` | FaceAISDKEngine | Bitmap 转特征值 |
| `saveCroppedFaceImage(bitmap, path, name)` | FaceAISDKEngine | 保存人脸图片 |
| `insertFaceFeature(...)` | FaceSearchFeatureManger | 插入人脸特征 |

### 人脸识别

| API | 类 | 说明 |
|-----|---|------|
| `FaceProcessBuilder` | - | 参数配置 Builder |
| `goVerifyWithImageProxy(imageProxy)` | FaceVerifyUtils | ImageProxy 识别 |
| `goVerifyWithBitmap(bitmap)` | FaceVerifyUtils | Bitmap 识别 |
| `goVerifyWithNV21Bytes(bytes)` | FaceVerifyUtils | NV21 字节流识别 |
| `goVerifyWithIR(ir, rgb)` | FaceVerifyUtils | 红外 + RGB 识别 |
| `evaluateFaceSimiByBitmap(b1, b2)` | FaceAISDKEngine | 比较两张人脸相似度 |

### 人脸搜索

| API | 类 | 说明 |
|-----|---|------|
| `SearchProcessBuilder` | - | 参数配置 Builder |
| `runSearchWithImageProxy(imageProxy)` | FaceSearchEngine | ImageProxy 搜索 |
| `runSearchWithBitmap(bitmap)` | FaceSearchEngine | Bitmap 搜索 |
| `runSearchWithIR(ir, rgb)` | FaceSearchEngine | 红外 + RGB 搜索 |
| `getFeatureSearcher().search(feature)` | FaceSearchEngine | 单次特征搜索 |
| `insertFaceFeature(...)` | FaceSearchFeatureManger | 插入特征 |
| `insertOrUpdateFaceEmbedding(...)` | FaceSearchFeatureManger | 插入向量 |
| `insertFeatures(list)` | FaceSearchFeatureManger | 批量插入 |
| `clearAllFaceFaceFeature()` | FaceSearchFeatureManger | 清空数据库 |
| `queryFaceFeatureByID(id)` | FaceSearchFeatureManger | 查询单个 |
| `queryAllFaceFaceFeature()` | FaceSearchFeatureManger | 查询所有 |

---

*文档版本: 2026-01-30*  
*基于 FaceAI SDK Android V20260115*
