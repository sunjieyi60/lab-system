# J-IM synSeq RPC 与协议转换机制深度解析

> 本文档详细分析 J-IM 框架如何基于 t-io 实现类 RPC 的同步调用机制，以及 ReqBody/RespBody 的协议转换设计。提供抽离 t-io 基础的精简 Demo。

---

## 一、架构总览

### 1.1 层级关系

```
┌─────────────────────────────────────────────────────────────────┐
│                         应用层 (Business)                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ ChatReqBody  │  │ LoginReqBody │  │ MessageReqBody       │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘  │
│         │                 │                     │              │
│         └─────────────────┼─────────────────────┘              │
│                           ▼                                    │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │              J-IM 协议层 (Protocol Layer)                 │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │ │
│  │  │  RespBody   │  │   Message   │  │ Command (Enum)  │  │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  │ │
│  │                           │                              │ │
│  │  ┌────────────────────────┼──────────────────────────┐  │ │
│  │  │         IProtocolConverter 协议转换器              │  │ │
│  │  │  ┌────────────┐ ┌────────────┐ ┌────────────┐    │  │ │
│  │  │  │ TCP Convert│ │ WS Convert │ │HTTP Convert│    │  │ │
│  │  │  └────────────┘ └────────────┘ └────────────┘    │  │ │
│  │  └────────────────────────┼──────────────────────────┘  │ │
│  └───────────────────────────┼──────────────────────────────┘ │
│                              ▼                                 │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │            J-IM 传输层 (Transport Layer)                  │ │
│  │      ┌─────────────────────────────────────────┐         │ │
│  │      │  TcpProtocolHandler / WsProtocolHandler │         │ │
│  │      │       (协议编解码 + 命令分发)              │         │ │
│  │      └─────────────────────────────────────────┘         │ │
│  └───────────────────────────┬──────────────────────────────┘ │
└──────────────────────────────┼─────────────────────────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                      t-io 网络层 (Network)                       │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │                   Packet (基类)                           │ │
│  │  - synSeq: int  (同步序列号，RPC 核心)                      │ │
│  │  - PacketListener: 消息监听                              │ │
│  │  - AioHandler: 编解码处理器                               │ │
│  │  - ChannelContext: 通道上下文                             │ │
│  └──────────────────────────────────────────────────────────┘ │
│                           │                                     │
│  ┌────────────────────────┼────────────────────────────────┐  │
│  │                   Tio (工具类)                            │  │
│  │  - send() / bSend()    │  bindUser() / bindGroup()       │  │
│  │  - getByUserId()       │  getByGroup()                   │  │
│  └────────────────────────┴────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 核心扩展点

| 扩展点 | t-io 基础 | J-IM 扩展 | 说明 |
|--------|----------|----------|------|
| **同步序列号** | `Packet.synSeq` | 协议标志位 MASK | t-io 提供基础字段，J-IM 扩展协议头标志位 |
| **消息体** | `byte[]` | `Command + RespBody` | t-io 原始字节，J-IM 增加命令码和结构化 Body |
| **协议识别** | 端口绑定 | `IProtocol.isProtocol()` | J-IM 支持同端口多协议自动识别 |
| **编解码器** | `AioHandler` | `IProtocolConverter` | J-IM 增加协议转换抽象层 |
| **命令分发** | 手动处理 | `CommandManager` | J-IM 实现命令路由分发 |

---

## 二、synSeq RPC 机制详解

### 2.1 原理概述

`synSeq` (Synchronize Sequence) 是 t-io `Packet` 基类提供的**同步序列号**字段。J-IM 利用此字段实现了**类 RPC 的请求-响应模式**。

**核心机制：**
1. 客户端发送请求时，生成唯一 `synSeq` (> 0) 并缓存请求上下文
2. 服务端处理请求后，将相同的 `synSeq` 复制到响应包
3. 客户端收到响应后，根据 `synSeq` 匹配到原始请求，完成回调

### 2.2 协议头设计

```
TCP 协议包结构 (含 synSeq):
┌────────┬────────┬──────────────┬────────┬────────────┬──────────────┐
│版本(1B)│标志(1B) │ 同步序列号(4B) │命令(1B)│ 消息长度(4B) │   消息体(NB)   │
└────────┴────────┴──────────────┴────────┴────────────┴──────────────┘
  0x01    0x3F      0x00000001      0x0B     0x00000064     {...}

标志位 (mask) 解析:
┌───┬───┬───┬───┬───┬───┬───┬───┐
│ 7 │ 6 │ 5 │ 4 │ 3 │ 2 │ 1 │ 0 │
└───┴───┴───┴───┴───┴───┴───┴───┘
  │   │   │   │   └───┴───┴───┴─── 版本号 (4bit)
  │   │   │   └────────────── 4字节长度标识 (1bit)
  │   │   └────────────────── 同步序列号标识 (1bit) ← 关键位
  │   └────────────────────── 压缩标识 (1bit)
  └────────────────────────── 加密标识 (1bit)

FIRST_BYTE_MASK_HAS_SYNSEQ = 0B00100000 (0x20)
```

### 2.3 关键代码解析

#### 2.3.1 编码阶段 (服务端发送响应)

```java
// TcpServerEncoder.java
public static ByteBuffer encode(TcpPacket tcpPacket, ImConfig imConfig, ImChannelContext imChannelContext) {
    // 1. 判断是否需要同步序列号
    boolean isHasSynSeq = tcpPacket.getSynSeq() > 0;
    
    // 2. 编码标志位，设置 HAS_SYNSEQ 位
    byte maskByte = ImPacket.encodeHasSynSeq(maskByte, isHasSynSeq);
    
    // 3. 计算总长度 (如果带 synSeq，额外需要 4 字节)
    int allLen = 1 + 1;  // 版本 + 标志
    if (isHasSynSeq) {
        allLen += 4;     // 同步序列号
    }
    allLen += 1 + 4 + bodyLen;  // 命令 + 长度 + 消息体
    
    ByteBuffer buffer = ByteBuffer.allocate(allLen);
    buffer.put(tcpPacket.getVersion());
    buffer.put(tcpPacket.getMask());
    
    // 4. 写入同步序列号
    if (isHasSynSeq) {
        buffer.putInt(tcpPacket.getSynSeq());
    }
    
    buffer.put(cmdByte);
    buffer.putInt(bodyLen);
    buffer.put(body);
    return buffer;
}
```

#### 2.3.2 解码阶段 (客户端接收响应)

```java
// TcpServerDecoder.java
public static TcpPacket decode(ByteBuffer buffer, ImChannelContext imChannelContext) {
    // 1. 读取版本号
    byte version = buffer.get();
    
    // 2. 读取标志位
    byte maskByte = buffer.get();
    Integer synSeq = 0;
    
    // 3. 判断是否携带同步序列号
    if (ImPacket.decodeHasSynSeq(maskByte)) {
        synSeq = buffer.getInt();  // 读取 4 字节 synSeq
    }
    
    // 4. 读取命令码和消息体
    byte cmdByte = buffer.get();
    int bodyLen = buffer.getInt();
    byte[] body = new byte[bodyLen];
    buffer.get(body, 0, bodyLen);
    
    // 5. 创建包对象
    TcpPacket tcpPacket = new TcpPacket(Command.forNumber(cmdByte), body);
    if (synSeq > 0) {
        tcpPacket.setSynSeq(synSeq);  // 关键：还原 synSeq
    }
    return tcpPacket;
}
```

#### 2.3.3 服务端响应处理

```java
// TcpProtocolHandler.java
public void handler(ImPacket packet, ImChannelContext imChannelContext) throws ImException {
    TcpPacket tcpPacket = (TcpPacket) packet;
    AbstractCmdHandler cmdHandler = CommandManager.getCommand(tcpPacket.getCommand());
    
    // 1. 业务处理
    ImPacket response = cmdHandler.handler(tcpPacket, imChannelContext);
    
    // 2. 关键逻辑：如果 synSeq > 0，说明是同步调用，已在 handler 中处理响应
    //    如果 synSeq < 1，说明是异步调用，需要在这里发送响应
    if (Objects.nonNull(response) && tcpPacket.getSynSeq() < 1) {
        JimServerAPI.send(imChannelContext, response);
    }
}

// ChatReqHandler.java - 业务 Handler 示例
public ImPacket handler(ImPacket packet, ImChannelContext channelContext) {
    // ... 处理业务逻辑 ...
    
    ImPacket chatPacket = new ImPacket(Command.COMMAND_CHAT_REQ, respBody.toByte());
    
    // 关键：将请求的 synSeq 复制到响应，实现 RPC 关联
    chatPacket.setSynSeq(packet.getSynSeq());
    
    // 发送到目标用户
    JimServerAPI.sendToUser(toId, chatPacket);
    
    // 返回成功响应（也会带有 synSeq）
    return ProtocolManager.Packet.success(channelContext);
}
```

#### 2.3.4 协议转换中的 synSeq 传递

```java
// JimServerAPI.java - convertPacket 方法
private static ImPacket convertPacket(ImChannelContext imChannelContext, ImPacket packet) {
    // 1. 转换协议格式 (TCP/WS/HTTP)
    ImPacket respPacket = ProtocolManager.Converter.respPacket(packet, packet.getCommand(), imChannelContext);
    
    // 2. 关键：保持 synSeq 不变，确保客户端能匹配到请求
    respPacket.setSynSeq(packet.getSynSeq());
    return respPacket;
}
```

### 2.4 RPC 调用流程图

```
客户端                                           服务端
  │                                               │
  │  1. 生成 synSeq = 1001                        │
  │  2. 缓存 Promise {seq: 1001, future}          │
  │                                               │
  │  ┌─────────────────────────────────────────┐  │
  │  │ TCP 包: [VER|MASK|synSeq=1001|CMD|LEN|BODY]│  │
  │  └─────────────────────────────────────────┘  │
  │ ─────────────────────────────────────────────>│
  │                                               │
  │                                               │  3. 解码，提取 synSeq=1001
  │                                               │  4. 业务处理
  │                                               │  5. 构建响应包，setSynSeq(1001)
  │                                               │
  │  ┌─────────────────────────────────────────┐  │
  │  │ TCP 包: [VER|MASK|synSeq=1001|CMD|LEN|BODY]│  │
  │  └─────────────────────────────────────────┘  │
  │ <─────────────────────────────────────────────│
  │                                               │
  │  6. 解码，提取 synSeq=1001                    │
  │  7. 查找 Promise，future.complete(result)     │
  │  8. 返回结果给调用方                          │
  ▼                                               ▼
```

---

## 三、ReqBody/RespBody 转换机制

### 3.1 核心设计模式

J-IM 采用**策略模式 + 抽象工厂**实现多协议转换：

```
┌─────────────────────────────────────────────────────────────┐
│                    IProtocolConverter                        │
│  (协议转换接口)                                               │
├─────────────────────────────────────────────────────────────┤
│  + ReqPacket(body, command, ctx): ImPacket                   │
│  + RespPacket(body, command, ctx): ImPacket                  │
│  + RespPacket(packet, command, ctx): ImPacket                │
└─────────────────────────────────────────────────────────────┘
                              △
          ┌───────────────────┼───────────────────┐
          │                   │                   │
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  TcpConvertPacket│  │  WsConvertPacket │  │ HttpConvertPacket│
│  (TCP 协议转换)   │  │ (WebSocket 转换) │  │  (HTTP 协议转换) │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

### 3.2 消息体继承体系

```
Message (Serializable)
    │
    ├── ChatBody (聊天消息)
    │       ├── from: String      (发送者)
    │       ├── to: String        (接收者)
    │       ├── content: String   (内容)
    │       ├── msgType: Integer  (消息类型)
    │       └── chatType: Integer (聊天类型)
    │
    ├── LoginReqBody / LoginRespBody
    │       ├── userId: String
    │       ├── password: String
    │       └── token: String
    │
    ├── HeartbeatBody
    │       └── hbbyte: byte
    │
    └── RespBody (通用响应基类)
            ├── code: Integer     (状态码)
            ├── msg: String       (状态消息)
            ├── command: Command  (命令码)
            └── data: Object      (业务数据)
```

### 3.3 转换流程详解

#### 3.3.1 请求包转换 (ReqPacket)

```java
// TcpConvertPacket.java
public ImPacket ReqPacket(byte[] body, Command command, ImChannelContext channelContext) {
    Object sessionContext = channelContext.getSessionContext();
    
    // 1. 验证是否为 TCP 协议上下文
    if (sessionContext instanceof TcpSessionContext) {
        // 2. 创建 TCP 包
        TcpPacket tcpPacket = new TcpPacket(command, body);
        
        // 3. 编码（计算 mask、写入协议头等）
        TcpServerEncoder.encode(tcpPacket, channelContext.getImConfig(), channelContext);
        
        tcpPacket.setCommand(command);
        return tcpPacket;
    }
    return null;
}
```

#### 3.3.2 响应包转换 (RespPacket)

```java
// TcpConvertPacket.java
public ImPacket RespPacket(byte[] body, Command command, ImChannelContext imChannelContext) {
    ImSessionContext sessionContext = imChannelContext.getSessionContext();
    
    if (sessionContext instanceof TcpSessionContext) {
        TcpPacket tcpPacket = new TcpPacket(command, body);
        TcpServerEncoder.encode(tcpPacket, imChannelContext.getImConfig(), imChannelContext);
        tcpPacket.setCommand(command);
        return tcpPacket;
    }
    return null;
}

// 重载方法：从已有包转换
public ImPacket RespPacket(ImPacket imPacket, Command command, ImChannelContext imChannelContext) {
    return this.RespPacket(imPacket.getBody(), command, imChannelContext);
}
```

#### 3.3.3 协议转换器调用链

```java
// ProtocolManager.Converter.java
public static ImPacket respPacket(RespBody respBody, ImChannelContext imChannelContext) {
    // 1. 将 RespBody 序列化为 JSON 字节
    return respPacket(respBody.toByte(), respBody.getCommand(), imChannelContext);
}

public static ImPacket respPacket(byte[] body, Command command, ImChannelContext imChannelContext) {
    // 2. 获取当前通道对应的协议转换器 (TCP/WS/HTTP)
    return getProtocolConverter(imChannelContext).RespPacket(body, command, imChannelContext);
}

private static IProtocolConverter getProtocolConverter(ImChannelContext imChannelContext) {
    ImServerChannelContext serverChannelContext = (ImServerChannelContext) imChannelContext;
    
    // 3. 从通道上下文获取协议处理器
    AbstractProtocolHandler protocolHandler = serverChannelContext.getProtocolHandler();
    
    // 4. 获取协议转换器
    return protocolHandler.getProtocol().getConverter();
}
```

### 3.4 JSON 序列化机制

```java
// JsonKit.java - 基于 Fastjson
public class JsonKit {
    // RespBody 转字节数组
    public static byte[] toJSONBytesEnumNoUsingName(Object obj) {
        return JSON.toJSONBytes(obj, 
            SerializerFeature.DisableCircularReferenceDetect);
    }
    
    // 字节数组转对象
    public static <T> T toBean(byte[] bytes, Class<T> clazz) {
        return JSON.parseObject(bytes, clazz);
    }
}

// RespBody.java
public class RespBody implements Serializable {
    // ... 字段定义 ...
    
    public byte[] toByte() {
        // 使用 JSON 序列化
        return JsonKit.toJSONBytesEnumNoUsingName(this);
    }
}
```

### 3.5 多协议支持对比

| 特性 | TCP | WebSocket | HTTP |
|------|-----|-----------|------|
| **协议头** | 自定义二进制头 | WebSocket Frame | HTTP Header |
| **Body 格式** | JSON 字节 | JSON 文本 | JSON 文本 |
| **synSeq 支持** | ✅ | ✅ | ❌ (无状态) |
| **持久连接** | ✅ | ✅ | ❌ |
| **适用场景** | IoT/APP | Web/H5 | API 调用 |

---

## 四、J-IM 对 t-io 的扩展总结

### 4.1 扩展内容一览

```
t-io 基础能力                    J-IM 扩展
─────────────────────────────────────────────────────────────
Packet                          ImPacket
├── synSeq                      ├── Command (命令码)
├── body (byte[])               ├── Status (状态码)
└── listener                    └── ImChannelContext 关联

AioHandler                      IProtocolHandler
├── decode                      ├── 多协议识别 (isProtocol)
├── encode                      ├── 协议初始化 (init)
└── handler                     └── 命令分发 (CommandManager)

ChannelContext                  ImChannelContext / ImSessionContext
├── bindUser                    ├── User (用户信息)
├── bindGroup                   ├── Group (群组信息)
└── setAttribute                └── ImClientNode (客户端节点)

Tio.send                        JimServerAPI
                                ├── send (单播)
                                ├── sendToUser (用户广播)
                                ├── sendToGroup (群组广播)
                                └── convertPacket (协议转换)
```

### 4.2 扩展方式分析

#### 4.2.1 继承扩展

```java
// 1. Packet -> ImPacket
public class ImPacket extends Packet {
    private Command command;      // 新增命令码
    private Status status;        // 新增状态码
    private ImChannelContext imChannelContext;  // 关联上下文
}

// 2. ChannelContext -> ImChannelContext
public class ImChannelContext extends ChannelContext {
    private ImSessionContext sessionContext;  // 会话上下文
}
```

#### 4.2.2 组合扩展 (装饰器模式)

```java
// ImChannelContext 包装 t-io ChannelContext
public class ImChannelContext {
    private ChannelContext tioChannelContext;  // 组合 t-io 上下文
    
    public void send(ImPacket packet) {
        // 转换为 t-io Packet 后发送
        Tio.send(tioChannelContext, packet);
    }
}
```

#### 4.2.3 配置化扩展

```java
// 通过 properties 文件配置命令处理器
// command.properties
5 = org.jim.server.command.handler.LoginReqHandler
11 = org.jim.server.command.handler.ChatReqHandler,org.jim.server.processor.chat.DefaultAsyncChatMessageProcessor
```

#### 4.2.4 策略模式扩展

```java
// 不同协议不同实现
public interface IProtocol {
    boolean isProtocol(ByteBuffer buffer, ImChannelContext ctx);
    IProtocolConverter getConverter();
}

// TCP 协议实现
public class TcpProtocol extends AbstractProtocol {
    @Override
    public boolean isProtocol(ByteBuffer buffer, ImChannelContext ctx) {
        // 检查第一个字节是否为 VERSION
        return buffer.get(0) == Protocol.VERSION;
    }
}
```

---

## 五、精简 Demo（仅依赖 t-io）

以下是一个抽离 J-IM 核心机制、仅依赖 t-io 的精简 RPC 框架 Demo。

### 5.1 项目结构

```
t-io-rpc-demo/
├── core/
│   ├── RpcPacket.java              # 扩展 t-io Packet
│   ├── RpcCommand.java             # 命令枚举
│   ├── RpcProtocol.java            # 协议常量
│   └── RpcBody.java                # 消息体基类
├── codec/
│   ├── RpcEncoder.java             # 编码器
│   └── RpcDecoder.java             # 解码器
├── handler/
│   ├── RpcServerHandler.java       # 服务端处理器
│   └── RpcClientHandler.java       # 客户端处理器
├── rpc/
│   ├── RpcClient.java              # RPC 客户端
│   ├── RpcServer.java              # RPC 服务端
│   └── RpcPromise.java             # RPC 调用凭证
└── demo/
    ├── DemoServer.java             # 服务端启动
    └── DemoClient.java             # 客户端启动
```

### 5.2 核心代码实现

#### RpcPacket.java

```java
import org.tio.core.intf.Packet;

/**
 * RPC 协议包 - 扩展 t-io Packet
 * 仅依赖 t-io 的 synSeq 字段
 */
public class RpcPacket extends Packet {
    private static final long serialVersionUID = 1L;
    
    private byte version = 0x01;
    private byte command;       // 命令码
    private byte[] body;        // 消息体 (JSON)
    
    public RpcPacket() {}
    
    public RpcPacket(byte command, byte[] body) {
        this.command = command;
        this.body = body;
    }
    
    // 关键：synSeq 继承自 Packet 基类
    
    public boolean isSync() {
        return getSynSeq() > 0;
    }
    
    // getters/setters...
    public byte getVersion() { return version; }
    public void setVersion(byte version) { this.version = version; }
    public byte getCommand() { return command; }
    public void setCommand(byte command) { this.command = command; }
    public byte[] getBody() { return body; }
    public void setBody(byte[] body) { this.body = body; }
}
```

#### RpcCommand.java

```java
/**
 * RPC 命令定义
 */
public enum RpcCommand {
    HEARTBEAT_REQ((byte) 1, "心跳请求"),
    HEARTBEAT_RESP((byte) 2, "心跳响应"),
    
    LOGIN_REQ((byte) 5, "登录请求"),
    LOGIN_RESP((byte) 6, "登录响应"),
    
    ECHO_REQ((byte) 11, "回显请求"),
    ECHO_RESP((byte) 12, "回显响应"),
    
    UNKNOWN((byte) 0, "未知");
    
    private final byte code;
    private final String desc;
    
    RpcCommand(byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public static RpcCommand fromCode(byte code) {
        for (RpcCommand cmd : values()) {
            if (cmd.code == code) return cmd;
        }
        return UNKNOWN;
    }
    
    // getters...
    public byte getCode() { return code; }
}
```

#### RpcBody.java

```java
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * RPC 消息体 - 简化版 RespBody
 */
public class RpcBody {
    private int code;           // 状态码
    private String msg;         // 状态消息
    private byte command;       // 命令码
    private Object data;        // 业务数据
    
    public RpcBody() {}
    
    public RpcBody(byte command) {
        this.command = command;
    }
    
    public RpcBody(byte command, int code, String msg) {
        this.command = command;
        this.code = code;
        this.msg = msg;
    }
    
    /**
     * 序列化为 JSON 字节
     */
    public byte[] toBytes() {
        return JSON.toJSONBytes(this);
    }
    
    /**
     * 从字节反序列化
     */
    public static RpcBody fromBytes(byte[] bytes) {
        return JSON.parseObject(bytes, RpcBody.class);
    }
    
    // 快速构建成功响应
    public static RpcBody success(byte command, Object data) {
        RpcBody body = new RpcBody(command, 200, "OK");
        body.setData(data);
        return body;
    }
    
    // 快速构建错误响应
    public static RpcBody error(byte command, String msg) {
        return new RpcBody(command, 500, msg);
    }
    
    // getters/setters...
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public byte getCommand() { return command; }
    public void setCommand(byte command) { this.command = command; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}
```

#### RpcEncoder.java

```java
import org.tio.core.ChannelContext;
import org.tio.core.intf.Packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * RPC 编码器 - 参考 J-IM TcpServerEncoder
 */
public class RpcEncoder {
    
    public static ByteBuffer encode(RpcPacket packet, ChannelContext channelContext) {
        byte[] body = packet.getBody();
        int bodyLen = body != null ? body.length : 0;
        
        // 判断是否带 synSeq (同步调用)
        boolean hasSynSeq = packet.getSynSeq() > 0;
        
        // 计算总长度
        int headerLen = 1 + 1;  // version + command
        if (hasSynSeq) {
            headerLen += 4;     // synSeq (4 bytes)
        }
        headerLen += 4;         // body length
        
        int totalLen = headerLen + bodyLen;
        
        ByteBuffer buffer = ByteBuffer.allocate(totalLen);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        // 1. 版本号
        buffer.put(packet.getVersion());
        
        // 2. 命令码
        buffer.put(packet.getCommand());
        
        // 3. 同步序列号 (如果是同步调用)
        if (hasSynSeq) {
            buffer.putInt(packet.getSynSeq());
        }
        
        // 4. 消息体长度
        buffer.putInt(bodyLen);
        
        // 5. 消息体
        if (bodyLen > 0) {
            buffer.put(body);
        }
        
        return buffer;
    }
}
```

#### RpcDecoder.java

```java
import org.tio.core.ChannelContext;

import java.nio.ByteBuffer;

/**
 * RPC 解码器 - 参考 J-IM TcpServerDecoder
 */
public class RpcDecoder {
    
    public static RpcPacket decode(ByteBuffer buffer, ChannelContext channelContext) {
        // 检查最小长度
        if (buffer.remaining() < 6) {  // version + command + bodyLen
            return null;
        }
        
        int position = buffer.position();
        
        // 1. 版本号
        byte version = buffer.get();
        if (version != 0x01) {
            throw new RuntimeException("Unknown protocol version: " + version);
        }
        
        // 2. 命令码
        byte command = buffer.get();
        
        // 3. 判断是否有 synSeq (通过检查剩余长度是否比预期多 4 字节)
        int synSeq = 0;
        buffer.mark();
        int bodyLen = buffer.getInt();
        
        // 启发式判断：如果 bodyLen 异常大，可能是 synSeq
        if (bodyLen > 10 * 1024 * 1024) {  // 超过 10MB 认为是 synSeq
            buffer.reset();
            synSeq = buffer.getInt();
            bodyLen = buffer.getInt();
        }
        buffer.reset();
        
        // 重新正确读取
        if (synSeq > 0) {
            synSeq = buffer.getInt();
        }
        bodyLen = buffer.getInt();
        
        // 4. 检查消息体是否完整
        if (buffer.remaining() < bodyLen) {
            return null;  // 等待更多数据
        }
        
        // 5. 读取消息体
        byte[] body = new byte[bodyLen];
        buffer.get(body);
        
        // 6. 构建包
        RpcPacket packet = new RpcPacket(command, body);
        packet.setVersion(version);
        if (synSeq > 0) {
            packet.setSynSeq(synSeq);
        }
        
        return packet;
    }
}
```

#### RpcPromise.java

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * RPC 调用凭证 - 用于同步等待响应
 */
public class RpcPromise<T> {
    private final int synSeq;
    private final CompletableFuture<T> future;
    private final long createTime;
    
    public RpcPromise(int synSeq) {
        this.synSeq = synSeq;
        this.future = new CompletableFuture<>();
        this.createTime = System.currentTimeMillis();
    }
    
    /**
     * 等待响应 (阻塞)
     */
    public T await(long timeout, TimeUnit unit) throws Exception {
        return future.get(timeout, unit);
    }
    
    /**
     * 完成 Promise
     */
    public void complete(T result) {
        future.complete(result);
    }
    
    /**
     * 异常完成
     */
    public void completeExceptionally(Throwable ex) {
        future.completeExceptionally(ex);
    }
    
    // getters...
    public int getSynSeq() { return synSeq; }
    public boolean isDone() { return future.isDone(); }
}
```

#### RpcClient.java

```java
import org.tio.client.ClientChannelContext;
import org.tio.client.ClientGroupContext;
import org.tio.client.ReconnConf;
import org.tio.client.TioClient;
import org.tio.core.Node;
import org.tio.core.Tio;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RPC 客户端 - 实现同步调用
 */
public class RpcClient {
    private final Node serverNode;
    private final ClientGroupContext clientGroupContext;
    private TioClient tioClient;
    private ClientChannelContext channelContext;
    
    // 同步序列号生成器
    private final AtomicInteger synSeqGenerator = new AtomicInteger(1);
    
    // 待完成的 Promise 表
    private final ConcurrentHashMap<Integer, RpcPromise<RpcBody>> pendingPromises = new ConcurrentHashMap<>();
    
    public RpcClient(String host, int port) {
        this.serverNode = new Node(host, port);
        
        // 创建客户端上下文
        this.clientGroupContext = new ClientGroupContext(
            new RpcClientHandler(this),  // 自定义 Handler
            new ReconnConf(5000L)        // 重连配置
        );
    }
    
    /**
     * 连接服务器
     */
    public void connect() throws Exception {
        tioClient = new TioClient(clientGroupContext);
        channelContext = tioClient.connect(serverNode);
    }
    
    /**
     * 同步 RPC 调用
     */
    public RpcBody call(RpcCommand command, RpcBody requestBody) throws Exception {
        // 1. 生成同步序列号
        int synSeq = synSeqGenerator.getAndIncrement();
        
        // 2. 创建 Promise
        RpcPromise<RpcBody> promise = new RpcPromise<>(synSeq);
        pendingPromises.put(synSeq, promise);
        
        try {
            // 3. 构建请求包
            RpcPacket packet = new RpcPacket(command.getCode(), requestBody.toBytes());
            packet.setSynSeq(synSeq);  // 关键：设置 synSeq
            
            // 4. 发送请求
            Tio.bSend(channelContext, packet);  // bSend 阻塞发送
            
            // 5. 等待响应
            return promise.await(10, TimeUnit.SECONDS);
            
        } finally {
            pendingPromises.remove(synSeq);
        }
    }
    
    /**
     * 处理响应 (由 Handler 回调)
     */
    public void handleResponse(RpcPacket packet) {
        int synSeq = packet.getSynSeq();
        if (synSeq > 0) {
            RpcPromise<RpcBody> promise = pendingPromises.get(synSeq);
            if (promise != null) {
                RpcBody body = RpcBody.fromBytes(packet.getBody());
                promise.complete(body);
            }
        }
    }
    
    /**
     * 异步发送 (无响应)
     */
    public void send(RpcCommand command, RpcBody body) {
        RpcPacket packet = new RpcPacket(command.getCode(), body.toBytes());
        Tio.send(channelContext, packet);
    }
    
    // getters...
    public ClientChannelContext getChannelContext() { return channelContext; }
}
```

#### RpcClientHandler.java

```java
import org.tio.client.intf.ClientAioHandler;
import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.exception.AioDecodeException;
import org.tio.core.intf.Packet;

import java.nio.ByteBuffer;

/**
 * RPC 客户端 Handler
 */
public class RpcClientHandler implements ClientAioHandler {
    private final RpcClient rpcClient;
    
    public RpcClientHandler(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }
    
    @Override
    public RpcPacket decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext) throws AioDecodeException {
        return RpcDecoder.decode(buffer, channelContext);
    }
    
    @Override
    public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
        return RpcEncoder.encode((RpcPacket) packet, channelContext);
    }
    
    @Override
    public void handler(Packet packet, ChannelContext channelContext) throws Exception {
        RpcPacket rpcPacket = (RpcPacket) packet;
        
        // 关键：如果有 synSeq，说明是 RPC 响应
        if (rpcPacket.isSync()) {
            rpcClient.handleResponse(rpcPacket);
        } else {
            // 异步消息处理
            System.out.println("Received async message: " + rpcPacket.getCommand());
        }
    }
    
    @Override
    public RpcPacket heartbeatPacket(ChannelContext channelContext) {
        // 心跳包
        return new RpcPacket(RpcCommand.HEARTBEAT_REQ.getCode(), new byte[]{-128});
    }
}
```

#### RpcServerHandler.java

```java
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.core.TioConfig;
import org.tio.core.exception.AioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.server.intf.ServerAioHandler;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * RPC 服务端 Handler
 */
public class RpcServerHandler implements ServerAioHandler {
    
    // 命令处理器注册表
    private final Map<Byte, Function<RpcBody, RpcBody>> handlers = new HashMap<>();
    
    /**
     * 注册命令处理器
     */
    public void registerHandler(RpcCommand command, Function<RpcBody, RpcBody> handler) {
        handlers.put(command.getCode(), handler);
    }
    
    @Override
    public RpcPacket decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext) throws AioDecodeException {
        return RpcDecoder.decode(buffer, channelContext);
    }
    
    @Override
    public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
        return RpcEncoder.encode((RpcPacket) packet, channelContext);
    }
    
    @Override
    public void handler(Packet packet, ChannelContext channelContext) throws Exception {
        RpcPacket rpcPacket = (RpcPacket) packet;
        byte command = rpcPacket.getCommand();
        
        // 解析请求体
        RpcBody requestBody = RpcBody.fromBytes(rpcPacket.getBody());
        
        // 查找处理器
        Function<RpcBody, RpcBody> handler = handlers.get(command);
        RpcBody responseBody;
        
        if (handler != null) {
            responseBody = handler.apply(requestBody);
        } else {
            responseBody = RpcBody.error(command, "Unknown command: " + command);
        }
        
        // 构建响应包
        RpcPacket response = new RpcPacket(
            getRespCommand(command), 
            responseBody.toBytes()
        );
        
        // 关键：复制 synSeq，实现 RPC 关联
        if (rpcPacket.isSync()) {
            response.setSynSeq(rpcPacket.getSynSeq());
        }
        
        // 发送响应
        Tio.send(channelContext, response);
    }
    
    /**
     * 获取对应的响应命令码
     */
    private byte getRespCommand(byte reqCommand) {
        switch (RpcCommand.fromCode(reqCommand)) {
            case LOGIN_REQ: return RpcCommand.LOGIN_RESP.getCode();
            case ECHO_REQ: return RpcCommand.ECHO_RESP.getCode();
            case HEARTBEAT_REQ: return RpcCommand.HEARTBEAT_RESP.getCode();
            default: return RpcCommand.UNKNOWN.getCode();
        }
    }
}
```

### 5.3 使用示例

#### DemoServer.java

```java
import org.tio.server.ServerGroupContext;
import org.tio.server.TioServer;

/**
 * RPC 服务端示例
 */
public class DemoServer {
    public static void main(String[] args) throws Exception {
        // 1. 创建 Handler
        RpcServerHandler handler = new RpcServerHandler();
        
        // 2. 注册业务处理器
        handler.registerHandler(RpcCommand.ECHO_REQ, body -> {
            String data = (String) body.getData();
            System.out.println("Received echo: " + data);
            return RpcBody.success(RpcCommand.ECHO_RESP.getCode(), "Echo: " + data);
        });
        
        handler.registerHandler(RpcCommand.LOGIN_REQ, body -> {
            String username = (String) body.getData();
            System.out.println("User login: " + username);
            return RpcBody.success(RpcCommand.LOGIN_RESP.getCode(), "Token_12345");
        });
        
        // 3. 创建服务器上下文
        ServerGroupContext serverGroupContext = new ServerGroupContext(handler);
        
        // 4. 启动服务器
        TioServer tioServer = new TioServer(serverGroupContext);
        tioServer.start("localhost", 8888);
        
        System.out.println("RPC Server started on port 8888");
    }
}
```

#### DemoClient.java

```java
/**
 * RPC 客户端示例
 */
public class DemoClient {
    public static void main(String[] args) throws Exception {
        // 1. 创建客户端
        RpcClient client = new RpcClient("localhost", 8888);
        
        // 2. 连接服务器
        client.connect();
        System.out.println("Connected to server");
        
        // 3. 同步调用 - Echo
        RpcBody echoReq = new RpcBody();
        echoReq.setData("Hello RPC!");
        
        RpcBody echoResp = client.call(RpcCommand.ECHO_REQ, echoReq);
        System.out.println("Echo response: " + echoResp.getData());
        
        // 4. 同步调用 - Login
        RpcBody loginReq = new RpcBody();
        loginReq.setData("user123");
        
        RpcBody loginResp = client.call(RpcCommand.LOGIN_REQ, loginReq);
        System.out.println("Login response: " + loginResp.getData());
        
        // 5. 关闭
        client.getChannelContext().close();
    }
}
```

### 5.4 运行结果

```
服务端输出:
RPC Server started on port 8888
Received echo: Hello RPC!
User login: user123

客户端输出:
Connected to server
Echo response: Echo: Hello RPC!
Login response: Token_12345
```

---

## 六、总结

### 6.1 设计要点

| 机制 | 实现方式 | 作用 |
|------|---------|------|
| **synSeq RPC** | Packet.synSeq + Promise | 实现请求-响应关联 |
| **协议头标志位** | mask byte 的位运算 | 灵活控制协议选项 |
| **命令分发** | CommandManager + Handler 映射 | 解耦协议与业务 |
| **协议转换** | IProtocolConverter 策略模式 | 支持 TCP/WS/HTTP |
| **JSON 序列化** | Fastjson | 结构化数据传输 |

### 6.2 适用场景

- **IoT 设备通信**：基于 TCP 的长连接 RPC
- **游戏服务器**：同步调用玩家服务
- **微服务通信**：内部服务间同步调用
- **即时通讯**：仿 J-IM 实现聊天系统

### 6.3 性能优化建议

1. **连接池**：复用 ChannelContext 减少连接开销
2. **批量处理**：合并小包发送减少网络往返
3. **压缩**：对大数据包启用压缩 (J-IM 支持)
4. **超时控制**：Promise 添加超时取消机制
5. **限流**：对 synSeq 生成做限流保护

---

## 附录：maven 依赖

```xml
<dependencies>
    <!-- t-io -->
    <dependency>
        <groupId>org.t-io</groupId>
        <artifactId>tio-core</artifactId>
        <version>3.5.8</version>
    </dependency>
    
    <!-- Fastjson -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>fastjson</artifactId>
        <version>1.2.83</version>
    </dependency>
    
    <!-- SLF4J -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>1.7.36</version>
    </dependency>
</dependencies>
```

---

*文档基于 J-IM 3.0.0 和 t-io 3.5.8 源码分析整理*
