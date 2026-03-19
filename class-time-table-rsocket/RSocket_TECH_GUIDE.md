# RSocket 技术使用指南

> 智慧班牌系统 P2P 通信技术文档
> 
> **服务端**: Java Spring Boot + RSocket Java  
> **客户端**: Android Kotlin + rsocket-kotlin

---

## 目录

1. [技术栈与依赖](#1-技术栈与依赖)
2. [RSocket 简介](#2-rsocket-简介)
3. [核心概念](#3-核心概念)
4. [通信模式详解](#4-通信模式详解)
5. [项目架构设计](#5-项目架构设计)
6. [服务端实现（Java）](#6-服务端实现java)
7. [客户端实现（Kotlin）](#7-客户端实现kotlin)
8. [双端交互业务 Demo](#8-双端交互业务-demo)
9. [最佳实践](#9-最佳实践)
10. [故障排查](#10-故障排查)

---

## 1. 技术栈与依赖

### 1.1 服务端依赖（Spring Boot）

```xml
<!-- pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-rsocket</artifactId>
    </dependency>
    <dependency>
        <groupId>io.rsocket</groupId>
        <artifactId>rsocket-core</artifactId>
        <version>1.1.4</version>
    </dependency>
</dependencies>
```

### 1.2 客户端依赖（Android Kotlin）

```kotlin
// build.gradle.kts (Module: app)
dependencies {
    // RSocket Kotlin
    implementation("io.rsocket.kotlin:rsocket-core:0.15.4")
    implementation("io.rsocket.kotlin:rsocket-ktor-client:0.15.4")
    
    // Ktor 引擎（TCP 或 WebSocket）
    implementation("io.ktor:ktor-client-cio:2.3.7")  // TCP
    // 或
    implementation("io.ktor:ktor-client-okhttp:2.3.7")  // WebSocket
    
    // Kotlin 序列化
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.7.3")
}
```

### 1.3 Kotlin 序列化配置

```kotlin
// build.gradle.kts (Project)
plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
}
```

---

## 2. RSocket 简介

### 2.1 为什么选择 RSocket

```
传统 HTTP/REST          vs          RSocket
─────────────────────────────────────────────────
请求-响应（轮询）                    双向流（推送）
短连接（高延迟）                     长连接（低延迟）
服务端无法主动推送                    服务端主动推送
无背压控制                           内置背压控制
```

**班牌场景适用性：**
- ✅ 服务端主动下发人脸/课表（Server → Android Push）
- ✅ 设备心跳上报（Android → Server Fire-and-Forget）
- ✅ 实时监控流（双向流）
- ✅ 断线重连、恢复（Resume 协议）

---

## 3. 核心概念

### 3.1 交互模型对照

| 模型 | Java (Reactor) | Kotlin (Coroutines) | 班牌场景 |
|------|----------------|---------------------|----------|
| Request/Response | `Mono<T>` | `suspend fun` | 查询课表 |
| Fire-and-Forget | `Mono<Void>` | `suspend fun` | 心跳上报 |
| Request Stream | `Flux<T>` | `Flow<T>` | 订阅更新 |
| Request Channel | `Flux<Flux<T>>` | `Flow<Flow<T>>` | 实时监控 |

### 3.2 数据模型（双端共享）

**Java (服务端)**
```java
@Data
@Builder
public class RpcMessage<T> implements Serializable {
    private String messageId;
    private Command command;        // 枚举
    private StatusCode status;      // 枚举
    private String fromUuid;
    private T payload;
    private Instant timestamp;
}
```

**Kotlin (客户端)**
```kotlin
@Serializable
@JvmName("RpcMessageKt")
data class RpcMessage<T>(
    val messageId: String = generateId(),
    val command: Command,           // 枚举
    val status: StatusCode = StatusCode.SUCCESS,
    val fromUuid: String? = null,
    val payload: T? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class Command {
    REGISTER, HEARTBEAT, GET_CURRENT_TIME_TABLE,
    PUSH_FACE_DATA, REPORT_ACCESS, // ...
}
```

---

## 4. 通信模式详解

### 4.1 Request/Response

**服务端（Java）**
```java
@Controller
public class DeviceController {
    
    @MessageMapping("device.register")
    public Mono<RpcMessage<DeviceStatus>> register(DeviceStatus status) {
        return deviceService.register(status)
            .map(device -> RpcMessage.success(Command.REGISTER, device));
    }
}
```

**客户端（Kotlin）**
```kotlin
class DeviceClient(private val rSocket: RSocket) {
    
    suspend fun register(status: DeviceStatus): RpcMessage<DeviceStatus> {
        val request = RpcMessage(
            command = Command.REGISTER,
            payload = status
        )
        
        return rSocket.requestResponse(
            buildPayload { data(Json.encodeToString(request)) }
        ).let { response ->
            Json.decodeFromString(response.data.readText())
        }
    }
}
```

### 4.2 Fire-and-Forget

**服务端（Java）**
```java
@MessageMapping("device.heartbeat")
public Mono<Void> heartbeat(DeviceStatus status) {
    return deviceService.updateHeartbeat(status);
}
```

**客户端（Kotlin）**
```kotlin
suspend fun heartbeat(status: DeviceStatus) {
    val request = RpcMessage(
        command = Command.HEARTBEAT,
        payload = status
    )
    
    rSocket.fireAndForget(
        buildPayload { data(Json.encodeToString(request)) }
    )
}
```

### 4.3 Request Stream

**服务端（Java）**
```java
@MessageMapping("timetable.subscribe")
public Flux<RpcMessage<TimeTable>> subscribeUpdates(String deviceUuid) {
    return timeTableService.getUpdateStream(deviceUuid)
        .map(table -> RpcMessage.success(Command.SUBSCRIBE_TIME_TABLE_UPDATES, table));
}
```

**客户端（Kotlin）**
```kotlin
fun subscribeTimeTableUpdates(deviceUuid: String): Flow<TimeTable> = 
    rSocket.requestStream(
        buildPayload { 
            data(Json.encodeToString(RpcMessage(
                command = Command.SUBSCRIBE_TIME_TABLE_UPDATES,
                payload = deviceUuid
            ))) 
        }
    )
    .map { response ->
        Json.decodeFromString<RpcMessage<TimeTable>>(response.data.readText())
    }
    .map { it.payload!! }
    .asFlow()  // 转换为 Kotlin Flow
```

### 4.4 Request Channel（双向流）

**服务端（Java）**
```java
@MessageMapping("device.channel")
public Flux<RpcMessage<String>> duplexChannel(Flux<RpcMessage<String>> input) {
    return input.map(msg -> 
        RpcMessage.success(Command.DUPLEX_CHANNEL, "Echo: " + msg.getPayload())
    );
}
```

**客户端（Kotlin）**
```kotlin
fun openChannel(commands: Flow<String>): Flow<String> = 
    rSocket.requestChannel(
        commands.map { cmd ->
            buildPayload {
                data(Json.encodeToString(RpcMessage(
                    command = Command.DUPLEX_CHANNEL,
                    payload = cmd
                )))
            }
        }.asPublisher()  // Flow -> Publisher
    )
    .asFlow()  // Publisher -> Flow
    .map { Json.decodeFromString<RpcMessage<String>>(it.data.readText()).payload!! }
```

---

## 5. 项目架构设计

### 5.1 双端架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         服务端（Java）                            │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │  @Controller                                               │ │
│  │  ├─ @MessageMapping("device.register")                    │ │
│  │  ├─ @MessageMapping("device.heartbeat")                   │ │
│  │  ├─ @MessageMapping("timetable.subscribe")                │ │
│  │  └─ @MessageMapping("face.push")                          │ │
│  └───────────────────────────────────────────────────────────┘ │
│                         ↓ RSocket TCP                           │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│                       客户端（Android Kotlin）                    │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │  ViewModel                                                 │ │
│  │  ├─ DeviceViewModel                                       │ │
│  │  ├─ TimeTableViewModel                                    │ │
│  │  └─ FaceRecognitionViewModel                              │ │
│  └───────────────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │  RSocketClient                                             │ │
│  │  ├─ connectToServer()                                     │ │
│  │  ├─ register()                                            │ │
│  │  ├─ startHeartbeat()                                      │ │
│  │  └─ observeTimeTableUpdates()                             │ │
│  └───────────────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │  Repository                                                │ │
│  │  ├─ DeviceRepository                                      │ │
│  │  ├─ TimeTableRepository                                   │ │
│  │  └─ FaceRepository                                        │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. 服务端实现（Java）

### 6.1 主控制器

```java
@Controller
@Slf4j
public class ClassTimeTableRSocketController {
    
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private TimeTableService timeTableService;
    @Autowired
    private FaceService faceService;
    
    // ========== 设备管理 ==========
    
    @MessageMapping("device.register")
    public Mono<RpcMessage<DeviceStatus>> register(DeviceStatus status) {
        log.info("设备注册: {}", status.getUuid());
        return deviceService.register(status)
            .map(device -> RpcMessage.success(Command.REGISTER, device));
    }
    
    @MessageMapping("device.heartbeat")
    public Mono<Void> heartbeat(DeviceStatus status) {
        return deviceService.updateHeartbeat(status);
    }
    
    @MessageMapping("device.{uuid}.status")
    public Mono<RpcMessage<DeviceStatus>> getDeviceStatus(
            @DestinationVariable String uuid) {
        return deviceService.findByUuid(uuid)
            .map(device -> RpcMessage.success(Command.GET_DEVICE_STATUS, device))
            .switchIfEmpty(Mono.just(
                RpcMessage.error(Command.GET_DEVICE_STATUS, StatusCode.DEVICE_OFFLINE, "设备离线")
            ));
    }
    
    // ========== 课表管理 ==========
    
    @MessageMapping("timetable.current")
    public Mono<RpcMessage<TimeTable>> getCurrentTimeTable(String deviceUuid) {
        return timeTableService.getCurrent(deviceUuid)
            .map(table -> RpcMessage.success(Command.GET_CURRENT_TIME_TABLE, table))
            .switchIfEmpty(Mono.just(
                RpcMessage.error(Command.GET_CURRENT_TIME_TABLE, StatusCode.TIME_TABLE_EMPTY, "当前无课程")
            ));
    }
    
    @MessageMapping("timetable.subscribe")
    public Flux<RpcMessage<TimeTable>> subscribeUpdates(String deviceUuid) {
        return timeTableService.getUpdateStream(deviceUuid)
            .map(table -> RpcMessage.success(Command.SUBSCRIBE_TIME_TABLE_UPDATES, table));
    }
    
    // ========== 人脸识别 ==========
    
    @MessageMapping("face.push")
    public Mono<RpcMessage<String>> pushFaceData(FaceData faceData) {
        return faceService.save(faceData)
            .map(id -> RpcMessage.success(Command.PUSH_FACE_DATA, id));
    }
    
    @MessageMapping("access.report")
    public Mono<Void> reportAccess(AccessRecord record) {
        return accessService.save(record).then();
    }
}
```

### 6.2 会话管理器

```java
@Service
@Slf4j
public class DeviceSessionManager {
    
    private final Map<String, RSocketRequester> sessions = new ConcurrentHashMap<>();
    
    @EventListener
    public void onConnect(RSocketConnectionEvent event) {
        String uuid = extractUuid(event.getSetupPayload());
        RSocketRequester requester = event.getRequester();
        
        sessions.put(uuid, requester);
        log.info("设备上线: {}, 当前在线: {}", uuid, sessions.size());
        
        // 监听断开
        event.getRequester().rsocket()
            .onClose()
            .doFinally(signal -> {
                sessions.remove(uuid);
                log.info("设备离线: {}", uuid);
            })
            .subscribe();
    }
    
    public RSocketRequester getRequester(String uuid) {
        return sessions.get(uuid);
    }
    
    public List<String> getOnlineDevices() {
        return new ArrayList<>(sessions.keySet());
    }
    
    // 主动推送人脸到设备
    public Mono<Void> pushFaceToDevice(String uuid, FaceData face) {
        RSocketRequester requester = sessions.get(uuid);
        if (requester == null) {
            return Mono.empty();
        }
        
        return requester.route("face.push")
            .data(face)
            .send();
    }
}
```

---

## 7. 客户端实现（Kotlin）

### 7.1 RSocket 客户端封装

```kotlin
class ClassTimeTableRSocketClient(
    private val host: String,
    private val port: Int,
    private val deviceUuid: String
) {
    private var rSocket: RSocket? = null
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }
    
    suspend fun connect() {
        _connectionState.value = ConnectionState.Connecting
        
        try {
            rSocket = RSocketConnector {
                // 重连策略
                reconnectStrategy { attempt, _ ->
                    val delay = (1000L * attempt).coerceAtMost(30000)
                    Logger.d("重连尝试 $attempt，延迟 ${delay}ms")
                    delay(delay)
                }
            }.connect(
                TcpClientTransport(host, port)
            )
            
            _connectionState.value = ConnectionState.Connected
            Logger.d("RSocket 连接成功")
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "未知错误")
            throw e
        }
    }
    
    // ========== 设备管理 ==========
    
    suspend fun register(deviceStatus: DeviceStatus): Result<DeviceStatus> = runCatching {
        val request = RpcMessage(
            command = Command.REGISTER,
            payload = deviceStatus
        )
        
        val response = rSocket?.requestResponse(
            buildPayload { data(Json.encodeToString(request)) }
        )?.await() ?: throw IllegalStateException("未连接")
        
        Json.decodeFromString<RpcMessage<DeviceStatus>>(response.data.readText())
            .let { 
                if (it.status == StatusCode.SUCCESS) Result.success(it.payload!!)
                else Result.failure(Exception(it.errorMsg))
            }
    }.getOrElse { Result.failure(it) }
    
    suspend fun heartbeat(status: DeviceStatus) {
        val request = RpcMessage(
            command = Command.HEARTBEAT,
            payload = status
        )
        
        rSocket?.fireAndForget(
            buildPayload { data(Json.encodeToString(request)) }
        )
    }
    
    // ========== 课表订阅 ==========
    
    fun observeTimeTableUpdates(): Flow<TimeTable> = 
        rSocket?.requestStream(
            buildPayload {
                data(Json.encodeToString(RpcMessage(
                    command = Command.SUBSCRIBE_TIME_TABLE_UPDATES,
                    payload = deviceUuid
                )))
            }
        )
        ?.map { response ->
            Json.decodeFromString<RpcMessage<TimeTable>>(response.data.readText())
        }
        ?.filter { it.status == StatusCode.SUCCESS }
        ?.map { it.payload!! }
        ?.asFlow()
        ?: emptyFlow()
    
    // ========== 人脸接收 ==========
    
    suspend fun observeFacePush(): Flow<FaceData> = callbackFlow {
        rSocket?.requestChannel(
            // 发送心跳保持连接
            flowOf(
                buildPayload {
                    data(Json.encodeToString(RpcMessage(
                        command = Command.DUPLEX_CHANNEL,
                        payload = "KEEP_ALIVE"
                    )))
                }
            ).asPublisher()
        )
        ?.asFlow()
        ?.map { Json.decodeFromString<RpcMessage<FaceData>>(it.data.readText()) }
        ?.filter { it.command == Command.PUSH_FACE_DATA }
        ?.map { it.payload!! }
        ?.collect { trySend(it) }
        
        awaitClose()
    } ?: emptyFlow()
    
    fun disconnect() {
        rSocket?.dispose()
        _connectionState.value = ConnectionState.Disconnected
    }
}
```

### 7.2 ViewModel 集成

```kotlin
@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val client: ClassTimeTableRSocketClient
) : ViewModel() {
    
    private val _deviceState = MutableStateFlow<DeviceState>(DeviceState.Idle)
    val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()
    
    private val _timeTable = MutableStateFlow<TimeTable?>(null)
    val timeTable: StateFlow<TimeTable?> = _timeTable.asStateFlow()
    
    init {
        viewModelScope.launch {
            client.connectionState.collect { state ->
                when (state) {
                    is ClassTimeTableRSocketClient.ConnectionState.Connected -> {
                        _deviceState.value = DeviceState.Online
                        startHeartbeat()
                        subscribeTimeTable()
                    }
                    is ClassTimeTableRSocketClient.ConnectionState.Error -> {
                        _deviceState.value = DeviceState.Error(state.message)
                    }
                    else -> { }
                }
            }
        }
    }
    
    fun connect() {
        viewModelScope.launch {
            client.connect()
            val deviceStatus = DeviceStatus(
                uuid = client.deviceUuid,
                deviceName = "A101班牌",
                status = DeviceStatus.Status.ONLINE
            )
            
            client.register(deviceStatus)
                .onSuccess { 
                    Logger.d("注册成功")
                    _deviceState.value = DeviceState.Registered
                }
                .onFailure {
                    Logger.e("注册失败: ${it.message}")
                }
        }
    }
    
    private fun startHeartbeat() {
        viewModelScope.launch {
            while (isActive) {
                delay(30000)  // 30秒心跳
                client.heartbeat(
                    DeviceStatus(
                        uuid = client.deviceUuid,
                        status = DeviceStatus.Status.ONLINE,
                        reportTime = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    
    private fun subscribeTimeTable() {
        viewModelScope.launch {
            client.observeTimeTableUpdates()
                .catch { e ->
                    Logger.e("课表订阅错误: ${e.message}")
                }
                .collect { table ->
                    _timeTable.value = table
                    // 更新 UI 显示
                }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        client.disconnect()
    }
}
```

---

## 8. 双端交互业务 Demo

### 8.1 场景一：班牌启动注册流程

**流程图：**
```
Android (Kotlin)              Spring Boot (Java)
    │                               │
    ├─ 1. TCP 连接 ───────────────→│
    │                               │
    ├─ 2. REGISTER ──────────────→│
    │                               ├─ 保存设备信息
    │←─ 3. 返回配置 ───────────────┤
    │                               │
    ├─ 4. GET_TIME_TABLE_LIST ───→│
    │←─ 5. 返回课表 ───────────────┤
    │                               │
    ╞═ 6. 心跳循环（每30秒）═══════╡
    ├─ HEARTBEAT ────────────────→│
    │                               ├─ 更新在线状态
```

**服务端（Java）**
```java
@Controller
@Slf4j
public class DeviceStartupController {
    
    @Autowired
    private DeviceService deviceService;
    
    @MessageMapping("device.register")
    public Mono<RpcMessage<DeviceConfig>> register(DeviceStatus status) {
        log.info("设备注册请求: {}", status.getUuid());
        
        return deviceService.register(status)
            .flatMap(device -> 
                // 下发初始配置
                configService.getDeviceConfig(device.getUuid())
                    .map(config -> RpcMessage.success(Command.REGISTER, config))
            );
    }
    
    @MessageMapping("timetable.list")
    public Flux<RpcMessage<TimeTable>> getTimeTableList(String deviceUuid) {
        String roomId = extractRoomFromDevice(deviceUuid);
        return timeTableService.findByRoomAndDate(roomId, LocalDate.now())
            .map(table -> RpcMessage.success(Command.GET_TIME_TABLE_LIST, table));
    }
    
    @MessageMapping("device.heartbeat")
    public Mono<Void> heartbeat(DeviceStatus status) {
        return deviceService.updateHeartbeat(status.getUuid(), status.getReportTime());
    }
}
```

**客户端（Kotlin）**
```kotlin
class DeviceStartupManager(
    private val client: ClassTimeTableRSocketClient
) {
    private var heartbeatJob: Job? = null
    
    suspend fun startup(): Result<DeviceConfig> = runCatching {
        // 1. 连接服务器
        Logger.d("正在连接服务器...")
        client.connect()
        
        // 2. 注册设备
        Logger.d("正在注册设备...")
        val deviceStatus = DeviceStatus(
            uuid = client.deviceUuid,
            deviceName = getDeviceName(),
            roomId = getRoomId(),
            status = DeviceStatus.Status.ONLINE,
            terminal = "Android ${Build.VERSION.RELEASE}",
            ipAddress = getLocalIpAddress()
        )
        
        val config = client.register(deviceStatus)
            .getOrElse { throw it }
        Logger.d("注册成功，收到配置: $config")
        
        // 3. 同步课表
        Logger.d("同步课表...")
        syncTimeTable()
        
        // 4. 启动心跳
        startHeartbeat()
        
        config
    }.onFailure { e ->
        Logger.e("启动失败: ${e.message}")
        enterOfflineMode()
    }
    
    private suspend fun syncTimeTable() {
        val request = RpcMessage(
            command = Command.GET_TIME_TABLE_LIST,
            payload = client.deviceUuid
        )
        
        client.rSocket?.requestStream(
            buildPayload { data(Json.encodeToString(request)) }
        )
        ?.asFlow()
        ?.map { Json.decodeFromString<RpcMessage<TimeTable>>(it.data.readText()) }
        ?.filter { it.status == StatusCode.SUCCESS }
        ?.map { it.payload!! }
        ?.toList()
        ?.let { tables ->
            // 保存到本地数据库
            TimeTableDatabase.saveAll(tables)
            Logger.d("课表同步完成，共 ${tables.size} 条")
        }
    }
    
    private fun startHeartbeat() {
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(30000)  // 30秒间隔
                
                try {
                    client.heartbeat(
                        DeviceStatus(
                            uuid = client.deviceUuid,
                            status = DeviceStatus.Status.ONLINE,
                            reportTime = System.currentTimeMillis()
                        )
                    )
                    Logger.d("心跳发送成功")
                } catch (e: Exception) {
                    Logger.e("心跳失败: ${e.message}")
                    // 触发重连
                    reconnect()
                }
            }
        }
    }
    
    private suspend fun reconnect() {
        // 指数退避重连
        var attempt = 0
        while (attempt < 5) {
            val delayMs = (1000L * (1 shl attempt)).coerceAtMost(30000)
            Logger.d("${delayMs}ms 后重试...")
            delay(delayMs)
            
            runCatching {
                client.connect()
                startup()
                return
            }
            attempt++
        }
        
        enterOfflineMode()
    }
    
    private fun enterOfflineMode() {
        Logger.w("进入离线模式")
        // 切换到本地缓存数据
    }
}
```

### 8.2 场景二：服务端主动推送人脸更新

**流程图：**
```
Web 管理端                       Spring Boot                  Android (Kotlin)
    │                               │                              │
    ├─ 上传新人脸 ───────────────→│                              │
    │                               ├─ 保存人脸数据               │
    │                               ├─ 查找在线设备               │
    │                               ├─ PUSH_FACE_DATA ─────────→│
    │                               │                              ├─ 保存到本地库
    │                               │←─ 返回成功 ─────────────────┤
    │←─ 推送成功 ─────────────────┤                              │
```

**服务端（Java）**
```java
@Service
@Slf4j
public class FacePushService {
    
    @Autowired
    private DeviceSessionManager sessionManager;
    @Autowired
    private FaceService faceService;
    
    /**
     * Web 端调用：上传人脸后推送到设备
     */
    public Mono<Void> uploadAndPush(FaceUploadRequest request) {
        return faceService.save(request)
            .flatMapMany(face -> 
                // 推送到指定教室的所有设备
                Flux.fromIterable(request.getTargetRoomIds())
                    .flatMap(roomId -> pushToRoom(roomId, face))
            )
            .then();
    }
    
    private Mono<Void> pushToRoom(String roomId, FaceData face) {
        return deviceService.findByRoomId(roomId)
            .flatMap(device -> {
                RSocketRequester requester = sessionManager.getRequester(device.getUuid());
                if (requester == null) {
                    log.warn("设备 {} 不在线，跳过推送", device.getUuid());
                    return Mono.empty();
                }
                
                return requester.route("face.push")
                    .data(face)
                    .retrieveMono(RpcMessage.class)
                    .doOnSuccess(resp -> 
                        log.info("人脸推送成功: {} -> {}", face.getFaceId(), device.getUuid())
                    )
                    .doOnError(e -> 
                        log.error("人脸推送失败: {}", device.getUuid(), e)
                    )
                    .onErrorResume(e -> Mono.empty())
                    .then();
            });
    }
}
```

**客户端（Kotlin）**
```kotlin
class FaceDataManager(
    private val rSocket: RSocket,
    private val faceDatabase: FaceDatabase
) {
    private val _faceUpdateFlow = MutableSharedFlow<FaceData>()
    val faceUpdateFlow: SharedFlow<FaceData> = _faceUpdateFlow.asSharedFlow()
    
    /**
     * 监听服务端推送的人脸数据
     */
    suspend fun startListening() {
        Logger.d("开始监听人脸推送...")
        
        // 使用 requestChannel 建立双向通道接收推送
        rSocket.requestChannel(
            // 发送端：定期发送心跳保持连接
            flow {
                while (true) {
                    emit(buildPayload {
                        data(Json.encodeToString(RpcMessage(
                            command = Command.DUPLEX_CHANNEL,
                            payload = "FACE_CHANNEL_KEEPALIVE"
                        )))
                    })
                    delay(60000)  // 每分钟心跳
                }
            }.asPublisher()
        )
        .asFlow()
        .map { payload ->
            Json.decodeFromString<RpcMessage<FaceData>>(payload.data.readText())
        }
        .filter { it.command == Command.PUSH_FACE_DATA }
        .collect { message ->
            val faceData = message.payload!!
            Logger.d("收到人脸推送: ${faceData.faceId} - ${faceData.faceName}")
            
            // 保存到本地数据库
            faceDatabase.insert(faceData)
            
            // 通知人脸识别引擎更新
            _faceUpdateFlow.emit(faceData)
            
            // 更新 UI
            NotificationHelper.show("新人脸注册: ${faceData.faceName}")
        }
    }
    
    /**
     * 获取本地所有人脸
     */
    suspend fun getLocalFaces(): List<FaceData> {
        return faceDatabase.getAll()
    }
}

// ViewModel 中使用
@HiltViewModel
class FaceViewModel @Inject constructor(
    private val faceManager: FaceDataManager
) : ViewModel() {
    
    init {
        viewModelScope.launch {
            faceManager.faceUpdateFlow.collect { face ->
                // 更新 UI 显示最新人脸列表
                _faceList.value = faceManager.getLocalFaces()
            }
        }
    }
}
```

### 8.3 场景三：实时门禁监控（双向流）

**流程图：**
```
Android (Kotlin)              Spring Boot              Web 监控端
    │                              │                         │
    ╞══════ 双向流通道 ═══════════╡                         │
    ├─ 1. 识别到人脸 ────────────→│                         │
    │                              ├─ 2. 验证权限            │
    │                              ├─ 3. 记录日志            │
    │←─ 4. 返回开门指令 ──────────┤                         │
    ├─ 5. 开门 ─────────────────→│                         │
    │                              ├─ 6. 推送监控端 ───────→│
    │                              │                         ├─ 实时显示
```

**服务端（Java）**
```java
@Controller
@Slf4j
public class AccessMonitorController {
    
    @Autowired
    private AccessControlService accessService;
    @Autowired
    private SimpMessagingTemplate webSocket;
    
    /**
     * 班牌建立实时监控通道
     */
    @MessageMapping("access.monitor.{deviceUuid}")
    public Flux<RpcMessage<AccessControlResult>> monitorChannel(
            @DestinationVariable String deviceUuid,
            Flux<RpcMessage<AccessEvent>> events) {
        
        log.info("开始监控设备: {}", deviceUuid);
        
        return events
            .flatMap(event -> handleAccessEvent(deviceUuid, event.getPayload()))
            .doOnNext(result -> {
                // 推送到 Web 监控端
                webSocket.convertAndSend("/topic/monitor/" + deviceUuid, result);
            })
            .doOnCancel(() -> log.info("监控结束: {}", deviceUuid));
    }
    
    private Mono<RpcMessage<AccessControlResult>> handleAccessEvent(
            String deviceUuid, AccessEvent event) {
        
        return switch (event.getType()) {
            case FACE_DETECTED -> handleFaceAccess(deviceUuid, event);
            case CARD_SWIPED -> handleCardAccess(deviceUuid, event);
            case EMERGENCY_OPEN -> handleEmergencyOpen(deviceUuid, event);
        };
    }
    
    private Mono<RpcMessage<AccessControlResult>> handleFaceAccess(
            String deviceUuid, AccessEvent event) {
        
        String faceId = event.getFaceId();
        
        return Mono.zip(
            faceService.findById(faceId),
            deviceService.findByUuid(deviceUuid)
        ).flatMap(tuple -> {
            FaceData face = tuple.getT1();
            Device device = tuple.getT2();
            
            // 验证权限
            return accessService.verifyPermission(face, device)
                .map(allowed -> {
                    AccessControlResult result = new AccessControlResult();
                    result.setAllowed(allowed);
                    result.setPersonName(face.getFaceName());
                    result.setAction(allowed ? "OPEN_DOOR" : "DENY");
                    
                    // 记录访问日志
                    recordAccess(deviceUuid, face, allowed);
                    
                    return RpcMessage.success(Command.DUPLEX_CHANNEL, result);
                });
        }).switchIfEmpty(Mono.just(
            RpcMessage.error(Command.DUPLEX_CHANNEL, StatusCode.FACE_NOT_FOUND, "未知人脸")
        ));
    }
}
```

**客户端（Kotlin）**
```kotlin
class AccessControlManager(
    private val rSocket: RSocket,
    private val faceEngine: FaceRecognitionEngine,
    private val doorController: DoorController
) {
    private val _accessState = MutableStateFlow<AccessState>(AccessState.Idle)
    val accessState: StateFlow<AccessState> = _accessState.asStateFlow()
    
    sealed class AccessState {
        object Idle : AccessState()
        object Recognizing : AccessState()
        data class Recognized(val personName: String, val score: Int) : AccessState()
        data class Opening(val personName: String) : AccessState()
        data class Error(val message: String) : AccessState()
    }
    
    /**
     * 启动门禁监控通道
     */
    suspend fun startAccessMonitor() {
        Logger.d("启动门禁监控通道...")
        
        // 人脸识别结果流
        val faceDetectionFlow = faceEngine.detectionResults
            .map { result ->
                RpcMessage(
                    command = Command.DUPLEX_CHANNEL,
                    payload = AccessEvent(
                        type = AccessEvent.Type.FACE_DETECTED,
                        faceId = result.faceId,
                        confidence = result.score,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            .map { buildPayload { data(Json.encodeToString(it)) } }
        
        // 建立双向通道
        rSocket.requestChannel(faceDetectionFlow.asPublisher())
            .asFlow()
            .map { 
                Json.decodeFromString<RpcMessage<AccessControlResult>>(it.data.readText()) 
            }
            .collect { response ->
                handleControlResult(response.payload!!)
            }
    }
    
    private suspend fun handleControlResult(result: AccessControlResult) {
        when (result.action) {
            "OPEN_DOOR" -> {
                _accessState.value = AccessState.Opening(result.personName)
                
                // 执行开门
                doorController.open()
                
                // 播放欢迎语音
                VoiceHelper.speak("欢迎，${result.personName}")
                
                // 3秒后关门
                delay(3000)
                doorController.close()
                _accessState.value = AccessState.Idle
            }
            "DENY" -> {
                _accessState.value = AccessState.Error("未授权")
                VoiceHelper.speak("未授权，禁止通行")
                delay(2000)
                _accessState.value = AccessState.Idle
            }
        }
    }
    
    /**
     * 应急开门（本地按钮）
     */
    suspend fun emergencyOpen() {
        val event = RpcMessage(
            command = Command.DUPLEX_CHANNEL,
            payload = AccessEvent(
                type = AccessEvent.Type.EMERGENCY_OPEN,
                timestamp = System.currentTimeMillis()
            )
        )
        
        // 发送应急事件
        rSocket.fireAndForget(
            buildPayload { data(Json.encodeToString(event)) }
        )
        
        // 本地立即开门
        doorController.open()
    }
}

// 在 Activity 中使用
class MainActivity : AppCompatActivity() {
    private val viewModel: AccessViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            viewModel.accessState.collect { state ->
                when (state) {
                    is AccessControlManager.AccessState.Recognizing -> {
                        binding.statusText.text = "识别中..."
                        binding.statusImage.setImageResource(R.drawable.ic_scanning)
                    }
                    is AccessControlManager.AccessState.Opening -> {
                        binding.statusText.text = "欢迎，${state.personName}"
                        binding.statusImage.setImageResource(R.drawable.ic_welcome)
                    }
                    // ...
                }
            }
        }
    }
}
```

### 8.4 场景四：课表实时同步（多端推送）

**服务端（Java）**
```java
@Service
@Slf4j
public class TimeTableSyncService {
    
    private final Sinks.Many<TimeTableChangeEvent> changeSink = 
        Sinks.many().multicast().onBackpressureBuffer();
    
    /**
     * 课表变更时触发
     */
    public void updateTimeTable(String roomId, TimeTable newTable) {
        timeTableRepository.save(newTable)
            .doOnSuccess(saved -> {
                TimeTableChangeEvent event = new TimeTableChangeEvent(roomId, saved);
                changeSink.tryEmitNext(event);
                log.info("课表已更新并推送: {}", roomId);
            })
            .subscribe();
    }
    
    @MessageMapping("timetable.subscribe.{roomId}")
    public Flux<RpcMessage<TimeTable>> subscribeTimeTable(
            @DestinationVariable String roomId) {
        
        return changeSink.asFlux()
            .filter(event -> event.getRoomId().equals(roomId))
            .map(event -> RpcMessage.success(
                Command.SUBSCRIBE_TIME_TABLE_UPDATES, 
                event.getTimeTable()
            ))
            .doOnSubscribe(s -> log.info("房间 {} 开始订阅课表", roomId))
            .doOnCancel(() -> log.info("房间 {} 取消订阅", roomId));
    }
}
```

**客户端（Kotlin）**
```kotlin
class TimeTableViewModel(
    private val client: ClassTimeTableRSocketClient
) : ViewModel() {
    
    private val _currentCourse = MutableStateFlow<TimeTable?>(null)
    val currentCourse: StateFlow<TimeTable?> = _currentCourse.asStateFlow()
    
    private val _nextCourse = MutableStateFlow<TimeTable?>(null)
    val nextCourse: StateFlow<TimeTable?> = _nextCourse.asStateFlow()
    
    init {
        viewModelScope.launch {
            client.observeTimeTableUpdates()
                .collect { table ->
                    updateCourseDisplay(table)
                }
        }
        
        // 定时更新当前课程状态
        viewModelScope.launch {
            while (isActive) {
                updateCurrentCourseStatus()
                delay(60000)  // 每分钟检查
            }
        }
    }
    
    private fun updateCourseDisplay(table: TimeTable) {
        val now = LocalDateTime.now()
        
        when {
            now.isAfter(table.startTime) && now.isBefore(table.endTime) -> {
                // 当前正在上这门课
                _currentCourse.value = table
                _nextCourse.value = null
            }
            now.isBefore(table.startTime) -> {
                // 这是下一节课
                _nextCourse.value = table
            }
        }
    }
    
    private fun updateCurrentCourseStatus() {
        val current = _currentCourse.value ?: return
        val now = LocalDateTime.now()
        
        if (now.isAfter(current.endTime)) {
            // 当前课程已结束，显示课间或下一节
            _currentCourse.value = null
        }
    }
}

// Composable UI
@Composable
fun TimeTableDisplay(viewModel: TimeTableViewModel) {
    val current by viewModel.currentCourse.collectAsState()
    val next by viewModel.nextCourse.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        if (current != null) {
            CurrentCourseCard(course = current!!)
        } else if (next != null) {
            NextCourseCard(course = next!!)
        } else {
            IdleCard()
        }
    }
}
```

---

## 9. 最佳实践

### 9.1 连接生命周期管理

```kotlin
class RSocketLifecycleManager(
    private val context: Context
) : DefaultLifecycleObserver {
    
    private val client = ClassTimeTableRSocketClient(...)
    
    fun onResume() {
        // App 回到前台，确保连接
        CoroutineScope(Dispatchers.IO).launch {
            if (client.connectionState.value !is Connected) {
                client.connect()
            }
        }
    }
    
    fun onPause() {
        // App 进入后台，可以断开或保持
    }
    
    fun onDestroy() {
        client.disconnect()
    }
}
```

### 9.2 错误处理与重连

```kotlin
class ResilientRSocketClient {
    
    private val retryPolicy: Retry = Retry
        .fixedDelay(5, Duration.ofSeconds(2))
        .doBeforeRetry { 
            Logger.w("连接断开，准备重试... ${it.failure().message}")
        }
    
    suspend fun connectWithRetry() {
        flow { emit(connect()) }
            .retryWhen { cause, attempt ->
                if (attempt < 5) {
                    delay(1000 * attempt)
                    true
                } else false
            }
            .catch { e ->
                Logger.e("连接失败，进入离线模式: ${e.message}")
                enterOfflineMode()
            }
            .collect()
    }
}
```

---

## 10. 故障排查

### 10.1 连接问题

```kotlin
// Android 网络权限检查
if (ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) 
    != PackageManager.PERMISSION_GRANTED) {
    // 请求权限
}

// 检查网络连接
val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
val activeNetwork = cm.activeNetworkInfo
if (activeNetwork?.isConnected != true) {
    Logger.e("网络未连接")
}
```

### 10.2 序列化问题

```kotlin
// 确保 Kotlin Serialization 已注册
val json = Json {
    ignoreUnknownKeys = true  // 忽略未知字段
    isLenient = true          // 宽松模式
    encodeDefaults = true     // 编码默认值
}

// 使用方式
json.decodeFromString<RpcMessage<TimeTable>>(jsonString)
```

---

## 附录

### 参考资源

- [RSocket Kotlin GitHub](https://github.com/rsocket/rsocket-kotlin)
- [Ktor 文档](https://ktor.io/docs/client.html)
- [Kotlin Serialization](https://github.com/Kotlin/kotlinx.serialization)
- [Spring RSocket Reference](https://docs.spring.io/spring-framework/reference/rsocket.html)

### 版本信息

| 组件 | 版本 |
|------|------|
| RSocket Java | 1.1.4 |
| RSocket Kotlin | 0.15.4 |
| Ktor | 2.3.7 |
| Spring Boot | 3.5.5 |

---

*文档版本：v2.0*  
*最后更新：2026-03-18*