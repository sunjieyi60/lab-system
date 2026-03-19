# class-time-table-rsocket

智慧班牌 RSocket 通信模块，支持 P2P（对等网络）双工通信，CS 角色可切换。

## 特性

- **P2P 架构**：无严格的服务端/客户端之分，班牌设备和管理端都是对等节点
- **双工通信**：支持 Request/Response、Fire-and-Forget、Request Stream、Request Channel
- **角色切换**：班牌可作为客户端连接管理端，也可作为服务端被管理端直连调试
- **响应式编程**：基于 Project Reactor，支持背压控制
- **类型安全**：使用 JSON 序列化，提供类型安全的 RPC 接口
- **枚举规范**：使用 Command 和 StatusCode 枚举替代魔法值

## 核心组件

```
┌─────────────────────────────────────────────────────────────┐
│                    RSocketPeerConnector                      │
│  ┌──────────────┐              ┌──────────────┐            │
│  │ connectAsClient │  ←────→  │ listenAsServer │            │
│  └──────────────┘              └──────────────┘            │
└─────────────────────────────────────────────────────────────┘
                            │
                    ┌───────┴───────┐
                    ▼               ▼
        ┌─────────────────┐  ┌─────────────────┐
        │ ClassTimeTableDevice │  │   ManagerServer   │
        │   (班牌设备端)      │  │   (管理服务端)     │
        └─────────────────┘  └─────────────────┘
```

## 枚举定义

### Command 命令枚举

```java
// 设备管理
Command.REGISTER                    // 设备注册/上线
Command.HEARTBEAT                   // 心跳上报
Command.GET_DEVICE_STATUS           // 获取设备状态

// 课表相关
Command.GET_CURRENT_TIME_TABLE      // 获取当前课表
Command.GET_TIME_TABLE_LIST         // 获取课表列表
Command.SUBSCRIBE_TIME_TABLE_UPDATES // 订阅课表更新

// 人脸识别
Command.PUSH_FACE_DATA              // 下发人脸数据
Command.REMOVE_FACE                 // 删除人脸
Command.GET_DEVICE_FACE_LIST        // 获取设备人脸列表
Command.REPORT_ACCESS               // 上报访问记录
Command.BATCH_PUSH_FACES            // 批量下发人脸

// 配置管理
Command.GET_DEVICE_CONFIG           // 获取设备配置
Command.UPDATE_DEVICE_CONFIG        // 更新设备配置
Command.REBOOT_DEVICE               // 远程重启设备
Command.REMOTE_OPEN_DOOR            // 远程开门

// 双向流
Command.DUPLEX_CHANNEL              // 实时通信通道
```

### StatusCode 状态码枚举

```java
// 成功 (0-399)
StatusCode.SUCCESS                  // 成功
StatusCode.ACCEPTED                 // 已接受

// 客户端错误 (400-499)
StatusCode.BAD_REQUEST              // 请求参数错误
StatusCode.UNAUTHORIZED             // 未授权
StatusCode.DEVICE_OFFLINE           // 设备离线
StatusCode.FACE_NOT_FOUND           // 人脸未找到
StatusCode.TIME_TABLE_EMPTY         // 当前无课表

// 服务端错误 (500-599)
StatusCode.INTERNAL_ERROR           // 服务器内部错误
StatusCode.DATABASE_ERROR           // 数据库错误

// 网络错误 (600-699)
StatusCode.NETWORK_ERROR            // 网络错误
StatusCode.CONNECTION_TIMEOUT       // 连接超时

// 业务错误 (700-799)
StatusCode.DEVICE_BUSY              // 设备忙碌
StatusCode.FACE_REGISTER_FAILED     // 人脸注册失败
```

## 快速开始

### 1. 班牌设备作为客户端连接管理端

```java
ClassTimeTableDevice device = new ClassTimeTableDevice(
    "device-001",      // 设备 UUID
    "A101班牌",        // 设备名称
    "A101"             // 教室编号
);

// 连接到管理端
device.connectToManager("192.168.1.100", 9000)
    .subscribe();

// 上报人脸识别记录
device.reportFaceAccess("face-001", "张三", AccessRecord.Result.ALLOWED, 95)
    .subscribe();
```

### 2. 班牌设备作为服务端启动（调试模式）

```java
ClassTimeTableDevice device = new ClassTimeTableDevice(
    "device-001", 
    "A101班牌", 
    "A101"
);

// 作为服务端启动，等待管理端连接
device.startAsServer(9000)
    .subscribe();
```

### 3. 主动调用远程方法

```java
// 获取 RPC 客户端
PeerRpcClient client = device.getRpcClient();

// 获取当前课表
client.getCurrentTimeTable("device-001")
    .subscribe(resp -> {
        if (resp.isSuccess()) {
            TimeTable table = resp.getPayload();
            System.out.println("当前课程: " + table.getCourseName());
        } else if (resp.getStatus() == StatusCode.TIME_TABLE_EMPTY) {
            System.out.println("当前无课程");
        }
    });

// 订阅课表更新（流式）
client.subscribeTimeTableUpdates("device-001")
    .subscribe(update -> {
        System.out.println("课表更新: " + update.getPayload());
    });
```

### 4. 使用 RpcMessage 构造消息

```java
// 成功响应
RpcMessage<TimeTable> success = RpcMessage.success(
    Command.GET_CURRENT_TIME_TABLE, 
    timeTable
);

// 错误响应
RpcMessage<String> error = RpcMessage.error(
    Command.PUSH_FACE_DATA,
    StatusCode.FACE_REGISTER_FAILED,
    "人脸质量不合格"
);

// Fire-and-Forget
RpcMessage<DeviceStatus> heartbeat = RpcMessage.fireAndForget(
    Command.HEARTBEAT,
    deviceStatus
);
```

## 通信模式

### Request-Response（请求-响应）

```java
// 班牌 → 管理端：获取课表
client.getCurrentTimeTable(deviceUuid)
    .subscribe(resp -> { ... });

// 管理端 → 班牌：下发人脸
client.pushFaceData(faceData)
    .subscribe(resp -> { ... });
```

### Fire-and-Forget（单向通知）

```java
// 班牌 → 管理端：心跳（不需要响应）
client.heartbeat(status).subscribe();

// 班牌 → 管理端：上报访问记录
client.reportAccess(record).subscribe();
```

### Request-Stream（请求-流）

```java
// 管理端 → 班牌：订阅课表更新
client.subscribeTimeTableUpdates(deviceUuid)
    .subscribe(update -> { ... });
```

### Request-Channel（双向流）

```java
// 双向实时通信
Flux<RpcMessage<String>> input = Flux.interval(Duration.ofSeconds(1))
    .map(i -> RpcMessage.success(Command.DUPLEX_CHANNEL, "Ping " + i));

client.duplexChannel(input)
    .subscribe(response -> System.out.println("收到: " + response.getPayload()));
```

## 项目结构

```
class-time-table-rsocket/
├── pom.xml                                          # Maven 配置
├── README.md                                        # 使用文档
├── RSocket_TECH_GUIDE.md                           # 技术文档
└── src/
    ├── main/java/xyz/jasenon/lab/class_time_table/rsocket/
    │   ├── client/
    │   │   ├── RSocketPeerConnector.java           # P2P 连接器
    │   │   ├── RSocketPeerHandler.java             # 消息处理器
    │   │   └── PeerRpcClient.java                  # RPC 客户端封装
    │   ├── device/
    │   │   └── ClassTimeTableDevice.java           # 班牌设备实现
    │   ├── model/
    │   │   ├── RpcMessage.java                     # 通用消息封装
    │   │   ├── DeviceStatus.java                   # 设备状态
    │   │   ├── TimeTable.java                      # 课表信息
    │   │   ├── FaceData.java                       # 人脸数据
    │   │   ├── AccessRecord.java                   # 访问记录
    │   │   └── StatusCode.java                     # 状态码枚举
    │   └── protocol/
    │       ├── Command.java                        # 命令枚举
    │       └── PeerProtocol.java                   # 协议接口定义
    └── test/
        └── RSocketPeerTest.java                    # 测试用例
```

## 协议定义

详见 `PeerProtocol.java` 和 `Command.java`，包含以下接口：

| 方法 | 模式 | 方向 | 说明 | Command |
|------|------|------|------|---------|
| `register` | Request-Response | 班牌→管理端 | 设备注册/上线 | REGISTER |
| `heartbeat` | Fire-and-Forget | 班牌→管理端 | 心跳上报 | HEARTBEAT |
| `getDeviceStatus` | Request-Response | 双向 | 获取设备状态 | GET_DEVICE_STATUS |
| `getCurrentTimeTable` | Request-Response | 班牌→管理端 | 获取当前课表 | GET_CURRENT_TIME_TABLE |
| `subscribeTimeTableUpdates` | Request-Stream | 管理端→班牌 | 订阅课表更新 | SUBSCRIBE_TIME_TABLE_UPDATES |
| `pushFaceData` | Request-Response | 管理端→班牌 | 下发人脸数据 | PUSH_FACE_DATA |
| `removeFace` | Request-Response | 管理端→班牌 | 删除人脸 | REMOVE_FACE |
| `reportAccess` | Fire-and-Forget | 班牌→管理端 | 上报访问记录 | REPORT_ACCESS |
| `batchPushFaces` | Request-Stream | 管理端→班牌 | 批量下发人脸 | BATCH_PUSH_FACES |
| `getDeviceConfig` | Request-Response | 双向 | 获取设备配置 | GET_DEVICE_CONFIG |
| `updateDeviceConfig` | Request-Response | 管理端→班牌 | 更新配置 | UPDATE_DEVICE_CONFIG |
| `rebootDevice` | Request-Response | 管理端→班牌 | 远程重启 | REBOOT_DEVICE |
| `remoteOpenDoor` | Request-Response | 管理端→班牌 | 远程开门 | REMOTE_OPEN_DOOR |
| `duplexChannel` | Request-Channel | 双向 | 实时通道 | DUPLEX_CHANNEL |

## 依赖

- Spring Boot 3.x
- RSocket Java 1.1.x
- Project Reactor
- Fastjson2

## 与现有系统集成

本模块设计为独立模块，可通过以下方式与现有系统整合：

1. **作为子模块引入**
   ```xml
   <dependency>
       <groupId>xyz.jasenon.lab</groupId>
       <artifactId>class-time-table-rsocket</artifactId>
       <version>1.0-SNAPSHOT</version>
   </dependency>
   ```

2. **替换现有 t-io/J-IM**
   - 保留业务逻辑
   - 替换传输层为 RSocket
   - 复用现有的 Redis 缓存
   - 使用 Command 和 StatusCode 枚举规范通信协议

## License

MIT
