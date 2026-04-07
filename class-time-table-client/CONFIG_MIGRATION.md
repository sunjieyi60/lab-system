# 班牌客户端配置迁移文档

## 概述

本次更新将班牌客户端的配置管理从旧的 `AppConfig` 模型迁移到新的服务端对齐模型，使用 `DeviceProfile`（设备档案）和 `DeviceRuntimeConfig`（运行时配置）替代。

## 主要变更

### 1. 配置模型变更

#### 旧模型（已弃用）
- `AppConfig`: 包含 deviceId, backendServer, weatherConfig, heziConfig, doorOpenConfig
- 配置存储在 SharedPreferences 和 assets/config.json 中
- 有兜底配置，配置不存在时从 assets 加载默认值

#### 新模型

**DeviceProfile（设备档案）** - 本地保存
```kotlin
data class DeviceProfile(
    val uuid: String,              // 设备唯一标识，首次启动生成，不可修改
    var laboratoryId: Long?,       // 关联的实验室ID，需用户配置
    var serverAddress: ServerAddress,  // 服务器地址，需用户配置
    var status: String             // 设备状态
)
```

**DeviceRuntimeConfig（运行时配置）** - 服务端下发
```kotlin
data class DeviceRuntimeConfig(
    val password: String,          // 密码开门密码
    val facePrecision: Float,      // 人脸识别精度
    val timeout: Int,              // 验证超时时间
    val timeoutUnit: TimeUnit      // 超时时间单位
)
```

### 2. 配置管理器变更

| 旧管理器 | 新管理器 | 说明 |
|---------|---------|------|
| `AppConfigManager` | `DeviceProfileManager` | 管理设备档案（UUID、实验室ID、服务器地址） |
| `ConfigObservable` | `DeviceProfileObservable` | 设备档案状态观察 |
| - | `DeviceRuntimeConfigObservable` | 运行时配置状态观察（服务端下发） |
| - | `WeatherConfigManager` | 独立的天气本地配置管理 |

### 3. 新增配置页面

**DeviceProfileConfigActivity** - 设备配置页面
- 显示设备 UUID（只读）
- 配置关联实验室ID
- 配置服务器地址（host、port、timeout）
- 首次启动或配置不完整时自动弹出

### 4. 配置流程变更

#### 首次启动流程
```
MainActivity.onCreate()
    │
    ▼
检查相机权限
    │
    ▼
checkDeviceProfile()
    │
    ├──► 配置不完整 ──► DeviceProfileConfigActivity
    │                      │
    │                      │ 配置完成保存
    │◄─────────────────────┘
    │
    ▼
initializeAfterPermissions()
    │
    ├──► DeviceProfileManager.loadProfile()
    │
    ├──► DeviceProfileObservable.updateProfile()
    │
    └──► 继续初始化其他组件
```

#### 运行时配置下发流程（RSocket）
```
RSocket 连接建立
    │
    ▼
发送注册请求（携带 uuid, laboratoryId）
    │
    ▼
服务端返回 REGISTER_ACK（携带 DeviceRuntimeConfig）
    │
    ▼
DeviceRuntimeConfigObservable.updateConfig()
    │
    ▼
UI 组件自动更新（密码、人脸精度等）
```

### 5. 开门方式配置更新

| 开门方式 | 配置来源 | 更新方式 |
|---------|---------|---------|
| 密码开门 | `DeviceRuntimeConfigObservable.getPassword()` | 服务端下发 |
| 人脸开门 | `DeviceRuntimeConfigObservable.getFacePrecision()` | 服务端下发 |
| 超时时间 | `DeviceRuntimeConfigObservable.getTimeout()` | 服务端下发 |

### 6. UI 更新

- **TopBar**: 新增设置按钮，点击打开配置页面
- **LabDashboardScreen**: 添加 `onOpenSettings` 回调
- **MainActivity**: 集成配置检查和配置页面跳转

## 文件变更清单

### 新增文件
1. `config/DeviceProfile.kt` - 设备档案数据类
2. `config/DeviceRuntimeConfig.kt` - 运行时配置数据类
3. `config/DeviceProfileManager.kt` - 设备档案管理器
4. `config/DeviceProfileObservable.kt` - 设备档案状态观察
5. `config/DeviceProfileConfigActivity.kt` - 配置页面
6. `config/WeatherLocalConfig.kt` - 本地天气配置

### 修改文件
1. `MainActivity.kt` - 添加配置检查和配置页面集成
2. `ui/LabDashboardScreen.kt` - 添加设置回调
3. `ui/TopBar.kt` - 添加设置按钮
4. `network/rsocket/RSocketClientManager.kt` - 使用新的配置观察
5. `ui/dialog/PwdOpenUiProvider.kt` - 使用运行时配置
6. `ui/dialog/FaceOpenUiProvider.kt` - 使用运行时配置
7. `ui/component/weather_ifno/LocalWeatherDataSource.kt` - 使用天气配置管理器
8. `ui/component/weather_ifno/WeatherDataSourceFactory.kt` - 更新工厂方法
9. `AndroidManifest.xml` - 注册配置 Activity

### 已删除的旧文件
- `config/AppConfig.kt` - 旧配置数据类
- `config/AppConfigManager.kt` - 旧配置管理器
- `config/ConfigObservable.kt` - 旧配置观察器

这些文件已被新的设备档案和运行时配置替代。

## 配置存储位置

| 配置类型 | 存储路径 | 文件格式 |
|---------|---------|---------|
| 设备档案 | `/data/data/{package}/files/device_profile.json` | JSON |
| 运行时配置 | 仅内存，不持久化 | - |
| 天气数据 | 仅内存，不持久化 | - |

## 注意事项

1. **UUID 不可修改**: 设备 UUID 在首次启动时自动生成，之后不可更改
2. **配置不存在宁可失败**: 取消了 assets/config.json 兜底，必须完成配置才能使用
3. **运行时配置不下发**: 服务端通过 RSocket 注册响应下发运行时配置
4. **服务器地址独立**: 天气配置中的位置信息不再与服务器地址混淆

## 后续工作

1. 实现 RSocket 注册请求发送（携带 uuid 和 laboratoryId）
2. 实现 REGISTER_ACK 响应处理，解析并保存运行时配置
3. 实现运行时配置的服务端推送更新机制
4. 在配置页面添加服务器连接测试功能

## 天气功能说明

### 当前实现
天气功能仅保留 UI 展示和状态观察机制：

**保留的文件：**
- `ui/component/weather_ifno/WeatherInfo.kt` - 天气数据模型
- `ui/component/weather_ifno/WeatherState.kt` - 天气状态封装
- `ui/component/weather_ifno/WeatherInfoInLineText.kt` - 天气 UI 展示

**天气数据流（后续实现）：**
```
RSocket 服务端推送
        │
        ▼
RemoteDataObservable.updateWeather()
        │
        ▼
WeatherState (StateFlow)
        │
        ▼
WeatherInfoInLineText (UI 展示)
```

### 已移除的文件
- `IWeatherDataSource.kt` - 天气数据源接口
- `LocalWeatherDataSource.kt` - 本地天气数据源实现
- `WeatherDataSourceFactory.kt` - 天气数据源工厂
- `WeatherPusher.kt` - 天气推送器
- `WeatherLocalConfig.kt` - 天气本地配置

### 后续实现建议
1. 在 RSocket 连接建立后，向服务端订阅天气数据
2. 服务端通过 RSocket 推送天气数据到客户端
3. 客户端调用 `RemoteDataObservable.updateWeather()` 更新 UI
