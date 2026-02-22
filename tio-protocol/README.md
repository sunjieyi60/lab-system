# tio-protocol 模块

## 概述

`tio-protocol` 是一个**纯 Java** 的 TCP 协议层实现，提供：

- **协议编解码**：固定头部 + 变长负载的二进制协议
- **QoS 机制**：三级服务质量（AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE）
- **超时重传**：指数退避策略，可配置重传次数
- **消息去重**：EXACTLY_ONCE 级别的消息去重支持

## 特点

- **零依赖**：仅依赖 SLF4J 日志门面（provided 作用域）
- **框架无关**：不依赖 Spring、t-io 等任何框架
- **Android 兼容**：纯 Java 实现，可直接在 Android 项目使用

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>xyz.jasenon.lab</groupId>
    <artifactId>tio-protocol</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 2. 创建 QoS 管理器

```java
// 实现 PacketSender 接口
QosManager.PacketSender sender = (channelId, packet) -> {
    // 实际发送逻辑，返回是否发送成功
    return tcpClient.send(packet);
};

// 创建 QoS 管理器
QosManager qosManager = new QosManager(sender);

// 配置参数（可选）
qosManager.setDefaultRetryTimeout(5000)     // 5秒超时
          .setMaxRetryCount(3)               // 最多重传3次
          .setConfirmationRetentionTime(60000); // 确认保留1分钟
```

### 3. 发送消息

```java
// 构建数据包
ProtocolPacket packet = new PacketBuilder()
    .build(CommandType.HEARTBEAT, payload, QosLevel.AT_LEAST_ONCE);

// 发送（自动处理 QoS）
boolean sent = qosManager.send("channel-001", packet);
```

### 4. 处理收到的消息

```java
// 解码
PacketCodec codec = new PacketCodec();
ProtocolPacket packet = codec.decode(byteBuffer);

// 如果是 ACK，处理确认
if (packet.getCmdType() == CommandType.QOS_ACK) {
    qosManager.handleAck(channelId, packet.getSeqId());
    return;
}

// 如果需要确认，自动发送 ACK
qosManager.sendAckIfRequired(channelId, packet);

// 处理业务逻辑...
```

## 协议格式

```
| 字段      | 长度   | 说明                |
|-----------|--------|---------------------|
| magic     | 4字节  | 魔数 0x5A5A5A5A     |
| version   | 1字节  | 协议版本（当前为1） |
| cmdType   | 1字节  | 命令类型            |
| seqId     | 2字节  | 序列号（用于 QoS）  |
| qos       | 1字节  | QoS级别(0/1/2)      |
| flags     | 1字节  | 标志位              |
| reserved  | 1字节  | 保留字段            |
| checkSum  | 1字节  | 校验和              |
| length    | 4字节  | 负载长度            |
| payload   | 变长   | 负载数据            |
```

## QoS 级别

| 级别 | 值 | 说明 | 使用场景 |
|------|-----|------|----------|
| AT_MOST_ONCE | 0 | 最多一次，不保证送达 | 心跳、状态上报 |
| AT_LEAST_ONCE | 1 | 至少一次，可能重复 | 指令下发、数据同步 |
| EXACTLY_ONCE | 2 | 精确一次，去重保证 | 关键业务操作 |

## Android 使用

直接复制源码到 Android 项目，或打包为 AAR 依赖：

```gradle
implementation 'xyz.jasenon.lab:tio-protocol:0.0.1-SNAPSHOT'
```

注意：需要在 Android 项目中提供 SLF4J 的实现（如 `slf4j-android`）。

## 目录结构

```
tio-protocol/
├── PacketHeader.java         # 协议头常量
├── ProtocolPacket.java       # 数据包对象
├── QosLevel.java             # QoS 级别枚举
├── PacketFlags.java          # 标志位定义
├── CommandType.java          # 命令类型常量
├── CheckSumCalculator.java   # 校验和计算
├── SeqIdGenerator.java       # 序列号生成器
├── PacketBuilder.java        # 数据包构建器
├── codec/
│   └── PacketCodec.java      # 编解码器
└── qos/
    └── QosManager.java       # QoS 管理器
```
