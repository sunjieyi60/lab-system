# 实验室管理系统 (Laboratory Management System)

## 项目概述

本项目是一个**实验室管理系统**（Lab System），用于管理高校或研究机构的实验室资源，包括：
- 用户与权限管理
- 实验室、楼栋、院系基础数据管理
- 教务排课管理（课程、教师、学期、课表）
- IoT设备管理（空调、灯光、门禁、传感器、断路器等）
- 智能任务调度与自动控制
- 数据分析和能耗统计

项目采用**多模块 Maven 架构**，主要语言为 **Java 17**，基于 **Spring Boot 3.5.5** 构建。

---

## 技术栈

### 核心技术
| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 3.5.5 | 基础框架 |
| MyBatis Plus | 3.5.12 | ORM 框架 |
| MyBatis Plus Join | 1.5.4 | 联表查询扩展 |
| MySQL | 9.x | 数据库（Connector 9.4.0）|
| Redis | - | 缓存与会话（Redisson 3.52.0）|

### 中间件与工具
| 组件 | 版本 | 说明 |
|------|------|------|
| Sa-Token | 1.44.0 | 权限认证与会话管理 |
| Quartz | - | 定时任务调度 |
| Eclipse Paho | 1.2.5 | MQTT 客户端 |
| t-io | 3.8.7 | TCP 通信框架 |
| Jackson | 2.20.0 | JSON 处理 |
| FastJSON2 | 2.0.43 | 备用 JSON 处理 |
| Hutool | 5.8.40 | 工具类库 |
| Lombok | 1.18.30 | 代码简化 |
| Swagger | 3.0.0 | API 文档 |

---

## 项目结构

```
lab-system/
├── pom.xml                 # 根 POM，统一管理依赖版本
├── sql/
│   └── db.sql             # 数据库初始化脚本
├── common/                # 公共模块（实体、工具、常量）
├── service/               # 主服务模块（REST API，端口 8088）
├── mqtt/                  # MQTT 服务模块（端口 8089）
├── class_time_table/      # 智慧班牌服务（端口 8083）
├── schedule/              # 排课相关（含前端 demo）
└── tio-client/            # t-io 通信客户端
```

### 模块说明

#### 1. common 模块
- **定位**：公共依赖模块，被其他模块引用
- **内容**：
  - `entity/`：数据库实体类（BaseEntity、设备实体、教务实体等）
  - `dto/`：数据传输对象（Task、TaskPriority）
  - `command/`：设备命令相关（Command、CommandLine、CheckType、SendType）
  - `utils/`：工具类（R 统一响应、CRC 校验、表达式求值等）
  - `service/`：通用 Service 接口定义

#### 2. service 模块（核心服务）
- **端口**：8088
- **主类**：`xyz.jasenon.lab.service.ServiceApplication`
- **功能**：
  - 用户管理（创建、编辑、删除、登录）
  - 基础数据（部门、楼栋、实验室）
  - 设备管理（增删改查、控制指令下发）
  - 教务管理（课程、教师、学期、课表 CRUD）
  - 数据分析（能耗统计、空调运行数据）
  - 智能调度（Quartz 定时任务、策略配置）

**包结构**：
```
service/
├── controller/      # REST API 控制器
├── service/         # 业务逻辑层（接口 + impl）
├── mapper/          # MyBatis 数据访问层
├── dto/             # 请求参数对象（CreateXXX、EditXXX、DeleteXXX）
├── vo/              # 响应视图对象
├── entity/          # 业务实体（如 UserPermission）
├── aspect/          # AOP 切面（权限检查、日志）
├── annotation/      # 自定义注解（@RequestPermission、@LogPoint）
├── strategy/        # 策略模式（设备轮询、任务发送）
├── quartz/          # 定时任务相关
├── config/          # 配置类
└── exception/       # 全局异常处理
```

#### 3. mqtt 模块
- **端口**：8089
- **主类**：`xyz.jasenon.lab.mqtt.MqttApplication`
- **功能**：
  - MQTT 客户端连接管理（订阅/发布）
  - 设备消息处理（空调、灯光、门禁、传感器、断路器）
  - 任务队列管理（优先级阻塞队列）
  - 报警上报

#### 4. class_time_table 模块（智慧班牌）
- **端口**：8083
- **t-io 端口**：9000
- **主类**：`xyz.jasenon.lab.class_time_table.ClassTimeTableApplication`
- **状态**：已重构为**基础架构版本**，业务逻辑待重新规划
- **功能**：
  - 智能班牌设备 TCP 长连接管理（t-io）
  - 基于 tio-protocol 的 QoS 机制（支持 AT_LEAST_ONCE/EXACTLY_ONCE）
  - 协议编解码（可复用于 Android）

**重构说明（2026-02-22）**：
1. 新增 `tio-protocol` 模块：纯 Java QoS 协议实现，可被 Android 复用
2. 移除复杂的分片机制，仅保留 QoS 核心功能
3. 清除所有业务实现代码（注册认证、课表、人脸等），待重新规划
4. 保留 t-io 基础设施和协议适配层

**新模块**：
- `tio-protocol`：通用 TCP 协议层（QoS、编解码），零框架依赖
- `class_time_table/t_io/adapter`：t-io 与 tio-protocol 的适配层

---

## 构建与运行

### 环境要求
- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+

### 构建命令
```bash
# 编译整个项目
mvn clean compile

# 打包（生成可执行 JAR）
mvn clean package

# 跳过测试打包
mvn clean package -DskipTests

# 安装到本地仓库
mvn clean install
```

### 运行服务
```bash
# 运行主服务（端口 8088）
cd service
mvn spring-boot:run
# 或
java -jar target/service-0.0.1-SNAPSHOT.jar

# 运行 MQTT 服务（端口 8089）
cd mqtt
mvn spring-boot:run

# 运行智慧班牌服务（端口 8083）
cd class_time_table
mvn spring-boot:run
```

### 数据库初始化
1. 创建数据库 `lab_sys4`
2. 执行 `sql/db.sql` 初始化表结构

### 配置文件
各模块的 `application.yaml` 位于 `src/main/resources/` 下：

**service 模块关键配置**：
```yaml
server:
  port: 8088

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lab_sys4?...
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
      database: 0

task:
  send:
    mqtt-task-host: http://localhost:8089/task/add  # MQTT 服务地址
  polling:
    core-pool-size: 8       # 设备轮询线程数
    period: 5               # 轮询间隔（秒）
```

---

## 代码规范与约定

### 1. 包命名
- 基础包：`xyz.jasenon.lab.{module}`
- 示例：`xyz.jasenon.lab.service.controller`

### 2. 类命名规范
| 类型 | 后缀 | 示例 |
|------|------|------|
| 实体类 | 无 | `User`、`Device`、`Course` |
| 数据传输对象 | DTO | `CreateUser`、`EditUser` |
| 视图对象 | Vo | `UserVo`、`DeviceVo` |
| 服务接口 | I + Service | `IUserService` |
| 服务实现 | Impl | `UserServiceImpl` |
| Mapper | Mapper | `UserMapper` |
| 控制器 | Controller | `UserController` |
| 工具类 | 无/Util | `R`、`CrcChecker` |

### 3. 统一响应格式
使用 `R<T>` 类包装所有 API 响应：
```json
{
  "ok": true,
  "code": 0,
  "msg": "业务处理成功",
  "data": {}
}
```

### 4. 权限注解
使用 `@RequestPermission` 标注需要权限的接口：
```java
@RequestPermission(allowed = {Permissions.USER_ADD})
@PostMapping("/create")
public R createUser(@RequestBody CreateUser dto) {
    // ...
}
```

### 5. 日志注解
使用 `@LogPoint` 记录操作日志：
```java
@LogPoint(title = "'账号管理'", sqEl = "#createUser", clazz = CreateUser.class)
```

### 6. 参数校验
- 使用 JSR-380 注解（`@NotNull`、`@NotBlank` 等）
- 控制器参数使用 `@Validated` 注解

---

## 测试

### 测试框架
- JUnit 5（Spring Boot Starter Test）

### 运行测试
```bash
# 运行所有测试
mvn test

# 运行特定模块测试
cd service && mvn test

# 运行单个测试类
mvn test -Dtest=UserControllerTest
```

### 测试结构
```
src/test/java/xyz/jasenon/lab/{module}/
├── controller/          # 控制器测试
├── service/             # 服务层测试
└── integration/         # 集成测试
```

---

## 安全与权限

### 权限体系
采用**树形权限结构**（定义在 `Permissions` 枚举）：
```
ROOT
├── USER（用户管理）
│   ├── USER_ADD
│   ├── USER_EDIT
│   └── USER_DELETE
├── ACADEMIC_AFFAIRS_MANAGEMENT（教务管理）
│   ├── SCHEDULE_CLASSES
│   ├── SCHEDULE_CLASSES_VIEW
│   └── SEMESTER_SETTINGS
├── CONTROL_CENTER（控制中心）
│   ├── DEVICE_ADD
│   ├── DEVICE_CONTROL
│   ├── DEVICE_SMART_CONTROL
│   └── DEVICE_ALARM_SETTINGS
├── DATA_ANALYSIS（数据分析）
│   ├── ACADEMIC_AFFAIRS_ANALYSIS
│   ├── LABORATORY_POWER_CONSUMPTION
│   └── LABORATORY_CENTRAL_AIRCONDITION
└── BASE_SETTINGS（基础设置）
    ├── BASE_CUD
    └── BASE_VIEW
```

### 认证机制
- **Sa-Token**：负责登录态管理与权限校验
- **Token**：登录成功后返回，后续请求携带
- **切面检查**：`PermissionCheck` 切面拦截带 `@RequestPermission` 的方法

### 密码加密
- 使用 MD5 加密（`Md5Encrypt` 处理器）

---

## 核心业务概念

### 1. 多态设备创建
创建设备时通过 `deviceType` 字段路由到具体子类型：
- `AirCondition`：空调设备
- `Light`：灯光设备
- `Access`：门禁设备
- `Sensor`：传感器
- `CircuitBreak`：断路器

### 2. 网关类型
- **RS485 Gateway**：MQTT 通信网关，通过 Topic 收发消息
- **Socket Gateway**：TCP 长连接网关，通过 MAC 地址识别

### 3. 任务调度
- **Quartz**：定时任务调度（如定时开关设备）
- **任务优先级**：NORMAL、HIGH、URGENT
- **任务分发策略**：MQTT 策略、Socket 策略

### 4. 课表排课冲突检测
创建课表时自动检测冲突：
- 主体重叠（同实验室或同教师）
- 星期交集
- 周次交集（单双周匹配）
- 时间段或节次重叠

---

## 部署建议

### 开发环境
```bash
# 1. 启动 MySQL 和 Redis
# 2. 初始化数据库（执行 sql/db.sql）
# 3. 启动 service 模块（端口 8088）
# 4. 启动 mqtt 模块（端口 8089）
# 5. 启动 class_time_table 模块（端口 8083）
```

### 生产环境
1. 修改各模块 `application.yaml` 中的数据库、Redis、MQTT 地址
2. 使用 `mvn package` 打包生成 JAR
3. 使用 `nohup` 或系统服务（systemd）运行 JAR
4. 配置反向代理（Nginx）处理跨域和负载均衡

### 端口占用
| 服务 | 端口 | 说明 |
|------|------|------|
| service | 8088 | 主 API 服务 |
| mqtt | 8089 | MQTT 通信服务 |
| class_time_table | 8083 | 智慧班牌 HTTP |
| class_time_table | 9000 | t-io TCP 端口 |

---

## API 文档

详见 `API.md` 文件，包含完整的接口定义、请求/响应示例。

主要接口分类：
- 用户管理：`/user/*`
- 部门管理：`/dept/*`
- 楼栋管理：`/building/*`
- 实验室管理：`/laboratory/*`
- 网关管理：`/gateway/*`
- 设备管理：`/device/*`
- 教务管理：`/academic/*`
- 数据分析：`/analysis/*`
- 日志查询：`/log/*`

---

## 参考资料

- `后端开发参考资料/`：项目相关技术文档
- `API.md`：接口文档
- `sql/db.sql`：数据库脚本
