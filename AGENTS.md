# Lab System - 实验室综合管理系统

## 项目概述

这是一个实验室综合管理系统（Lab Management System），用于管理高校或研究机构的实验室、设备、课程排课和门禁控制。系统采用 Maven 多模块架构，包含主业务服务、MQTT 通信服务、智慧班牌服务和公共模块。

### 核心功能

- **实验室管理**：楼宇、院系、实验室的基础数据管理
- **设备管理**：支持多种设备类型（空调、照明、门禁、传感器、断路器），通过 RS485/Socket 网关通信
- **课程排课**：学期、课程、教师、课表安排管理
- **定时任务**：基于 Quartz 的条件-动作调度系统，支持 SpEL 表达式条件判断
- **设备控制**：通过 MQTT 协议与硬件网关通信
- **智慧班牌**：基于 J-IM 框架的 TCP 长连接服务，支持课表同步和人脸识别门禁

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
| MyBatis Plus | 3.5.12 | ORM 框架 |
| MyBatis Plus Join | 1.5.4 | 联表查询 |
| Quartz | - | 定时任务调度 |
| t-io | 3.5.8 | TCP 通信框架 |
| J-IM | 3.0.0 | 即时通讯框架（智慧班牌） |
| MQTT (Paho) | 1.2.5 | 物联网消息协议 |
| Sa-Token | 1.44.0 | 权限认证 |
| Redisson | 3.52.0 | Redis 客户端 |
| Hutool | 5.8.40 | 工具类库 |
| Lombok | 1.18.30 | 代码简化 |
| Jackson | 2.20.0 | JSON 处理 |
| Swagger | 3.0.0 | API 文档 |

---

## 项目结构

```
lab-system/
├── pom.xml                          # 父 POM，统一管理依赖版本
├── sql/
│   └── db.sql                       # 数据库初始化脚本（638 行，含表结构和初始数据）
│
├── common/                          # 公共模块（库模块）
│   ├── entity/                      # 实体类（MyBatis Plus 注解）
│   │   ├── base/                    # 基础数据实体（楼宇、院系、用户、实验室等）
│   │   ├── device/                  # 设备相关实体（空调、照明、门禁、传感器、断路器）
│   │   ├── class_time_table/        # 课表相关实体（学期、课程、教师、排课）
│   │   └── record/                  # 设备状态记录实体
│   ├── command/                     # 命令定义（设备控制指令枚举）
│   ├── dto/task/                    # 任务 DTO
│   └── utils/                       # 工具类（R 统一响应、CRC 校验、SpEL 表达式等）
│
├── service/                         # 主业务服务模块（端口 8088）
│   ├── controller/                  # REST API 控制器
│   ├── service/                     # 业务逻辑层（I 前缀接口 + Impl 后缀实现）
│   │   └── impl/                    # 服务实现
│   ├── mapper/                      # MyBatis 数据访问层
│   ├── dto/                         # 数据传输对象（Create/Edit/Delete 动词前缀）
│   ├── vo/                          # 视图对象（Vo 后缀）
│   ├── quartz/                      # 定时任务配置与实现
│   │   ├── job/                     # Quartz Job 实现
│   │   ├── model/                   # 任务模型（ScheduleTask/Action/Condition 等）
│   │   └── service/                 # 任务运行时服务
│   ├── aspect/                      # AOP 切面（日志、权限）
│   └── config/                      # 配置类
│
├── mqtt/                            # MQTT 通信服务模块（端口 8089）
│   ├── mqtt/client/                 # MQTT 客户端实现
│   ├── service/                     # 设备数据处理服务
│   ├── mapper/                      # 数据访问层
│   └── controller/
│       └── TaskController.java      # 任务接收接口
│
├── class-time-table/                # 智慧班牌 HTTP 服务模块（端口 8083）
│   ├── t_io/                        # t-io TCP 服务器相关
│   │   ├── adapter/                 # 协议适配器
│   │   ├── codec/                   # 编解码器
│   │   ├── config/                  # 服务器配置
│   │   ├── handler/                 # 消息处理器
│   │   └── listener/                # 连接监听器
│   └── ClassTimeTableApplication.java
│
├── class-time-table-core/           # 智慧班牌核心协议模块（库模块）
│   ├── ImConst.java                 # 常量定义
│   ├── cache/                       # 缓存实现（Caffeine、Redis）
│   ├── cluster/                     # 集群支持
│   └── listener/                    # 监听器接口
│
└── class-time-table-server/         # 智慧班牌服务器模块（库模块）
    ├── JimServer.java               # J-IM 服务器主类
    ├── JimServerAPI.java            # 服务器 API
    ├── command/                     # 命令处理器
    ├── handler/                     # 连接处理器
    ├── listener/                    # 连接监听器
    ├── processor/                   # 协议处理器
    └── protocol/                    # 协议处理
```

---

## 模块依赖关系

```
                    ┌─────────────┐
                    │   parent    │
                    │   (pom.xml) │
                    └──────┬──────┘
                           │
       ┌───────────┬───────┼───────┬───────────────────┐
       │           │       │       │                   │
       ▼           ▼       ▼       ▼                   ▼
  ┌────────┐  ┌────────┐  │  ┌──────────┐      ┌─────────────┐
  │ common │  │  mqtt  │  │  │  service │      │class-time-  │
  └───┬────┘  └────┬───┘  │  └────┬─────┘      │table-server │
      │            │      │       │            └──────┬──────┘
      │            │      │       │                   │
      │            └──────┼───────┘                   │
      │                   │                          │
      │                   ▼                          │
      │            ┌──────────┐                      │
      └───────────►│class-time│◄─────────────────────┘
                   │-table    │
                   └──────────┘
                          │
                          ▼
                   ┌─────────────┐
                   │class-time-  │
                   │table-core   │
                   └─────────────┘
```

---

## 服务端口配置

| 服务 | HTTP 端口 | TCP 端口 | 说明 |
|------|-----------|----------|------|
| service | 8088 | - | 主业务 API 服务 |
| mqtt | 8089 | - | MQTT 消息服务（HTTP 接口） |
| class-time-table | 8083 | 9000 | HTTP 管理接口 / t-io TCP 长连接 |

---

## 数据库配置

默认数据库配置（在 application.yaml 中）：
- 地址：`10.230.80.109:3306`
- 数据库名：`lab_sys4`
- 用户名：`root`
- 密码：`labsystem`

Redis 默认配置：
- 地址：`localhost:6379`
- 数据库：`0`

---

## 构建与运行

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+

### 数据库初始化

```bash
# 创建数据库并导入初始数据
mysql -u root -p < sql/db.sql
```

### 后端服务构建

```bash
# 编译整个项目（跳过测试）
mvn clean install -DskipTests

# 运行主服务（端口 8088）
cd service
mvn spring-boot:run

# 运行 MQTT 服务（端口 8089）
cd mqtt
mvn spring-boot:run

# 运行智慧班牌服务（端口 8083）
cd class-time-table
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

---

## 代码规范

### 包命名规范

- 基础包名：`xyz.jasenon.lab`
- 模块子包：`xyz.jasenon.lab.{module}`

### 类命名规范

| 类型 | 后缀/前缀 | 示例 |
|------|----------|------|
| 实体类 | 无 | `User`, `Device` |
| 服务接口 | `I` 前缀 + `Service` | `IUserService` |
| 服务实现 | `Impl` 后缀 | `UserServiceImpl` |
| 控制器 | `Controller` 后缀 | `UserController` |
| Mapper | `Mapper` 后缀 | `UserMapper` |
| DTO | 动词前缀 | `CreateUser`, `EditUser`, `DeleteUser` |
| VO | `Vo` 后缀 | `UserVo` |

### 统一响应格式

使用 `xyz.jasenon.lab.common.utils.R<T>` 作为统一响应包装：

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

- 继承 `BaseEntity`（包含 id、createTime、updateTime）
- 使用 Lombok `@Getter/@Setter/@Accessors(chain = true)`
- 使用 MyBatis Plus 注解 `@TableId/@TableField`
- 多态实体使用 Jackson `@JsonTypeInfo/@JsonSubTypes`

示例：
```java
@Getter
@Setter
@Accessors(chain = true)
public class User extends BaseEntity {
    @TableField("username")
    private String username;
    // ...
}
```

---

## 数据库表结构

### 核心基础表

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
| `rs485_gateway` | RS485 网关 |
| `socket_gateway` | Socket 网关 |

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

### 定时任务（Quartz 自定义表）

| 表名 | 说明 |
|------|------|
| `schedule_task` | 任务定义 |
| `condition_group` | 条件组 |
| `condition` | 条件定义（SpEL 表达式） |
| `action_group` | 动作组 |
| `action` | 动作定义 |
| `time_rule` | 时间规则（支持学期、周次、星期、时段） |
| `alarm` | 报警配置 |
| `data` | 数据源定义 |
| `operation_log` | 操作日志 |
| `alarm_log` | 报警日志 |

---

## Quartz 定时任务系统

### 任务配置结构

```json
{
  "task": { /* ScheduleTask - 任务主体 */ },
  "actionGroups": [ /* ActionGroup + Action - 动作组 */ ],
  "dataGroup": [ /* Data - 数据源定义 */ ],
  "conditionGroups": [ /* ConditionGroup + Condition - 条件组 */ ],
  "timeRule": { /* TimeRule - 时间规则 */ },
  "alarmGroup": [ /* Alarm - 报警配置 */ ]
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

详细示例参考 `quartz-task-examples.md`

---

## J-IM 协议规范（智慧班牌）

### 设计参考

参考 [J-IM](https://gitee.com/xchao/j-im) 的 RPC 设计，基于 t-io 实现：
- **synSeq RPC 机制**：利用 t-io Packet.synSeq 实现请求-响应关联
- **ReqBody/RespBody**：结构化消息体，支持 JSON 序列化
- **Command + Handler**：命令路由分发，解耦协议与业务

### 命令类型

| 命令 | 值 | 说明 |
|------|-----|------|
| `HANDSHAKE_REQ` | 0x01 | 握手请求 |
| `HANDSHAKE_RESP` | 0x02 | 握手响应 |
| `HEARTBEAT_REQ` | 0x10 | 心跳请求 |
| `CLASS_TIME_TABLE_REQ` | - | 课表请求 |

详细协议设计参考 `后端开发参考资料/JIM_RPC_Protocol_Design.md`

---

## 测试

### 测试框架

- JUnit 5（Jupiter）
- Mockito

### 测试文件位置

```
{module}/src/test/java/xyz/jasenon/lab/{module}/
```

### 测试类示例

- `UserControllerTest` - 控制器单元测试（Mockito 示例）
- `CourseScheduleServiceImplTest` - 服务层测试
- `UserInitIntegrationTest` - 集成测试

### 测试示例

```java
@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @InjectMocks
    private UserController userController;

    @Mock
    private IUserService userService;

    @Test
    void testCreateUser() {
        // 测试代码
    }
}
```

---

## 安全注意事项

1. **数据库密码**：配置文件中的数据库密码为开发环境使用，生产环境应使用环境变量或配置中心
2. **JWT 密钥**：Sa-Token 的密钥配置应在生产环境重新生成
3. **MQTT 凭证**：MQTT 连接用户名/密码应从配置中心获取
4. **设备认证**：智慧班牌设备注册需要实现设备证书验证

---

## 文档索引

| 文档 | 位置 | 说明 |
|------|------|------|
| `quartz-task-examples.md` | 项目根目录 | Quartz 定时任务配置示例 |
| `后端开发参考资料/JIM_RPC_Protocol_Design.md` | 后端开发参考资料 | J-IM RPC 协议设计 |
| `后端开发参考资料/定时任务设计.md` | 后端开发参考资料 | 定时任务设计文档 |
| `sql/db.sql` | sql 目录 | 数据库初始化脚本 |
| `class-time-table/README.md` | class-time-table 模块 | 班牌服务说明 |
| `class-time-table/todo.md` | class-time-table 模块 | 开发待办事项 |

---

## 开发参考资料

项目根目录下的 `后端开发参考资料/` 包含以下文档：

- 门禁系统开发参考
- 电源监控系统开发参考
- 通视智能设备通讯协议
- 考勤系统开发参考
- 综合采集器开发参考
- RS-485 接口设备地址分配表
- 电子班牌移动端部署文档
- 远程接入说明
- 教室系统开发参考
- 整体功能介绍 PPT

---

## 作者

**Jasenon_ce**

---

*最后更新：2026-03-08*
