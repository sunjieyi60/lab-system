# 智慧班牌 RSocket 模块

基于 RSocket 协议的智慧班牌设备通信模块，支持设备注册、配置下发、心跳保活和双工通信。

## 核心功能

1. **设备上电注册** - 设备上电后通过 RSocket 注册并获取配置
2. **配置下发** - 注册成功后自动下发班牌配置（密码、人脸精度等）
3. **心跳保活** - 设备定期发送心跳维持在线状态
4. **双工通信** - 支持服务器主动向设备推送具体业务命令

## 技术栈

- RSocket (TCP/WebSocket)
- Spring Boot RSocket Starter
- Project Reactor
- MyBatis Plus + Redis (异步缓存)

## 端口配置

| 协议 | 端口 | 说明 |
|------|------|------|
| RSocket TCP | 7000 | 设备长连接端口 |
| HTTP | 8084 | 管理接口端口 |

## RSocket 路由

### 基础通信

| 路由 | 模式 | 请求 | 响应 | 说明 |
|------|------|------|------|------|
| `device.register` | Request-Response | `Message<RegisterRequest>` | `Message<RegisterResponse>` | 设备注册 |
| `device.heartbeat` | Request-Response | `Message<HeartbeatPayload>` | `Message<HeartbeatPayload>` | 心跳 |

### 业务命令

| 路由 | 模式 | 请求 | 响应 | 说明 |
|------|------|------|------|------|
| `device.door.open` | Request-Response | `Message<OpenDoorRequest>` | `Message<OpenDoorResponse>` | 开门 |
| `device.config.update` | Request-Response | `Message<UpdateConfigRequest>` | `Message<UpdateConfigResponse>` | 更新配置 |
| `device.reboot` | Request-Response | `Message<RebootRequest>` | `Message<RebootResponse>` | 重启设备 |
| `device.screenshot` | Request-Response | `Message<ScreenshotRequest>` | `Message<ScreenshotResponse>` | 远程截图 |
| `device.schedule.update` | Request-Response | `Message<UpdateScheduleRequest>` | `Message<UpdateScheduleResponse>` | 更新课表 |
| `device.face-library.update` | Request-Response | `Message<UpdateFaceLibraryRequest>` | `Message<UpdateFaceLibraryResponse>` | 更新人脸库 |

### 数据流

| 路由 | 模式 | 说明 |
|------|------|------|
| `device.stream` | Request-Stream | 服务器推送流 |
| `device.channel` | Request-Channel | 双向通道 |

## 数据流包装

所有通信使用 `Message<T>` 包装具体业务 DTO：

```java
public class Message<T> {
    private Type type;          // REQUEST_RESPONSE / FIRE_AND_FORGET / REQUEST_STREAM / REQUEST_CHANNEL
    private Long from;          // 发送方ID
    private Long to;            // 接收方ID
    private T payload;          // 具体业务 DTO
    private Status status;      // 状态
    private Instant timestamp;  // 时间戳
}
```

### 业务 DTO 列表

| DTO | 说明 |
|-----|------|
| `RegisterRequest` / `RegisterResponse` | 设备注册 |
| `HeartbeatPayload` | 心跳请求/响应 |
| `OpenDoorRequest` / `OpenDoorResponse` | 开门控制 |
| `UpdateConfigRequest` / `UpdateConfigResponse` | 配置更新 |
| `RebootRequest` / `RebootResponse` | 设备重启 |
| `ScreenshotRequest` / `ScreenshotResponse` | 远程截图 |
| `UpdateScheduleRequest` / `UpdateScheduleResponse` | 课表更新 |
| `UpdateFaceLibraryRequest` / `UpdateFaceLibraryResponse` | 人脸库更新 |

## 设备注册流程

### 1. 设备发送注册请求

```java
RegisterRequest request = new RegisterRequest();
request.setUuid("CTT001");
request.setLaboratoryId(200L);

Message<RegisterRequest> message = new Message<>();
message.setType(Message.Type.REQUEST_RESPONSE);
message.setPayload(request);

Message<RegisterResponse> response = rsocketRequester
    .route("device.register")
    .data(message)
    .retrieveMono(new ParameterizedTypeReference<Message<RegisterResponse>>() {})
    .block();

// 获取配置
Config config = response.getPayload().getConfig();
```

### 2. 服务器响应配置

```java
RegisterResponse response = new RegisterResponse();
response.setDeviceDbId(1L);
response.setConfig(Config.Default());  // 密码、人脸精度、超时时间等
```

## HTTP 管理接口

### 设备状态查询

```bash
# 获取在线设备数量
GET /api/device/online/count

# 获取在线设备列表
GET /api/device/online

# 检查设备在线状态
GET /api/device/online/{deviceDbId}

# 获取在线设备详细信息
GET /api/device/online/details
```

### 业务控制接口

```bash
# 远程开门
POST /api/device/{deviceDbId}/door/open
Content-Type: application/json

{
    "type": "REMOTE",
    "verifyInfo": "admin",
    "duration": 5
}

# 更新设备配置
POST /api/device/{deviceDbId}/config
Content-Type: application/json

{
    "password": "654321",
    "facePrecision": 0.9,
    "timeout": 60,
    "unit": "SECONDS"
}

# 重启设备
POST /api/device/{deviceDbId}/reboot
Content-Type: application/json

{
    "delaySeconds": 10,
    "reason": "系统维护"
}

# 远程截图
POST /api/device/{deviceDbId}/screenshot
Content-Type: application/json

{
    "format": "jpg",
    "quality": 80
}

# 更新课表
POST /api/device/{deviceDbId}/schedule
Content-Type: application/json

{
    "scheduleVersion": 1,
    "effectiveTime": "2024-01-01T00:00:00Z"
}

# 更新人脸库
POST /api/device/{deviceDbId}/face-library
Content-Type: application/json

{
    "updateType": "INCREMENTAL",
    "libraryVersion": 1,
    "faces": [...]
}
```

### 广播接口

```bash
# 向实验室广播开门指令
POST /api/device/lab/{laboratoryId}/door/open
Content-Type: application/json

{
    "type": "REMOTE",
    "duration": 5
}

# 向实验室广播更新课表
POST /api/device/lab/{laboratoryId}/schedule
Content-Type: application/json

{
    "scheduleVersion": 1,
    "schedules": [...]
}
```

## 数据库表

```sql
CREATE TABLE `class_time_table_device` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `uuid` VARCHAR(64) NOT NULL COMMENT '班牌唯一编号',
    `config` JSON NULL COMMENT '班牌的配置信息',
    `laboratory_id` BIGINT NULL COMMENT '关联的实验室id',
    `status` VARCHAR(16) DEFAULT 'offline',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT(1) DEFAULT 0
);
```

## 运行

```bash
# 编译
mvn clean install -DskipTests

# 运行
mvn spring-boot:run
```

## 配置

```yaml
spring:
  rsocket:
    server:
      port: 7000
      transport: tcp
  
  datasource:
    url: jdbc:mysql://localhost:3306/lab_sys4
    username: root
    password: labsystem
  
  data:
    redis:
      host: localhost
      port: 6379
```
