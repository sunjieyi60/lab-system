# 智慧班牌服务端接口规范

## 概述

本文档定义智慧班牌系统的服务端接口规范，包括：
- **RSocket 接口**：用于设备长连接通信（双向实时通信）
- **HTTP 接口**：用于管理后台调用（RESTful API）

---

## 一、RSocket 接口（设备通信）

### 1.1 基础连接管理

#### 1.1.1 设备注册
- **路由**：`device.register`
- **交互模式**：REQUEST_RESPONSE
- **请求类型**：`RegisterRequest`
- **响应类型**：`RegisterResponse`
- **业务说明**：
  - 设备首次连接时注册
  - 服务端保存设备连接（用于后续主动推送）
  - 返回设备配置信息
- **请求字段**：
  - `uuid`: String - 设备唯一标识
  - `laboratoryId`: Long - 所属实验室ID
- **响应字段**：
  - `uuid`: String - 设备唯一标识
  - `config`: Config - 设备配置信息

#### 1.1.2 心跳保活
- **路由**：`device.heartbeat`
- **交互模式**：FIRE_AND_FORGET
- **请求类型**：`Heartbeat`
- **业务说明**：
  - 设备定期发送心跳（建议30秒）
  - 服务端更新设备在线状态
  - 可在响应中携带配置更新标记
- **请求字段**：
  - `uuid`: String - 设备标识
  - `interval`: Integer - 心跳间隔（秒）
  - `configUpdated`: Boolean - 客户端是否已更新配置
- **响应字段**（服务端主动推送时）：
  - `configUpdated`: Boolean - 是否有新配置
  - `newConfig`: Config - 新配置（如有）

#### 1.1.3 连接断开通知
- **路由**：`device.disconnect`
- **交互模式**：FIRE_AND_FORGET
- **业务说明**：
  - 设备主动断开时通知服务端
  - 服务端清理连接资源

---

### 1.2 门禁控制（核心功能）

#### 1.2.1 开门请求（设备主动上报）
- **路由**：`door.open.request`
- **交互模式**：REQUEST_RESPONSE
- **请求类型**：`OpenDoorRequest`
- **响应类型**：`OpenDoorResponse`
- **业务说明**：
  - 设备人脸识别/密码验证成功后上报
  - 服务端记录开门日志
  - 返回是否允许开门
- **请求字段**：
  - `type`: Enum - 开门方式（FACE/PASSWORD/REMOTE）
  - `verifyInfo`: String - 验证信息（人脸ID/密码）
  - `duration`: Integer - 开门持续时间（秒）
- **响应字段**：
  - `success`: Boolean - 是否允许开门
  - `code`: Integer - 结果码
  - `message`: String - 结果消息

#### 1.2.2 远程开门（服务端主动下发）
- **路由**：`client.door.open`
- **交互模式**：REQUEST_RESPONSE
- **请求类型**：`OpenDoorRequest`
- **响应类型**：`OpenDoorResponse`
- **业务说明**：
  - 管理员通过后台远程开门
  - 服务端主动推送到指定设备
  - 设备执行开门并返回结果
- **触发方式**：HTTP 调用后，服务端通过 ConnectionManager 主动推送

---

### 1.3 课表管理

#### 1.3.1 课表同步请求
- **路由**：`schedule.sync`
- **交互模式**：REQUEST_RESPONSE
- **请求类型**：`UpdateScheduleRequest`
- **响应类型**：`UpdateScheduleResponse`
- **业务说明**：
  - 设备开机或定时同步课表
  - 返回当前生效的课表数据
- **请求字段**：
  - `scheduleVersion`: Long - 本地课表版本
- **响应字段**：
  - `success`: Boolean - 同步是否成功
  - `currentVersion`: Long - 当前课表版本
  - `schedules`: List<CourseSchedule> - 课表数据

#### 1.3.2 课表更新推送（服务端主动）
- **路由**：`client.schedule.update`
- **交互模式**：REQUEST_RESPONSE
- **业务说明**：
  - 后台修改课表后主动推送到设备
  - 设备更新本地课表

---

### 1.4 设备配置管理

#### 1.4.1 配置更新推送（服务端主动）
- **路由**：`client.config.update`
- **交互模式**：REQUEST_RESPONSE
- **请求类型**：`UpdateConfigRequest`
- **响应类型**：`UpdateConfigResponse`
- **业务说明**：
  - 后台修改配置后主动推送到设备
  - 支持立即生效或定时生效
- **请求字段**：
  - `config`: Config - 新配置
  - `immediate`: Boolean - 是否立即生效
  - `version`: Long - 配置版本号
- **响应字段**：
  - `success`: Boolean - 更新是否成功
  - `currentVersion`: Long - 当前配置版本

#### 1.4.2 配置同步请求
- **路由**：`config.sync`
- **交互模式**：REQUEST_RESPONSE
- **业务说明**：
  - 设备请求获取最新配置
  - 用于开机同步或手动刷新

---

### 1.5 人脸库管理

#### 1.5.1 人脸库更新推送（服务端主动）
- **路由**：`client.face.update`
- **交互模式**：REQUEST_RESPONSE
- **请求类型**：`UpdateFaceLibraryRequest`
- **响应类型**：`UpdateFaceLibraryResponse`
- **业务说明**：
  - 后台增删人脸后主动推送到设备
  - 支持全量更新和增量更新
- **请求字段**：
  - `updateType`: Enum - 更新类型（FULL/INCREMENTAL）
  - `libraryVersion`: Long - 人脸库版本
  - `faces`: List<FaceItem> - 人脸数据列表
  - `deletedFaceIds`: List<String> - 删除的人脸ID列表
- **响应字段**：
  - `success`: Boolean - 更新是否成功
  - `processedCount`: Integer - 成功处理的人脸数量
  - `currentVersion`: Long - 当前人脸库版本

---

## 二、HTTP 接口（管理后台）

### 2.1 设备管理

#### 2.1.1 获取在线设备数量
```http
GET /api/device/online/count
```
**响应**：
```json
{
  "count": 10
}
```

#### 2.1.2 获取在线设备列表
```http
GET /api/device/online
```
**响应**：
```json
{
  "count": 10,
  "devices": ["uuid1", "uuid2", ...]
}
```

#### 2.1.3 检查设备在线状态
```http
GET /api/device/online/{uuid}
```
**响应**：
```json
{
  "uuid": "xxx",
  "online": true,
  "laboratoryId": 1
}
```

#### 2.1.4 获取设备详细信息
```http
GET /api/device/online/details
```
**响应**：设备详情列表

---

### 2.2 远程控制

#### 2.2.1 远程开门
```http
POST /api/device/{uuid}/door/open
Content-Type: application/json

{
  "type": "REMOTE",
  "duration": 10,
  "reason": "管理员远程开门"
}
```
**业务逻辑**：
1. 检查设备是否在线
2. 通过 ConnectionManager 获取连接
3. 调用 `server.sendTo(uuid, openDoorRequest)`
4. 返回开门结果
---

### 2.3 配置管理

#### 2.3.1 更新设备配置
```http
POST /api/device/{uuid}/config
Content-Type: application/json

{
  "facePrecision": 0.85,
  "timeout": 30,
  "password": "123456"
}
```
**业务逻辑**：
1. 更新数据库配置
2. 如设备在线，通过 RSocket 推送更新
3. 返回更新结果

#### 2.3.2 获取设备配置
```http
GET /api/device/{uuid}/config
```

---

### 2.4 课表管理

#### 2.4.1 推送课表到设备
```http
POST /api/device/{uuid}/schedule/push
Content-Type: application/json

{
  "scheduleVersion": 100,
  "schedules": [ ... ]
}
```

#### 2.4.2 触发课表同步
```http
POST /api/device/{uuid}/schedule/sync
```
**业务逻辑**：通知设备主动拉取最新课表

---

### 2.5 人脸库管理

#### 2.5.1 推送人脸库更新
```http
POST /api/device/{uuid}/face/push
Content-Type: application/json

{
  "updateType": "INCREMENTAL",
  "faces": [ ... ],
  "deletedFaceIds": [ ... ]
}
```

#### 2.5.2 触发人脸库同步
```http
POST /api/device/{uuid}/face/sync
```

---

## 三、数据模型

### 3.1 通用响应格式

所有 RSocket 响应使用统一格式：

```java
public class Message<T> {
    private String route;        // 路由
    private Type type;           // 消息类型
    private String from;         // 发送方
    private String to;           // 接收方
    private T data;              // 业务数据
    private Status status;       // 状态（code, msg）
    private Instant timestamp;   // 时间戳
}
```

### 3.2 关键枚举

```java
enum Type {
    REQUEST_RESPONSE,  // 请求-响应
    FIRE_AND_FORGET,   // 单向发送
    REQUEST_STREAM,    // 请求流
    REQUEST_CHANNEL    // 双向流
}

enum OpenType {
    FACE,      // 人脸识别
    PASSWORD,  // 密码
    REMOTE     // 远程开门
}

enum UpdateType {
    FULL,         // 全量更新
    INCREMENTAL   // 增量更新
}
```

---

## 四、接口优先级

### P0 - 核心功能（必须实现）
1. `device.register` - 设备注册
2. `device.heartbeat` - 心跳保活
3. `door.open.request` - 开门请求
4. `client.config.update` - 配置更新推送
5. `client.door.open` - 远程开门

### P1 - 重要功能（建议实现）
1. `schedule.sync` - 课表同步
2. `client.schedule.update` - 课表更新推送
3. `client.face.update` - 人脸库更新
4. `stream.device.status` - 设备状态流

### P2 - 增强功能（可选实现）
1. `channel.command` - 实时命令通道
2. `stream.notification` - 系统通知流
3. `device.disconnect` - 断开通知

---

## 五、错误码定义

| 错误码 | 说明 |
|-------|------|
| 10000 | 成功 |
| 10001 | 失败（通用）|
| 10002 | 设备不在线 |
| 10003 | 设备未注册 |
| 10004 | 验证失败 |
| 10005 | 配置版本冲突 |
| 10006 | 课表版本过期 |

---

## 六、使用示例

### 场景1：设备注册流程
```java
// 设备发送
RegisterRequest request = new RegisterRequest();
request.setUuid("device-001");
request.setLaboratoryId(1L);

Message<RegisterRequest> message = Message.<RegisterRequest>builder()
    .route("device.register")
    .data(request)
    .build();

Mono<Message<RegisterResponse>> response = clientRequester
    .route("device.register")
    .data(message)
    .retrieveMono(new ParameterizedTypeReference<>() {});
```

### 场景2：管理员远程开门
```java
// HTTP 接口接收请求
@PostMapping("/{uuid}/door/open")
public Mono<Map<String, Object>> openDoor(@PathVariable String uuid) {
    // 1. 检查设备在线
    if (!connectionManager.isOnline(uuid)) {
        return Mono.just(Map.of("error", "设备不在线"));
    }
    
    // 2. 构建开门命令
    OpenDoorRequest request = new OpenDoorRequest();
    request.setType(OpenDoorRequest.OpenType.REMOTE);
    request.setDuration(10);
    
    // 3. 通过 Api 发送
    return api.sendTo(uuid, request)
        .map(response -> Map.of(
            "success", response.getData().isSuccess(),
            "message", "远程开门成功"
        ));
}
```

---

*文档版本：1.0*
*更新日期：2026-03-21*
