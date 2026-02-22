# tio-protocol 使用指南

> 通用 TCP 协议层 - QoS 机制实现与使用说明

## 目录

- [项目概述](#项目概述)
- [QoS 机制详解](#qos-机制详解)
- [协议格式](#协议格式)
- [Class-Time-Table 服务端使用](#class-time-table-服务端使用)
- [Android 客户端使用](#android-客户端使用)
- [完整交互流程](#完整交互流程)

---

## 项目概述

`tio-protocol` 是一个**纯 Java** 实现的 TCP 协议层，提供可靠的消息传输机制（QoS），适用于：

- **服务端**：配合 t-io 框架使用（如 class-time-table 模块）
- **客户端**：Android 电子门禁班牌设备
- **特点**：零框架依赖，纯 Java 实现，可被 Android 直接复用

### 核心功能

| 功能 | 说明 |
|------|------|
| 三级 QoS | AT_MOST_ONCE / AT_LEAST_ONCE / EXACTLY_ONCE |
| 超时重传 | 指数退避策略，可配置重传次数 |
| 消息去重 | EXACTLY_ONCE 级别的消息去重支持 |
| 协议编解码 | 固定头部 + 变长负载的二进制协议 |

---

## QoS 机制详解

### QoS 级别定义

```java
public enum QosLevel {
    AT_MOST_ONCE((byte) 0),   // 最多一次 - 不保证送达（心跳包）
    AT_LEAST_ONCE((byte) 1),  // 至少一次 - 保证送达，可能重复（推荐）
    EXACTLY_ONCE((byte) 2);   // 精确一次 - 保证送达且不重复（关键业务）
}
```

### 消息交互流程

#### 1. AT_MOST_ONCE (QoS=0)

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    
    C->>S: SEND (qos=0, 无需确认)
    Note over C,S: 发完即忘，不保证送达
```

**适用场景**：心跳包、状态上报（可容忍丢失）

---

#### 2. AT_LEAST_ONCE (QoS=1) - 最常用

```mermaid
sequenceDiagram
    participant C as Client
    participant QM_C as Client QoSManager
    participant QM_S as Server QoSManager
    participant S as Server
    
    Note over C,S: 客户端发送消息
    C->>QM_C: send(msg, qos=1)
    QM_C->>S: SEND (seqId=100)
    activate QM_C
    Note right of QM_C: 启动超时计时器
    
    S->>QM_S: 收到消息
    QM_S->>C: QOS_ACK (seqId=100)
    QM_S->>S: 递交给业务层
    
    C->>QM_C: 收到 ACK
    deactivate QM_C
    Note right of QM_C: 确认送达，停止计时
```

**适用场景**：指令下发、配置同步（推荐默认使用）

---

#### 3. EXACTLY_ONCE (QoS=2)

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    
    Note over C,S: 第一阶段：发送消息
    C->>S: SEND (seqId=100, qos=2)
    S->>C: QOS_ACK (seqId=100)
    
    Note over C,S: 服务端去重检查
    S->>S: 检查是否已处理 seqId=100
    alt 已处理
        S->>S: 丢弃重复消息
    else 未处理
        S->>S: 处理业务逻辑
    end
```

**适用场景**：支付、关键控制指令（不允许重复执行）

---

### 重传机制

```mermaid
sequenceDiagram
    participant C as Client
    participant QM as QoSManager
    participant S as Server
    
    C->>QM: send(msg, qos=1)
    activate QM
    QM->>S: SEND (seqId=100)
    
    Note right of QM: 等待 ACK（5秒）
    Note right of QM: 超时未收到...
    
    QM->>S: RESEND (seqId=100, retransmit标志)
    Note right of QM: 重传次数+1，超时时间翻倍（10秒）
    
    S->>C: QOS_ACK (seqId=100)
    C->>QM: 收到ACK
    deactivate QM
    Note right of QM: 确认送达，停止重传
```

**重传策略**：
- 默认超时：5秒
- 重传次数：最多 3 次
- 退避策略：指数退避（5s → 10s → 20s）

---

## 协议格式

### 数据包结构

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                           MAGIC (0x5A5A5A5A)                  |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
| VER | CMD   |    SEQ_ID       | QOS | FLAGS | RSV | CHECKSUM  |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                           LENGTH                              |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                                                               |
|                           PAYLOAD                             |
|                                                               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### 字段说明

| 字段 | 长度 | 说明 |
|------|------|------|
| MAGIC | 4 bytes | 魔数 `0x5A5A5A5A`，用于识别协议 |
| VER | 1 byte | 协议版本，当前为 `0x01` |
| CMD | 1 byte | 命令类型（见下文） |
| SEQ_ID | 2 bytes | 序列号（1-32767），用于 QoS 确认 |
| QOS | 1 byte | QoS 级别（0/1/2） |
| FLAGS | 1 byte | 标志位：`0x01=需要ACK`, `0x02=重传` |
| RSV | 1 byte | 保留字段 |
| CHECKSUM | 1 byte | 校验和（头部累加取低8位） |
| LENGTH | 4 bytes | 负载长度 |
| PAYLOAD | 变长 | 业务数据（JSON/Protobuf 等） |

### 命令类型

```java
public class CommandType {
    // QoS 确认
    public static final byte QOS_ACK = 0x00;
    
    // 注册
    public static final byte REGISTER = 0x01;
    public static final byte REGISTER_ACK = 0x02;
    
    // 心跳
    public static final byte HEARTBEAT = 0x10;
    public static final byte HEARTBEAT_ACK = 0x11;
    
    // 业务扩展（0x20-0xFF）
}
```

---

## Class-Time-Table 服务端使用

### 项目结构

```
class_time_table/
├── t_io/
│   ├── adapter/
│   │   ├── TioPacketAdapter.java      # 适配 t-io Packet
│   │   └── TioQosAdapter.java         # QoS 与 t-io 集成
│   ├── codec/
│   │   └── TioProtocolCodec.java      # 编解码器
│   ├── config/
│   │   ├── TioServerConfig.java       # 服务器配置
│   │   └── TioServerProperties.java   # 配置属性
│   ├── handler/
│   │   └── SmartBoardTioHandler.java  # 消息处理器
│   └── listener/
│       └── SmartBoardTioListener.java # 连接监听器
```

### 1. 处理客户端注册

```java
@Component
public class SmartBoardTioHandler implements TioServerHandler {
    
    @Autowired
    private TioQosAdapter qosAdapter;
    @Autowired
    private PacketBuilder packetBuilder;
    
    @Override
    public void handler(Packet packet, ChannelContext ctx) throws Exception {
        TioPacketAdapter adapter = (TioPacketAdapter) packet;
        ProtocolPacket protocolPacket = adapter.getProtocolPacket();
        
        // 1. QoS 层自动处理 ACK（如果客户端要求确认）
        qosAdapter.handleReceived(ctx, adapter);
        
        // 2. 业务分发
        switch (protocolPacket.getCmdType()) {
            case CommandType.REGISTER:
                handleRegister(ctx, protocolPacket);
                break;
            // ... 其他命令
        }
    }
    
    private void handleRegister(ChannelContext ctx, ProtocolPacket packet) {
        // 解析注册请求
        RegisterRequest request = parse(packet.getPayload());
        
        // 验证设备
        if (!validate(request)) {
            Tio.close(ctx, "非法设备");
            return;
        }
        
        // 绑定设备ID到通道
        ctx.setBsId(request.getDeviceId());
        
        // 构造配置响应（qos=1，确保送达）
        DeviceConfig config = loadConfig(request.getDeviceId());
        ProtocolPacket ackPacket = packetBuilder.build(
            CommandType.REGISTER_ACK,
            serialize(config),
            QosLevel.AT_LEAST_ONCE  // 重要：qos=1
        );
        
        // 发送配置（自动处理重传）
        qosAdapter.send(ctx, ackPacket);
        
        log.info("设备 {} 注册成功，配置已下发", request.getDeviceId());
    }
}
```

### 2. 主动推送消息

```java
@Service
public class DevicePushService {
    
    @Autowired
    private TioQosAdapter qosAdapter;
    @Autowired
    private PacketBuilder packetBuilder;
    
    /**
     * 向设备推送课表更新
     */
    public void pushTimetable(String deviceId, Timetable data) {
        // 查找设备通道
        ChannelContext ctx = findChannel(deviceId);
        if (ctx == null) {
            log.warn("设备 {} 离线，推送失败", deviceId);
            return;
        }
        
        // 构造推送消息（qos=1）
        ProtocolPacket packet = packetBuilder.build(
            CommandType.TIMETABLE_PUSH,
            serialize(data),
            QosLevel.AT_LEAST_ONCE
        );
        
        // 发送（QoS 管理器自动处理确认和重传）
        boolean sent = qosAdapter.send(ctx, packet);
        if (sent) {
            log.info("课表已推送给设备 {}，等待确认...", deviceId);
        }
    }
}
```

### 3. 配置参数

```yaml
# application.yaml
t-io:
  server:
    name: smart-board-server
    host: 0.0.0.0
    port: 9000
    heartbeat-timeout: 60000  # 心跳超时（毫秒）
```

---

## Android 客户端使用

### 1. 引入依赖

#### 方式一：源码复制（推荐）

将 `tio-protocol/src/main/java` 下的所有 Java 文件复制到 Android 项目的 `java/xyz/jasenon/lab/tioprotocol` 目录。

#### 方式二：打包 AAR

```groovy
// build.gradle (Module: app)
dependencies {
    implementation 'xyz.jasenon.lab:tio-protocol:0.0.1-SNAPSHOT'
    implementation 'org.slf4j:slf4j-android:2.0.16'  // SLF4J Android 实现
}
```

### 2. 实现 PacketSender

```java
public class TcpConnection implements QosManager.PacketSender {
    
    private Socket socket;
    private OutputStream outputStream;
    private PacketCodec codec = new PacketCodec();
    private QosManager qosManager;
    
    public TcpConnection() {
        // 创建 QoS 管理器，传入发送器实现
        this.qosManager = new QosManager(this::send);
        
        // 配置参数
        qosManager.setDefaultRetryTimeout(5000)
                  .setMaxRetryCount(3)
                  .setConfirmationRetentionTime(60000);
    }
    
    /**
     * 实现 PacketSender 接口
     * 实际发送数据到 socket
     */
    @Override
    public boolean send(String channelId, ProtocolPacket packet) {
        try {
            byte[] data = codec.encode(packet);
            outputStream.write(data);
            outputStream.flush();
            return true;
        } catch (IOException e) {
            Log.e("TcpConnection", "发送失败", e);
            return false;
        }
    }
    
    /**
     * 连接服务器
     */
    public void connect(String host, int port) {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 10000);
        outputStream = socket.getOutputStream();
        
        // 启动接收线程
        new Thread(this::receiveLoop).start();
    }
}
```

### 3. 注册流程实现

```java
public class DeviceRegistrar {
    
    private TcpConnection connection;
    private PacketBuilder packetBuilder = new PacketBuilder();
    private CompletableFuture<RegisterResult> pendingRegistration;
    
    /**
     * 发起设备注册
     * 
     * @param deviceId 设备ID
     * @param token 认证令牌
     * @return 注册结果（异步）
     */
    public CompletableFuture<RegisterResult> register(String deviceId, String token) {
        pendingRegistration = new CompletableFuture<>();
        
        // 构造注册请求（qos=1，确保服务端收到）
        RegisterRequest request = new RegisterRequest(deviceId, token);
        ProtocolPacket packet = packetBuilder.build(
            CommandType.REGISTER,
            serialize(request),
            QosLevel.AT_LEAST_ONCE  // qos=1
        );
        
        // 发送（QoS 管理器自动等待 ACK）
        connection.getQosManager().send("server", packet);
        
        // 设置超时
        pendingRegistration.orTimeout(15, TimeUnit.SECONDS)
            .exceptionally(e -> {
                Log.e("DeviceRegistrar", "注册超时", e);
                return RegisterResult.fail("超时");
            });
        
        return pendingRegistration;
    }
    
    /**
     * 处理收到的消息
     */
    public void onPacketReceived(ProtocolPacket packet) {
        // 交给 QoS 管理器处理（自动回复 ACK）
        boolean isAck = connection.getQosManager().handleReceived("server", packet);
        if (isAck) {
            // 这是协议层 ACK，已自动处理
            Log.d("DeviceRegistrar", "收到 ACK，消息已送达");
            return;
        }
        
        // 处理业务响应
        switch (packet.getCmdType()) {
            case CommandType.REGISTER_ACK:
                handleRegisterAck(packet);
                break;
                
            case CommandType.TIMETABLE_PUSH:
                handleTimetablePush(packet);
                break;
                
            case CommandType.HEARTBEAT_ACK:
                // 心跳响应，更新连接状态
                break;
        }
    }
    
    private void handleRegisterAck(ProtocolPacket packet) {
        // 解析配置
        DeviceConfig config = deserialize(packet.getPayload(), DeviceConfig.class);
        
        // 保存配置
        ConfigManager.save(config);
        
        // 完成注册流程
        pendingRegistration.complete(RegisterResult.success(config));
        
        Log.i("DeviceRegistrar", "注册成功，配置已保存");
    }
    
    private void handleTimetablePush(ProtocolPacket packet) {
        Timetable timetable = deserialize(packet.getPayload(), Timetable.class);
        
        // 保存课表
        TimetableManager.save(timetable);
        
        Log.i("DeviceRegistrar", "收到课表更新");
    }
}
```

### 4. 心跳保活

```java
public class HeartbeatManager {
    
    private TcpConnection connection;
    private PacketBuilder packetBuilder = new PacketBuilder();
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    /**
     * 启动心跳
     */
    public void start() {
        scheduler.scheduleAtFixedRate(
            this::sendHeartbeat,
            0, 30, TimeUnit.SECONDS  // 每30秒发送一次
        );
    }
    
    private void sendHeartbeat() {
        // 构造心跳包（qos=0，不需要确认）
        ProtocolPacket heartbeat = packetBuilder.build(
            CommandType.HEARTBEAT,
            new byte[0],
            QosLevel.AT_MOST_ONCE  // qos=0，减少网络开销
        );
        
        // 发送（不等待确认）
        connection.send(heartbeat);
    }
}
```

### 5. 完整使用示例

```java
public class MainActivity extends AppCompatActivity {
    
    private TcpConnection connection;
    private DeviceRegistrar registrar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化连接
        connection = new TcpConnection();
        registrar = new DeviceRegistrar(connection);
        
        // 连接服务器并注册
        new Thread(() -> {
            try {
                connection.connect("10.0.2.2", 9000);
                
                RegisterResult result = registrar.register("DEVICE001", "token123")
                    .get(15, TimeUnit.SECONDS);
                
                if (result.isSuccess()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e("MainActivity", "连接失败", e);
            }
        }).start();
    }
}
```

---

## 完整交互流程

### 设备注册 + 配置下发（带 QoS）

```mermaid
sequenceDiagram
    autonumber
    participant A as Android班牌
    participant QC as Client QoSManager
    participant S as t-io Server
    participant QS as Server QoSManager
    participant H as 业务Handler
    
    Note over A,S: ========== 建立连接 ==========
    A->>S: TCP 连接
    S->>A: 连接成功
    
    Note over A,S: ========== 注册流程 ==========
    A->>QC: register(deviceId, token)
    Note right of QC: 构造 REGISTER 包<br/>(qos=1, seqId=100)
    
    QC->>S: REGISTER (seqId=100, qos=1)
    activate QC
    Note right of QC: 启动超时计时器(5s)
    
    S->>QS: 收到 REGISTER
    QS->>A: QOS_ACK (seqId=100)
    A->>QC: 收到 ACK
    deactivate QC
    Note right of QC: 确认送达
    
    QS->>H: 递交给业务层
    
    H->>H: 验证设备合法性
    H->>H: 生成配置数据
    
    H->>QS: 发送配置 (REGISTER_ACK, qos=1, seqId=200)
    activate QS
    Note right of QS: 启动超时计时器
    
    QS->>A: REGISTER_ACK (seqId=200, payload=配置)
    
    A->>QC: 收到 REGISTER_ACK
    QC->>S: QOS_ACK (seqId=200)
    S->>QS: 收到 ACK
    deactivate QS
    Note right of QS: 确认送达
    
    QC->>A: 递交业务层
    A->>A: 保存配置
    Note over A: 注册完成，设备在线
    
    Note over A,S: ========== 心跳保活 ==========
    loop 每30秒
        A->>S: HEARTBEAT (qos=0)
        Note right of A: 无需确认
        S->>A: HEARTBEAT_ACK (可选)
    end
    
    Note over A,S: ========== 服务端主动推送 ==========
    H->>QS: 课表更新 (TIMETABLE_PUSH, qos=1)
    activate QS
    QS->>A: TIMETABLE_PUSH
    A->>QC: 收到推送
    QC->>S: QOS_ACK
    S->>QS: 收到确认
    deactivate QS
    A->>A: 更新课表显示
```

### 异常处理：超时重传

```mermaid
sequenceDiagram
    participant A as Android班牌
    participant QC as Client QoSManager
    participant S as 服务器
    
    A->>QC: 发送消息 (seqId=100)
    activate QC
    
    QC->>S: SEND
    Note right of QC: 等待 ACK (5s)
    
    Note over S: 网络丢包
    
    Note right of QC: 超时未收到...
    QC->>S: RESEND (seqId=100, 重传标志)
    Note right of QC: 重试次数: 1<br/>超时时间: 10s
    
    Note over S: 再次丢包
    
    Note right of QC: 超时未收到...
    QC->>S: RESEND (seqId=100, 重传标志)
    Note right of QC: 重试次数: 2<br/>超时时间: 20s
    
    S->>A: QOS_ACK
    A->>QC: 收到确认
    deactivate QC
    Note right of QC: 停止重传
```

---

## 最佳实践

### 1. QoS 选择建议

| 场景 | 推荐 QoS | 原因 |
|------|----------|------|
| 心跳包 | AT_MOST_ONCE (0) | 高频发送，允许偶尔丢失 |
| 注册/配置 | AT_LEAST_ONCE (1) | 必须送达，业务可容忍重复 |
| 开门指令 | EXACTLY_ONCE (2) | 关键操作，不能重复执行 |
| 课表同步 | AT_LEAST_ONCE (1) | 数据重要，但可覆盖更新 |

### 2. 序列号管理

```java
// 每个连接独立维护序列号
SeqIdGenerator generator = new SeqIdGenerator();

// 发送消息时自动生成
short seqId = generator.nextSeqId();
```

### 3. 幂等性设计

服务端处理消息时，需要支持幂等（防止重复处理）：

```java
private Set<String> processedMessages = ConcurrentHashMap.newKeySet();

public void handleMessage(ProtocolPacket packet) {
    String key = packet.getChannelId() + ":" + packet.getSeqId();
    
    if (!processedMessages.add(key)) {
        // 已处理过，直接返回
        log.debug("重复消息，忽略: {}", key);
        return;
    }
    
    // 处理业务逻辑...
}
```

### 4. 连接状态管理

```java
public enum ConnectionState {
    DISCONNECTED,   // 未连接
    CONNECTING,     // 连接中
    CONNECTED,      // 已连接
    REGISTERING,    // 注册中
    REGISTERED,     // 已注册（正常工作）
    RECONNECTING    // 重连中
}
```

---

## 常见问题

### Q1: 为什么 QOS_ACK 和业务响应是两个不同的包？

**A**: 
- `QOS_ACK` 是**协议层**的确认，由 `QosManager` 自动处理，确保消息送达
- `REGISTER_ACK` 是**业务层**的响应，携带业务数据（配置），由业务代码构造

两者职责分离，不要混淆。

### Q2: 如果 REGISTER_ACK 丢失怎么办？

**A**: 
- 服务端发送 `REGISTER_ACK` 时设置了 `qos=1`，会自动重传直到收到客户端的 `QOS_ACK`
- 客户端可能会收到重复的 `REGISTER_ACK`，需要幂等处理（如根据 seqId 去重）

### Q3: Android 端如何检测连接断开？

**A**: 
- 心跳超时检测：如果连续 N 次心跳无响应，认为连接断开
- Socket 异常捕获：在 `receiveLoop` 中捕获 `IOException`
- 重连策略：指数退避重连（1s → 2s → 4s → ... → 60s）

```java
private void receiveLoop() {
    try {
        while (isConnected) {
            ProtocolPacket packet = codec.decode(inputStream);
            onPacketReceived(packet);
        }
    } catch (IOException e) {
        Log.e("TcpConnection", "连接断开", e);
        onDisconnected();
        scheduleReconnect();  // 调度重连
    }
}
```

### Q4: 如何支持更大的 payload（如图片）？

**A**: 
当前协议 payload 长度用 4 字节表示，最大支持 **2GB**。如需分片传输：

1. 业务层自行分片（推荐）：将图片分成多个 chunk，每个 chunk 作为一个消息发送
2. 或使用 `EXACTLY_ONCE` 确保每个 chunk 不丢失

```java
// 分片发送示例
List<byte[]> chunks = splitImage(imageData, 64 * 1024); // 64KB per chunk
for (int i = 0; i < chunks.size(); i++) {
    ImageChunk chunk = new ImageChunk(i, chunks.size(), chunks.get(i));
    send(CommandType.IMAGE_CHUNK, serialize(chunk), QosLevel.AT_LEAST_ONCE);
}
```

---

## 相关文档

- [tio-protocol README](./README.md)
- [class-time-table README](../class_time_table/README.md)
- [t-io 官方文档](https://www.tiocloud.com/doc/tio/?dir=tio%E6%A0%B8%E5%BF%83/1.%E4%BB%8B%E7%BB%8D)

---

**作者**: Jasenon_ce  
**版本**: 1.0  
**日期**: 2026-02-22
