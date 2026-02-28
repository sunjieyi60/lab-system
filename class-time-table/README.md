# class_time_table 模块（重构版）

## 概述

智慧班牌服务模块，当前为**基础架构版本**：

- ✅ 保留 t-io TCP 长连接基础设施
- ✅ 集成 tio-protocol 的 QoS 机制
- ✅ 支持协议编解码
- ⏳ 业务逻辑待重新规划

## 架构

```
class_time_table/
├── ClassTimeTableApplication.java          # 应用入口
└── t_io/
    ├── adapter/
    │   ├── TioPacketAdapter.java          # t-io 数据包适配器
    │   └── TioQosAdapter.java             # QoS 适配器
    ├── codec/
    │   └── TioProtocolCodec.java          # 协议编解码器
    ├── config/
    │   ├── TioServerConfig.java           # t-io 服务器配置
    │   └── TioServerProperties.java       # 配置属性
    ├── handler/
    │   └── SmartBoardTioHandler.java      # 消息处理器
    └── listener/
        └── SmartBoardTioListener.java     # 连接监听器
```

## 配置

```yaml
# application.yaml
server:
  port: 8083

t-io:
  server:
    name: smart-board-server
    host: 0.0.0.0
    port: 9000
    heartbeat: 60000
```

## 运行

```bash
cd class_time_table
mvn spring-boot:run
```

## 业务规划

业务逻辑层已清空，待实现：

- 设备注册认证
- 课表数据同步
- 人脸录入
- 门禁控制
- OTA 升级

## 依赖

- `tio-protocol`: QoS 机制和协议编解码
- `t-io`: TCP 通信框架
- `spring-boot`: 基础框架
