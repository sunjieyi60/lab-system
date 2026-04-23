---
name: rsocket-controller
description: RSocket 智慧班牌双工通信协议设计与实现规范。用于指导基于 RSocket 的 Server-Client（Android Kotlin）核心交互开发，包括 Packet 入参出参设计、Command 命令分发、路由映射、Handler 处理器体系以及 Server 主动推送机制。当需要新增 RSocket 业务命令、Packet 类、Controller 接口或 Android Client 处理器时使用此 Skill。
---

# RSocket Controller Skill

## 1. 核心架构：双工对等 + 双向标识

系统采用 **RSocket 双工对等架构**，Android 设备（Client）和 Java Server 均可主动发起请求。

| 方向 | 标识字段 | 路由/分发方式 | 交互模式 |
|------|----------|---------------|----------|
| **Client → Server** | `route` | Spring `@MessageMapping` | Request-Response / Fire-and-Forget |
| **Server → Client** | `command` | 底层 RSocket + `HandlerManager` 按 `Command` 分发 | Request-Response / Fire-and-Forget |

**关键设计原则**：
- Client 发 Server 用 **`route`**（字符串路由，如 `"device.register"`）
- Server 发 Client 用 **`command`**（枚举命令，如 `Command.OPEN_DOOR`）
- Server 主动推送**绕过 Spring `@MessageMapping`**，直接通过底层 `RSocket` 发送 JSON Payload
- Client 收到后按 **`command` 字段**路由到对应 `Handler` 处理器

---

## 2. 消息基类 Message

所有 Packet 直接继承 `Message`，**不使用泛型**（避免 Mono 构造困难）。

```java
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Message {
    private String route;          // Client → Server 时使用
    private String from;           // 消息来源设备标识
    private String to;             // 目标设备标识
    private Command command;       // Server → Client 时使用
    private Integer code;          // 状态码，默认 10000
    private String desc;           // 状态描述
    private String msg;            // 提示信息
    private String error;          // 详细错误/堆栈
    private Map<String, Object> extras;  // 扩展字段
    private Long timestamp;        // 时间戳

    public byte[] toByte() {
        return JSON.toJSONBytes(this);  // fastjson2 序列化
    }
}
```

---

## 3. 方向标记接口

Packet 通过实现方向接口表明通信方向：

```java
// Client → Server
public interface ClientSend {
    String route();
}

// Server → Client
public interface ServerSend<T extends Message> {
    Command command();
    default void init(T t) {
        t.setCommand(command());
        t.setTimestamp(System.currentTimeMillis());
    }
}
```

**使用规则**：
- **Request（Client 发出）**：实现 `ClientSend`，如 `RegisterRequest implements ClientSend`
- **Response（Server 返回）**：实现 `ServerSend`，如 `RegisterResponse implements ServerSend<RegisterResponse>`
- **双向共用**：如 `Heartbeat` 同时实现 `ClientSend` 和 `ServerSend`

---

## 4. Command 枚举（Server → Client 的命令标识）

```java
@Getter
public enum Command {
    REGISTER(1, "设备注册"),
    HEARTBEAT(2, "心跳"),
    OPEN_DOOR(3, "开门"),
    UPDATE_CONFIG(4, "更新配置"),
    UPDATE_FACE_LIBRARY(5, "更新人脸库"),
    UPDATE_SCHEDULE(6, "更新课表"),
    REBOOT(7, "设备重启"),
    SCREENSHOT(8, "远程截图"),
    STATUS_REPORT(9, "设备状态上报"),
    INIT_UPLOAD_TASK(10, "初始化上传任务"),
    UPLOAD_FACE_IMAGE(11, "上传人脸图片"),
    COMPLETE_UPLOAD_TASK(12, "完成上传任务"),
    RESPONSE(100, "通用响应");

    private final Integer code;
    private final String desc;
    // valueOf(Integer code) 静态查找
}
```

**核心机制**：Server 返回给 Client 的包通过 `command` 字段区分处理器。Client 端的 `HandlerManager` 维护 `Map<Command, Handler>`，收到消息后提取 `message.getCommand()` 分发到对应处理器。

---

## 5. Status 状态码 + Const.Route

自有协议状态码，区间规则：10000 成功、10001–19999 系统错误、20001–29999 业务错误、30001–39999 设备错误、40001–49999 网络错误。

路由常量（Client → Server 使用）：

```java
interface Route {
    String DEVICE_REGISTER = "device.register";
    String DEVICE_HEARTBEAT = "device.heartbeat";
    String DEVICE_STREAM = "device.stream";
    String DEVICE_CHANNEL = "device.channel";
    String DEVICE_COMMAND = "device.command";
    String DEVICE_CONFIG_UPDATE = "device.config.update";
    String DEVICE_FACE_UPDATE = "device.face.update";
    String DEVICE_SCHEDULE_UPDATE = "device.schedule.update";
    String DEVICE_DOOR_OPEN = "device.door.open";
    String DEVICE_REBOOT = "device.reboot";
}
```

---

## 6. Packet 入参出参概览

所有 Packet **直接继承 `Message`**，字段即业务数据，不通过嵌套 data 对象包装。

| 业务 | Request（方向） | Response（方向） | 关键字段 |
|------|-----------------|------------------|----------|
| 设备注册 | `RegisterRequest` (C→S) | `RegisterResponse` (S→C) | uuid, laboratoryId, config |
| 心跳 | `Heartbeat` (双向) | `Heartbeat` (双向) | uuid, interval |
| 配置更新 | `UpdateConfigRequest` (S→C) | `UpdateConfigResponse` (C→S) | config, immediate, version |
| 开门 | `OpenDoorRequest` (S→C) | `OpenDoorResponse` (C→S) | type{FACE,PASSWORD,REMOTE}, verifyInfo, duration |
| 人脸库更新 | `UpdateFaceLibraryRequest` (S→C) | `UpdateFaceLibraryResponse` (C→S) | updateType{FULL,INCREMENTAL}, faces, deletedFaceIds |
| 课表更新 | `UpdateScheduleRequest` (S→C) | `UpdateScheduleResponse` (C→S) | schedules, effectiveTime |
| 重启 | `RebootRequest` (S→C) | `RebootResponse` (C→S) | delaySeconds, reason |
| 文件上传 | `UploadTaskInitRequest` / `FileChunkPacket` / `UploadCompleteRequest` (S→C) | 对应 Response (C→S) | taskId, chunkIndex, data |

**完整字段定义参见** [references/packets.md](references/packets.md)。

---

## 7. Server 端 Controller 设计

### 7.1 RSocket Controller（Client → Server）

```java
@Slf4j
@Controller
@RequiredArgsConstructor
public class ConnectController {
    private final DeviceService deviceService;
    private final AbstractConnectionManager manager;

    @ConnectMapping
    public Mono<Void> logSetup(RSocketRequester requester, @Payload SetUp setUp) {
        return Mono.empty();
    }

    @MessageMapping(Const.Route.DEVICE_REGISTER)
    public Mono<RegisterResponse> registerResponse(
            @Payload RegisterRequest request, RSocketRequester requester) {
        return deviceService.register(request)
                .map(resp -> {
                    manager.register(resp.getUuid(), requester);
                    return resp;
                });
    }
}
```

### 7.2 HTTP 管理 Controller（业务触发推送）

Server 通过 HTTP REST 接口接收管理端操作，内部调用 `Server` 接口主动推送到设备。

---

## 8. Server 主动推送机制（核心）

Server 主动推送**不经过 Spring `@MessageMapping`**，直接通过底层 RSocket 发送 JSON Payload。

```java
@Slf4j
@Component
public class ServerImpl implements Server {

    @Autowired
    private AbstractConnectionManager connectionManager;

    // 单发 Request-Response
    @Override
    public Mono<Message> send(ServerSend message, RSocketRequester requester) {
        Command command = message.command();
        ((Message) message).setTimestamp(System.currentTimeMillis());
        ((Message) message).setCommand(command);

        return requester.rsocket()
                .requestResponse(ByteBufPayload.create(JSON.toJSONBytes(message)))
                .map(payload -> {
                    ByteBuffer buffer = payload.getData();
                    byte[] bytes = byteBufferToBytes(buffer);
                    payload.release();
                    return JSON.parseObject(bytes, Message.class);
                });
    }

    // 指定设备发送
    @Override
    public Mono<Message> sendTo(String deviceId, ServerSend message) {
        RSocketRequester requester = connectionManager.getRequester(deviceId);
        if (requester == null) {
            return Mono.error(new IllegalStateException("设备 " + deviceId + " 不在线"));
        }
        ((Message) message).setTo(deviceId);
        return send(message, requester);
    }

    // 广播
    @Override
    public Mono<Integer> broadcast(ServerSend message) {
        var connections = connectionManager.getAllConnections();
        return Flux.fromIterable(connections.entrySet())
                .flatMap(entry -> send(message, entry.getValue()).map(resp -> 1)
                        .onErrorResume(e -> Mono.just(0)))
                .reduce(Integer::sum);
    }
}
```

**关键要点**：
- `ServerSend` 参数必须提供 `command()`
- 发送前自动设置 `command` 和 `timestamp`
- 使用 `fastjson2` 序列化为 JSON 字节
- 通过 `ByteBufPayload.create()` 构造 RSocket Payload
- 返回反序列化为 `Message.class`（基类）

---

## 9. Client 端 Handler 分发机制（核心）

### 9.1 Handler 接口

```java
public interface Handler {
    Command command();                     // 返回支持的 Command 类型
    Mono<Message> handle(Message message); // 处理消息，返回响应 Message
}
```

### 9.2 HandlerManager

```java
@Slf4j
@Component
public class HandlerManager {
    private static final Map<Command, Handler> HANDLER_MAP = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private List<Handler> handlers;

    @PostConstruct
    public void init() {
        if (handlers != null) {
            for (Handler handler : handlers) {
                register(handler);
            }
        }
    }

    public static void register(Handler handler) {
        HANDLER_MAP.put(handler.command(), handler);
    }

    public static Handler get(Command command) {
        return HANDLER_MAP.get(command);
    }

    public static boolean supports(Command command) {
        return HANDLER_MAP.containsKey(command);
    }
}
```

### 9.3 预置 Handler 列表

| Handler | Command | 职责 |
|---------|---------|------|
| `RegisterHandler` | `REGISTER` | 处理注册响应 |
| `HeartbeatHandler` | `HEARTBEAT` | 处理心跳 |
| `OpenDoorHandler` | `OPEN_DOOR` | 执行开门操作 |
| `UpdateConfigHandler` | `UPDATE_CONFIG` | 更新本地配置 |
| `UpdateFaceLibraryHandler` | `UPDATE_FACE_LIBRARY` | 更新人脸库 |
| `UpdateScheduleHandler` | `UPDATE_SCHEDULE` | 更新课表 |
| `RebootHandler` | `REBOOT` | 执行重启 |
| `FileChunkHandler` | `UPLOAD_FACE_IMAGE` | 接收人脸图片分片 |
| `UploadTaskInitHandler` | `INIT_UPLOAD_TASK` | 初始化上传任务 |
| `UploadCompleteHandler` | `COMPLETE_UPLOAD_TASK` | 完成上传提取特征 |

### 9.4 Handler 实现示例

```java
@Slf4j
public class OpenDoorHandler implements Handler {
    @Override
    public Command command() {
        return Command.OPEN_DOOR;
    }

    @Override
    public Mono<Message> handle(Message message) {
        if (!(message instanceof OpenDoorRequest)) {
            return Mono.just(Message.error(Command.OPEN_DOOR, Status.C10001, "消息类型错误"));
        }
        OpenDoorRequest request = (OpenDoorRequest) message;

        OpenDoorResponse response = new OpenDoorResponse();
        response.setCommand(Command.OPEN_DOOR);
        response.setSuccess(true);
        response.setCode(0);
        response.setMessageText("开门成功");
        response.setOpenTime(System.currentTimeMillis());
        response.setStatus(Status.C10000, "开门成功");
        return Mono.just(response);
    }
}
```

---

## 10. Android Kotlin Client 架构

Android 端采用 **Kotlin 协程 + rsocket-kotlin** 实现，同时作为 Client 发起请求和作为 Server 接收推送。

**核心组件**：

| 组件 | 职责 |
|------|------|
| `RSocketClientManager` | 单例管理长连接、自动重连、状态监听 |
| `RSocketRequestHandler` | Client 角色：发送 Request-Response / Fire-and-Forget |
| `RSocketResponseHandler` | Server 角色：接收对端请求，按 `route` 分发到 handler |
| `RSocketRequest` / `RSocketResponse` | 请求/响应数据包装 |
| `RSocketRequestable` | 可序列化请求接口，模板方法模式 |

**完整实现代码参见** [references/android-client.md](references/android-client.md)。

**Android 使用示例**：

```kotlin
// 建立连接并注册 Server 端处理器
lifecycleScope.launch {
    val responseHandler = RSocketResponseHandler(coroutineContext)
    responseHandler.onDoorOpen { data ->
        val request = GsonUtil.fromJson<OpenDoorRequest>(data)
        // 执行开门...
        RSocketResponse.fromJson("""{"success":true}""")
    }

    RSocketClientManager.getInstance(context).connect(
        setup = SetUp(DeviceProfileObservable.getCurrentUuid()),
        responseHandler = responseHandler
    )
}

// 发送注册请求
lifecycleScope.launch {
    val response = RSocketClientManager.getInstance(context).requestResponse(
        request = RegisterRequest(uuid = "xxx", laboratoryId = 1L).convert(),
        clazz = RegisterResponse::class.java
    )
}
```

---

## 11. 新增业务命令的标准流程

当需要新增一个 Server → Client 的业务命令时，按以下步骤执行：

### 11.1 定义 Command

```java
// Command.java
NEW_FEATURE(13, "新功能");
```

### 11.2 创建 Request/Response Packet

```java
// Server → Client 的 Request
@Getter @Setter
public class NewFeatureRequest extends Message implements ServerSend {
    private String featureData;
    public NewFeatureRequest() { init(this); }
    @Override public Command command() { return Command.NEW_FEATURE; }
}

// Client → Server 的 Response
@Getter @Setter
public class NewFeatureResponse extends Message implements ClientSend {
    private boolean success;
    @Override public String route() { return Const.Route.DEVICE_COMMAND; }
}
```

### 11.3 Server 端推送

```java
NewFeatureRequest request = new NewFeatureRequest();
request.setFeatureData("...");
server.sendTo(deviceUuid, request).subscribe();
```

### 11.4 注册 Handler（Java Server 作为 Client 时）

```java
@Slf4j
public class NewFeatureHandler implements Handler {
    @Override public Command command() { return Command.NEW_FEATURE; }

    @Override
    public Mono<Message> handle(Message message) {
        NewFeatureRequest request = (NewFeatureRequest) message;
        NewFeatureResponse response = new NewFeatureResponse();
        response.setSuccess(true);
        return Mono.just(response);
    }
}
```

在 `HandlerConfiguration` 中注册：

```java
@Bean @ConditionalOnMissingBean
public NewFeatureHandler newFeatureHandler() {
    return new NewFeatureHandler();
}
```

### 11.5 Android Client 注册处理器

```kotlin
responseHandler.registerHandler("device.new.feature") { data ->
    val request = GsonUtil.fromJson<NewFeatureRequest>(data)
    // 业务处理...
    RSocketResponse.fromJson("""{"success":true}""")
}
```

---

## 12. 关键约束与注意事项

1. **不使用泛型**：`Message` 基类无泛型参数，避免 Mono/Flux 类型推断问题
2. **序列化统一**：Server 端使用 `fastjson2`，Android 端使用 `Gson`
3. **字段兼容**：Packet 新增字段时必须可空或有默认值，确保前后兼容
4. **Handler 线程安全**：`HandlerManager` 使用 `ConcurrentHashMap`，Handler 实现需保证无状态或线程安全
5. **连接管理**：`AbstractConnectionManager` 维护 `Map<String, RSocketRequester>`，设备断线时自动移除
6. **超时控制**：命令默认超时 30 秒，由调用方控制
