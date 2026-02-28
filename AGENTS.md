# Lab System - 实验室综合管理系统

## 项目概述

这是一个实验室综合管理系统（Lab Management System），用于管理高校或研究机构的实验室、设备、课程安排和门禁控制。系统采用多模块架构，包含后端服务、MQTT通信服务、智慧班牌服务和Android客户端。

### 核心功能

- **实验室管理**：楼宇、院系、实验室的基础数据管理
- **设备管理**：支持多种设备类型（空调、照明、门禁、传感器、断路器）
- **课程排课**：学期、课程、教师、课表安排
- **定时任务**：基于Quartz的条件-动作调度系统，支持SpEL表达式条件判断
- **设备控制**：通过MQTT协议与RS485/Socket网关通信
- **智慧班牌**：TCP长连接班牌系统，支持人脸识别门禁

---

## 技术栈

### 后端技术

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.5.5 | 基础框架 |
| Maven | 3.8+ | 构建工具 |
| MySQL | 8.x | 关系型数据库 |
| Redis | 6.x+ | 缓存与会话 |
| MyBatis Plus | 3.5.12 | ORM框架 |
| MyBatis Plus Join | 1.5.4 | 联表查询 |
| Quartz | - | 定时任务调度 |
| t-io | 3.8.7 | TCP通信框架 |
| MQTT (Paho) | 1.2.5 | 物联网消息协议 |
| Sa-Token | 1.44.0 | 权限认证 |
| Redisson | 3.52.0 | Redis客户端 |
| Hutool | 5.8.40 | 工具类库 |
| Lombok | 1.18.30 | 代码简化 |
| Jackson | 2.20.0 | JSON处理 |
| Swagger | 3.0.0 | API文档 |

### Android客户端技术

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.0.21 | 开发语言 |
| Android Gradle Plugin | 9.0.0 | 构建工具 |
| Jetpack Compose | 2024.09.00 | UI框架 |
| compileSdk | 36 | 编译SDK |
| minSdk/targetSdk | 30 | Android版本 |
| t-io | 3.8.7 | TCP通信 |
| FaceAISDK | 2026.01.15 | 人脸识别 |
| OkHttp | 4.12.0 | HTTP通信 |
| Gson | 2.10.1 | JSON解析 |

---

## 项目结构

```
lab-system/
├── pom.xml                          # 父POM，统一管理依赖版本
├── sql/
│   └── db.sql                       # 数据库初始化脚本（含表结构和初始数据）
│
├── common/                          # 公共模块
│   ├── entity/                      # 实体类（MyBatis Plus注解）
│   │   ├── base/                    # 基础数据实体（楼宇、院系、用户、实验室等）
│   │   ├── device/                  # 设备相关实体（空调、照明、门禁等）
│   │   ├── class_time_table/        # 课表相关实体（学期、课程、教师、排课）
│   │   └── record/                  # 设备状态记录实体
│   ├── command/                     # 命令定义（设备控制指令）
│   ├── dto/task/                    # 任务DTO
│   └── utils/                       # 工具类（R统一响应、CRC校验等）
│
├── service/                         # 主业务服务模块（端口8088）
│   ├── controller/                  # REST API控制器
│   ├── service/                     # 业务逻辑层
│   │   └── impl/                    # 服务实现
│   ├── mapper/                      # MyBatis数据访问层
│   ├── dto/                         # 数据传输对象（Create/Edit/Delete前缀）
│   ├── vo/                          # 视图对象（Vo后缀）
│   ├── quartz/                      # 定时任务配置与实现
│   │   ├── job/                     # Quartz Job实现
│   │   ├── model/                   # 任务模型（ScheduleTask/Action/Condition等）
│   │   ├── service/                 # 任务运行时服务
│   │   └── check/                   # 条件检查器
│   ├── strategy/                    # 策略模式实现
│   │   ├── device/                  # 设备轮询策略（工厂+队列模式）
│   │   └── task/                    # 任务发送策略（MQTT/Socket）
│   ├── aspect/                      # AOP切面（日志、权限）
│   ├── annotation/                  # 自定义注解
│   └── exception/                   # 全局异常处理
│
├── mqtt/                            # MQTT通信服务模块（端口8089）
│   ├── mqtt/client/                 # MQTT客户端实现
│   │   └── handler/                 # 消息处理器
│   ├── service/                     # 设备服务实现
│   └── controller/TaskController.java # 任务接收接口
│
├── class_time_table/                # 智慧班牌服务模块（端口8083/9000）
│   └── t_io/                        # t-io TCP服务器相关
│       ├── adapter/                 # 协议适配器
│       ├── codec/                   # 编解码器
│       ├── config/                  # 服务器配置
│       ├── handler/                 # 消息处理器
│       └── listener/                # 连接监听器
│
├── tio-protocol/                    # 纯Java TCP协议层（可被Android复用）
│   ├── ProtocolPacket.java          # 协议数据包
│   ├── PacketBuilder.java           # 数据包构建器
│   ├── PacketHeader.java            # 协议头定义
│   ├── PacketFlags.java             # 标志位定义
│   ├── CommandType.java             # 基础命令类型（0x00-0xFF）
│   ├── QosLevel.java                # QoS级别枚举
│   ├── codec/PacketCodec.java       # 编解码器
│   ├── rpc/                         # RPC实现
│   │   ├── SimpleRpcClient.java     # 简化RPC客户端
│   │   ├── SimpleRpcServer.java     # 简化RPC服务端
│   │   ├── RpcRequest.java          # RPC请求
│   │   ├── RpcResponse.java         # RPC响应
│   │   └── RpcDispatcher.java       # 请求分发器
│   └── qos/                         # QoS管理器（三级QoS实现）
│
├── tio-client/                      # t-io测试客户端
│
├── j-im/                            # 即时通讯模块（可选）
│   ├── jim-core/                    # 核心协议
│   ├── jim-server/                  # 服务器
│   └── jim-client/                  # 客户端
│
└── class_time_table_android/        # 智慧班牌Android客户端
    ├── app/                         # 主应用模块
    │   ├── MainActivity.kt          # 主界面
    │   ├── network/                 # 网络通信（t-io客户端）
    │   │   ├── TioClientManager.kt  # t-io连接管理
    │   │   └── handler/             # 消息处理器
    │   ├── ui/                      # Jetpack Compose UI组件
    │   ├── config/                  # 配置管理
    │   └── dto/                     # 数据传输对象
    └── faceAILib/                   # 人脸识别库模块
```

---

## 模块依赖关系

```
                    ┌─────────────┐
                    │   parent    │
                    │   (pom.xml) │
                    └──────┬──────┘
                           │
       ┌───────────┬───────┴───────┬───────────┐
       │           │               │           │
       ▼           ▼               ▼           ▼
  ┌────────┐  ┌────────┐    ┌──────────┐  ┌──────────┐
  │ common │  │tio-protocol│ │tio-client│  │service   │
  └───┬────┘  └────┬───┘    └──────────┘  └────┬─────┘
      │            │                            │
      │            └────────────────────────────┘
      │                         │
      ▼                         ▼
 ┌─────────┐              ┌──────────┐      ┌─────────────┐
 │  mqtt   │              │class_time│      │class_time_  │
 │(端口8089)│              │_table    │      │table_android│
 └─────────┘              │(端口8083)│      │  (Android)  │
                          └──────────┘      └─────────────┘
```

---

## 构建与运行

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
- Android Studio (用于Android客户端)

### 数据库初始化

```bash
# 创建数据库并导入初始数据
mysql -u root -p < sql/db.sql
```

数据库默认配置（在application.yaml中）：
- 地址：`10.230.80.109:3306`
- 数据库名：`lab_sys4`
- 用户名：`root`
- 密码：`labsystem`

### 后端服务构建

```bash
# 编译整个项目（跳过测试）
mvn clean install -DskipTests

# 运行主服务（端口8088）
cd service
mvn spring-boot:run

# 运行MQTT服务（端口8089）
cd mqtt
mvn spring-boot:run

# 运行智慧班牌服务（端口8083）
cd class_time_table
mvn spring-boot:run
```

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定模块测试
cd service
mvn test

# 运行特定测试类
mvn test -Dtest=UserControllerTest
```

### Android客户端构建

```bash
cd class_time_table_android

# 编译
./gradlew build

# 安装到设备
./gradlew installDebug
```

---

## 服务端口配置

| 服务 | HTTP端口 | TCP端口 | 说明 |
|------|----------|---------|------|
| service | 8088 | - | 主业务API服务 |
| mqtt | 8089 | - | MQTT消息服务（HTTP接口） |
| class_time_table | 8083 | 9000 | HTTP管理接口 / t-io TCP长连接 |

---

## 数据库表结构

### 核心表

| 表名 | 说明 |
|------|------|
| `classTimeTable` | 用户表 |
| `dept` | 院系部门 |
| `building` | 楼宇 |
| `laboratory` | 实验室 |
| `dept_building` | 院系-楼宇关联 |
| `dept_user` | 院系-用户关联 |
| `laboratory_manager` | 实验室管理员 |
| `laboratory_user` | 实验室用户 |

### 设备表

| 表名 | 说明 |
|------|------|
| `device` | 设备主表（支持多态：空调、照明、门禁、传感器、断路器） |
| `rs485_gateway` | RS485网关 |
| `socket_gateway` | Socket网关 |

### 设备记录表

| 表名 | 说明 |
|------|------|
| `access_record` | 门禁记录 |
| `air_condition_record` | 空调状态记录 |
| `light_record` | 照明状态记录 |
| `sensor_record` | 传感器数据记录 |
| `circuit_break_record` | 断路器状态记录 |

### 课表相关

| 表名 | 说明 |
|------|------|
| `semester` | 学期 |
| `course` | 课程 |
| `teacher` | 教师 |
| `course_schedule` | 课程安排 |

### 定时任务（Quartz自定义表）

| 表名 | 说明 |
|------|------|
| `schedule_task` | 任务定义 |
| `condition_group` | 条件组 |
| `condition` | 条件定义（SpEL表达式） |
| `action_group` | 动作组 |
| `action` | 动作定义 |
| `time_rule` | 时间规则（支持学期、周次、星期、时段） |
| `alarm` | 报警配置 |
| `data` | 数据源定义 |

---

## tio-protocol 协议规范（新版 J-IM 风格 RPC）

### 设计参考

参考 [J-IM](https://gitee.com/xchao/j-im) 的 RPC 设计，完全基于 t-io 实现：
- **synSeq RPC 机制**：利用 t-io Packet.synSeq 实现请求-响应关联
- **ReqBody/RespBody**：结构化消息体，支持 JSON 序列化
- **Command + Handler**：命令路由分发，解耦协议与业务
- **ProtocolConverter**：协议转换抽象层，支持多协议扩展

### 架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                         应用层 (Business)                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ RegisterReq  │  │ FaceRecognize│  │ ScheduleReqBody      │  │
│  │ ReqBody      │  │ ReqBody      │  │                      │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘  │
│         └─────────────────┼─────────────────────┘              │
│                           ▼                                    │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │              tio-protocol 协议层                          │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │ │
│  │  │  RespBody   │  │ Command     │  │ ProtocolPacket  │  │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  │ │
│  │                           │                              │ │
│  │  ┌────────────────────────┼──────────────────────────┐  │ │
│  │  │         ProtocolConverter 协议转换器                │  │ │
│  │  │  ┌────────────┐ ┌────────────┐ ┌────────────┐    │  │ │
│  │  │  │ TCP Convert│ │ JSON Convert│ │ (extensible)│    │  │ │
│  │  │  └────────────┘ └────────────┘ └────────────┘    │  │ │
│  │  └────────────────────────┼──────────────────────────┘  │ │
│  └───────────────────────────┼──────────────────────────────┘ │
│                              ▼                                 │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │            t-io 网络层 (Network)                          │ │
│  │      ┌─────────────────────────────────────────┐         │ │
│  │      │  RpcServerHandler / RpcClientHandler    │         │ │
│  │      │       (编解码 + 命令分发)                 │         │ │
│  │      └─────────────────────────────────────────┘         │ │
│  └───────────────────────────┬──────────────────────────────┘ │
└──────────────────────────────┼─────────────────────────────────┘
                               ▼
                      ┌─────────────────┐
                      │   t-io Core     │
                      │  Packet.synSeq  │
                      └─────────────────┘
```

### 数据包结构

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                           MAGIC (0x5A5A5A5A)                  |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
| VER | CMD   |    FLAGS        |          SEQ_ID               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                             SYN_SEQ (RPC模式)                  |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                           LENGTH                              |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                                                               |
|                           BODY (JSON)                         |
|                                                               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### 命令类型

```java
public final class Command {
    // 系统级命令 (0x00-0x0F)
    public static final byte REGISTER_REQ = 0x01;      // 设备注册请求
    public static final byte REGISTER_RESP = 0x02;     // 设备注册响应
    public static final byte HEARTBEAT_REQ = 0x10;     // 心跳请求
    public static final byte HEARTBEAT_RESP = 0x11;    // 心跳响应
    
    // 人脸/生物识别 (0x20-0x2F)
    public static final byte FACE_RECOGNIZE_REQ = 0x20;
    public static final byte FACE_RECOGNIZE_RESP = 0x21;
    public static final byte FACE_REGISTER_REQ = 0x22;
    
    // 门禁控制 (0x30-0x3F)
    public static final byte DOOR_OPEN_REQ = 0x30;
    public static final byte DOOR_OPEN_RESP = 0x31;
    
    // 课表/日程 (0x40-0x4F)
    public static final byte SCHEDULE_TODAY_REQ = 0x40;
    public static final byte SCHEDULE_TODAY_RESP = 0x41;
    
    // 设备管理 (0x50-0x5F)
    public static final byte DEVICE_INFO_REQ = 0x50;
    public static final byte OTA_UPGRADE_REQ = 0x5A;
    
    // 消息通知 (0x60-0x6F)
    public static final byte NOTIFICATION_PUSH = 0x60;
    
    // 系统配置 (0x70-0x7F)
    public static final byte TIME_SYNC_REQ = 0x70;
    public static final byte CONFIG_GET_REQ = 0x72;
}
```

### 核心组件

| 组件 | 说明 | 参考 J-IM |
|------|------|----------|
| `ProtocolPacket` | 协议包，扩展 t-io Packet | `ImPacket` |
| `ReqBody/RespBody` | 请求/响应消息体 | `ReqBody/RespBody` |
| `Command` | 命令常量定义 | `Command` |
| `CommandHandler` | 命令处理器接口 | `CmdHandler` |
| `CommandManager` | 命令分发管理 | `CommandManager` |
| `ProtocolConverter` | 协议转换器 | `IProtocolConverter` |
| `RpcContext` | RPC 调用上下文 | `ImChannelContext` |
| `Processor` | 业务处理器 | `Processor` |
| `ProcessorManager` | 处理器管理器 | - |
| `ProtocolManager` | 协议管理器（单例） | `ProtocolManager` |

### 使用示例

#### 1. 注册命令处理器

```java
// 在 SmartBoardHandler 中注册
commandManager.register(new CommandHandler() {
    @Override
    public ProtocolPacket handle(ProtocolPacket packet, RpcContext context) {
        // 解析请求
        FaceRecognizeReqBody req = JsonUtils.fromBytes(
            packet.getBody(), FaceRecognizeReqBody.class);
        
        // 业务处理...
        
        // 构建响应
        FaceRecognizeRespBody resp = new FaceRecognizeRespBody();
        resp.setRecognized(true);
        resp.setUserName("张三");
        
        byte[] body = JsonUtils.toBytes(resp);
        return packet.createResponse(Command.FACE_RECOGNIZE_RESP, body);
    }
    
    @Override
    public byte getCommand() {
        return Command.FACE_RECOGNIZE_REQ;
    }
});
```

#### 2. 集成到 class_time_table

```java
@Component
public class SmartBoardHandler extends RpcServerHandler implements ServerAioHandler {
    
    private final PacketCodec codec = new PacketCodec();
    
    @Override
    public Packet decode(ByteBuffer buffer, int limit, int position, 
                        int readableLength, ChannelContext ctx) 
                        throws TioDecodeException {
        ProtocolPacket packet = codec.decode(buffer);
        return new SmartBoardPacket(packet);
    }
    
    @Override
    public void handler(Packet packet, ChannelContext ctx) throws Exception {
        SmartBoardPacket p = (SmartBoardPacket) packet;
        // 交给 RpcServerHandler 处理
        handleReceived(ctx.getId(), codec.encode(p.getProtocolPacket()));
    }
}
```

---

## 代码规范

### 包命名规范

- 基础包名：`xyz.jasenon.lab`
- 模块子包：`xyz.jasenon.lab.{module}`

### 类命名规范

| 类型 | 后缀/前缀 | 示例 |
|------|----------|------|
| 实体类 | 无 | `User`, `Device` |
| 服务接口 | `I`前缀 + `Service` | `IUserService` |
| 服务实现 | `ServiceImpl` | `UserServiceImpl` |
| 控制器 | `Controller` | `UserController` |
| Mapper | `Mapper` | `UserMapper` |
| DTO | 无（动词前缀） | `CreateUser`, `EditUser` |
| VO | `Vo` | `UserVo` |

### 统一响应格式

使用 `common.utils.R<T>` 作为统一响应包装：

```java
// 成功响应
R.success(data);
R.success(data, "操作成功");

// 失败响应
R.fail("错误消息");
R.fail(500, "错误消息");
R.fail(500, "错误消息", data);
```

### 实体类规范

- 继承 `BaseEntity`（包含id、createTime、updateTime）
- 使用Lombok `@Getter/@Setter/@Accessors(chain = true)`
- 使用MyBatis Plus注解 `@TableId/@TableField`
- 多态实体使用Jackson `@JsonTypeInfo/@JsonSubTypes`

---

## Quartz定时任务系统

### 任务配置结构

```json
{
  "task": { /* ScheduleTask - 任务主体 */ },
  "actionGroups": [ /* ActionGroup + Action - 动作组 */ ],
  "dataGroup": [ /* Data - 数据源定义 */ ],
  "conditionGroups": [ /* ConditionGroup + Condition - 条件组 */ ],
  "timeRule": { /* TimeRule - 时间规则 */ },
  "alarmGroup": [ /* Alarm - 报警配置 */ ],
  "watchDog": { /* WatchDog - 看门狗 */ }
}
```

### 条件表达式（SpEL）

支持引用设备数据：`#{data.{dataId}}.property`

```java
// 示例：空调温度判断
"#{data.1789569705678901234}.roomTemperature <= 18"
"#{data.1789569705678901234}.isOpen == false"
"#{data.1789569705678901234}.errorCode > 0"
```

---

## 测试

### 测试文件位置

```
{module}/src/test/java/xyz/jasenon/lab/{module}/
```

### 测试类示例

- `UserControllerTest` - 控制器单元测试
- `CourseScheduleServiceImplTest` - 服务层测试
- `RpcIntegrationTest` - RPC集成测试
- `QosMechanismTest` - QoS机制测试

---

## 安全注意事项

1. **数据库密码**：配置文件中的数据库密码为开发环境使用，生产环境应使用环境变量或配置中心
2. **JWT密钥**：Sa-Token的密钥配置应在生产环境重新生成
3. **MQTT凭证**：MQTT连接用户名/密码应从配置中心获取
4. **设备认证**：智慧班牌设备注册需要实现设备证书验证
5. **人脸识别数据**：人脸特征数据在本地加密存储，不得上传服务器

---

## 文档索引

| 文档 | 位置 | 说明 |
|------|------|------|
| `quartz-task-examples.md` | 项目根目录 | Quartz定时任务配置示例 |
| `tio-protocol/README.md` | tio-protocol模块 | 协议设计文档 |
| `tio-protocol/USAGE_GUIDE.md` | tio-protocol模块 | 使用指南 |
| `class_time_table/README.md` | class_time_table模块 | 智慧班牌服务说明 |
| `sql/db.sql` | sql目录 | 数据库初始化脚本 |

---

## 开发参考资料

项目根目录下的 `后端开发参考资料/` 包含以下文档：

- 门禁系统开发参考
- 电源控制系统开发参考
- 通视智能设备通讯协议
- 考勤系统开发参考
- 综合采集器开发参考
- RS-485接口设备地址分配表
- 电子班牌移动端部署文档
- 远程接入说明
- 教室系统开发参考
- 整体功能介绍PPT

---

## 作者

**Jasenon_ce**

---

*最后更新：2026-02-28*
