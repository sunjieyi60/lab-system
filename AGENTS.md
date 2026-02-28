# Lab System - 实验室综合管理系统

## 项目概述

这是一个实验室综合管理系统（Lab Management System），用于管理高校或研究机构的实验室、设备、课程安排和门禁控制。系统采用多模块架构，包含后端服务、MQTT通信服务、智慧班牌服务和Android客户端。

### 核心功能

- **实验室管理**：楼宇、院系、实验室的基础数据管理
- **设备管理**：支持多种设备类型（空调、照明、门禁、传感器、断路器）
- **课程排课**：学期、课程、教师、课表安排
- **定时任务**：基于Quartz的条件-动作调度系统
- **设备控制**：通过MQTT协议与RS485/Socket网关通信
- **智慧班牌**：TCP长连接班牌系统，支持人脸识别门禁

---

## 技术栈

### 后端技术

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.5.5 | 基础框架 |
| Maven | - | 构建工具 |
| MySQL | 8.x | 关系型数据库 |
| Redis | - | 缓存与会话 |
| MyBatis Plus | 3.5.12 | ORM框架 |
| Quartz | - | 定时任务调度 |
| t-io | 3.8.7 | TCP通信框架 |
| MQTT (Paho) | 1.2.5 | 物联网消息协议 |
| Sa-Token | 1.44.0 | 权限认证 |
| Redisson | 3.52.0 | Redis客户端 |
| Hutool | 5.8.40 | 工具类库 |
| Lombok | 1.18.30 | 代码简化 |

### Android客户端技术

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.0.21 | 开发语言 |
| Android Gradle Plugin | 9.0.0 | 构建工具 |
| Jetpack Compose | 2024.09.00 | UI框架 |
| t-io | 3.8.7 | TCP通信 |
| FaceAISDK | 2026.01.15 | 人脸识别 |
| minSdk/targetSdk | 30 | Android版本 |

---

## 项目结构

```
lab-system/
├── pom.xml                          # 父POM，统一管理依赖版本
├── sql/
│   └── db.sql                       # 数据库初始化脚本（含表结构和初始数据）
│
├── common/                          # 公共模块
│   ├── entity/                      # 实体类（JPA/MyBatis注解）
│   │   ├── base/                    # 基础数据实体（楼宇、院系、用户等）
│   │   ├── device/                  # 设备相关实体
│   │   ├── class_time_table/        # 课表相关实体
│   │   └── record/                  # 设备状态记录实体
│   ├── command/                     # 命令定义（设备控制指令）
│   ├── dto/task/                    # 任务DTO
│   └── utils/                       # 工具类（R统一响应、CRC校验等）
│
├── service/                         # 主业务服务模块（端口8088）
│   ├── controller/                  # REST API控制器
│   ├── service/                     # 业务逻辑层
│   ├── mapper/                      # MyBatis数据访问层
│   ├── dto/                         # 数据传输对象
│   ├── vo/                          # 视图对象
│   ├── quartz/                      # 定时任务配置与实现
│   ├── strategy/                    # 策略模式实现
│   │   ├── device/                  # 设备轮询策略
│   │   └── task/                    # 任务发送策略（MQTT/Socket）
│   └── aspect/                      # AOP切面（日志、权限）
│
├── mqtt/                            # MQTT通信服务模块（端口8089）
│   ├── mqtt/client/                 # MQTT客户端实现
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
│   ├── codec/PacketCodec.java       # 编解码器
│   ├── qos/QosManager.java          # QoS管理器（三级QoS实现）
│   └── ...
│
├── tio-client/                      # t-io测试客户端
│
└── class_time_table_android/        # 智慧班牌Android客户端
    ├── app/                         # 主应用模块
    │   ├── MainActivity.kt          # 主界面
    │   ├── network/                 # 网络通信（t-io客户端）
    │   ├── ui/                      # Jetpack Compose UI
    │   └── config/                  # 配置管理
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
 ┌─────────┐              ┌──────────┐
 │  mqtt   │              │class_time│
 │(端口8089)│              │_table    │
 └─────────┘              │(端口8083)│
                          └──────────┘
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
# 执行SQL脚本创建数据库和初始数据
mysql -u root -p < sql/db.sql
```

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

| 服务 | 端口 | 说明 |
|------|------|------|
| service | 8088 | 主业务API服务 |
| mqtt | 8089 | MQTT消息服务（HTTP接口） |
| class_time_table | 8083 | HTTP管理接口 |
| class_time_table | 9000 | t-io TCP长连接端口 |

---

## 数据库表结构

### 核心表

| 表名 | 说明 |
|------|------|
| `user` | 用户表 |
| `dept` | 院系部门 |
| `building` | 楼宇 |
| `laboratory` | 实验室 |
| `device` | 设备主表 |
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

### 定时任务（Quartz）

| 表名 | 说明 |
|------|------|
| `schedule_task` | 任务定义 |
| `condition_group` | 条件组 |
| `condition` | 条件定义 |
| `action_group` | 动作组 |
| `action` | 动作定义 |
| `time_rule` | 时间规则 |

---

## tio-protocol 协议规范

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

### QoS级别

| 级别 | 值 | 说明 | 使用场景 |
|------|-----|------|----------|
| AT_MOST_ONCE | 0 | 最多一次 | 心跳包 |
| AT_LEAST_ONCE | 1 | 至少一次 | 指令下发、数据同步 |
| EXACTLY_ONCE | 2 | 精确一次 | 关键业务操作 |

### 命令类型

```java
public class CommandType {
    public static final byte QOS_ACK = 0x00;        // QoS确认
    public static final byte REGISTER = 0x01;       // 设备注册
    public static final byte REGISTER_ACK = 0x02;   // 注册响应
    public static final byte HEARTBEAT = 0x10;      // 心跳
    public static final byte HEARTBEAT_ACK = 0x11;  // 心跳响应
    // 0x20-0xFF 业务扩展
}
```

---

## 代码规范

### 包命名规范

- 基础包名：`xyz.jasenon.lab`
- 模块子包：`xyz.jasenon.lab.{module}`

### 类命名规范

| 类型 | 后缀 | 示例 |
|------|------|------|
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
```

---

## 测试

### 测试文件位置

```
{module}/src/test/java/xyz/jasenon/lab/{module}/
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

---

## 安全注意事项

1. **数据库密码**：配置文件中的数据库密码为开发环境使用，生产环境应使用环境变量或配置中心
2. **JWT密钥**：Sa-Token的密钥配置应在生产环境重新生成
3. **MQTT凭证**：MQTT连接用户名/密码应从配置中心获取
4. **设备认证**：智慧班牌设备注册需要实现设备证书验证
5. **人脸识别数据**：人脸特征数据在本地加密存储，不得上传服务器

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

*最后更新：2026-02-25*
