# 智慧班牌系统功能使用分析报告

> 基于项目需求：课表同步 + 人脸识别门禁（TCP 长连接）
> 分析模块：class-time-table-core、class-time-table-server

---

## 一、项目需求概览

根据 AGENTS.md，智慧班牌功能需求：
- **课表同步**（`COMMAND_GET_CLASS_TIME_TABLE_REQ/RESP`）
- **人脸识别门禁**（`COMMAND_BITMAP_PUSH_REQ/RESP`）
- **设备管理**（在线状态、分组管理）
- **基础通信**（握手、心跳、TCP 长连接）

---

## 二、不需要的功能（建议移除/简化）

### 2.1 IM 即时通讯消息系统

| 功能 | 文件路径 | 说明 | 建议 |
|------|----------|------|------|
| **消息类型定义** | `core/packets/MsgType.java` | 文本、图片、语音、视频、音乐、图文等消息类型 | ❌ 删除 |
| **消息基类** | `core/packets/Message.java` | IM 消息基类，含 createTime、id、cmd、extras 等字段 | ⚠️ 可简化为仅保留 cmd 字段 |
| **Message 继承** | `core/packets/Group.java`<br>`core/packets/ClassTimeTable.java` | 继承自 Message 类 | ⚠️ 移除继承，改为独立类 |

**影响评估：** 班牌系统不需要 IM 消息功能，只需要 RPC 请求/响应模式。

---

### 2.2 好友/社交系统

| 功能 | 文件路径 | 说明 | 建议 |
|------|----------|------|------|
| **好友列表存储** | `core/message/MessageHelper.java`<br>`server/helper/redis/RedisMessageHelper.java` | `getRelatedClassTimeTables()`、`getAllRelatedClassTimeTables()` 方法及 FRIENDS 缓存 | ❌ 删除 |
| **FRIENDS 缓存** | `server/helper/redis/RedisMessageHelper.java` | `FRIEND_REQUEST` 缓存注册（第286行） | ❌ 删除 |
| **好友相关 Key** | `core/message/AbstractMessageHelper.java` | `FRIENDS` 常量定义 | ❌ 删除 |

**代码示例（需要删除）：**
```java
// RedisMessageHelper.java 第154-181行
@Override
public Group getRelatedClassTimeTables(String uuid, String relatedGroupId, Integer type) {
    List<Group> relatedGroups = RedisCacheManager.getCache(USER).get(uuid + SUFFIX + FRIENDS, List.class);
    // ... 好友逻辑
}

// 第207-232行 getAllRelatedClassTimeTables 方法
```

**影响评估：** 班牌设备不需要好友关系，只需要群组（分组）管理。

---

### 2.5 MongoDB 持久化支持

| 功能 | 文件路径 | 说明 | 建议 |
|------|----------|------|------|
| **MongoMessageHelper** | `server/helper/mongo/MongoMessageHelper.java` | 整个文件已被注释掉 | ❌ 删除 |

**代码状态：** 该文件已全部注释（第1-107行），无实际使用。

---

### 2.6 复杂缓存机制（可简化）

| 功能 | 文件路径 | 说明 | 建议 |
|------|----------|------|------|
| **多级缓存** | `core/cache/caffeineredis/` | Caffeine + Redis 二级缓存 | ⚠️ 如只用 Redis，可简化 |
| **Ehcache** | `core/cache/ehcache/EhcacheConst.java` | Ehcache 常量（仅存空文件） | ❌ 删除 |
| **Caffeine 缓存** | `core/cache/caffeine/` | 本地缓存实现 | ⚠️ 可选，保留 Redis 即可 |

**文件列表：**
```
core/cache/caffeineredis/
├── CaffeineRedisCache.java
├── CaffeineRedisCacheManager.java
├── RedisAsyncRunnable.java
└── RedisL2Vo.java

core/cache/caffeine/
├── CaffeineCache.java
├── CaffeineCacheManager.java
├── CaffeineConfig.java
├── CaffeineConfiguration.java
├── CaffeineConfigurationFactory.java
├── CaffeineUtils.java
└── DefaultRemovalListener.java
```

**影响评估：** 班牌系统数据量不大，单级 Redis 缓存足够。

---

### 2.7 集群功能（如单节点部署）

| 功能 | 文件路径 | 说明 | 建议 |
|------|----------|------|------|
| **集群接口** | `core/cluster/` | ICluster、ImCluster 等 | ⚠️ 如单节点，可移除 |
| **Redis 集群** | `server/cluster/redis/` | RedisCluster、RedisClusterConfig | ⚠️ 同上 |

---

### 2.9 多余状态码

| 状态码 | 定义位置 | 说明 | 建议 |
|--------|----------|------|------|
| C10003 | `core/ImStatus.java` | 获取用户信息成功 | ⚠️ 保留或重命名 |
| C10004 | `core/ImStatus.java` | 获取设备信息失败 | ✅ 保留 |
| C10005 | `core/ImStatus.java` | 获取在线设备成功 | ✅ 保留 |
| C10006 | `core/ImStatus.java` | 获取离线设备成功 | ✅ 保留 |
| C10007 | `core/ImStatus.java` | 登录成功 | ⚠️ 如不用登录则删除 |
| C10008 | `core/ImStatus.java` | 登录失败 | ⚠️ 如不用登录则删除 |
| C10009 | `core/ImStatus.java` | 鉴权成功 | ⚠️ 如不用鉴权则删除 |
| C10010 | `core/ImStatus.java` | 鉴权失败 | ⚠️ 如不用鉴权则删除 |
| C10011 | `core/ImStatus.java` | 加入群组成功 | ✅ 保留 |
| C10012 | `core/ImStatus.java` | 加入群组失败 | ✅ 保留 |
| C10015 | `core/ImStatus.java` | 获取用户消息失败 | ❌ 删除 |
| C10016 | `core/ImStatus.java` | 获取离线消息成功 | ❌ 删除 |
| C10017 | `core/ImStatus.java` | 未知的cmd命令 | ⚠️ 保留 |
| C10018 | `core/ImStatus.java` | 获取历史消息成功 | ❌ 删除 |
| C10022 | `core/ImStatus.java` | 好友申请已发送 | ❌ 删除 |
| C10023 | `core/ImStatus.java` | 好友申请已处理 | ❌ 删除 |

---

## 三、需要保留的核心功能

### 3.1 必需保留

| 功能 | 文件/包路径 | 说明 |
|------|-------------|------|
| **TCP 协议** | `core/tcp/` | TCP 通信核心 |
| **核心数据包** | `core/packets/RespBody.java`<br>`core/packets/BitmapBody.java`<br>`core/packets/BitmapRespBody.java`<br>`core/packets/ClassTimeTableReqBody.java`<br>`core/packets/ClassTimeTableRespBody.java`<br>`core/packets/HandshakeBody.java`<br>`core/packets/HeartbeatBody.java` | RPC 请求响应体 |
| **命令枚举** | `core/packets/Command.java` | 命令定义 |
| **核心状态码** | `core/ImStatus.java` | C10000, C10001, C10026, C10027 等 |
| **设备实体** | `core/packets/ClassTimeTable.java` | 班牌设备 |
| **群组实体** | `core/packets/Group.java` | 分组管理 |
| **Redis 缓存** | `core/cache/redis/` | 在线状态、群组存储 |
| **消息助手** | `core/message/MessageHelper.java`<br>`server/helper/redis/RedisMessageHelper.java` | 设备管理接口 |
| **服务器 API** | `server/JimServerAPI.java` | 发送消息接口 |
| **协议管理** | `server/protocol/ProtocolManager.java` | 协议转换 |
| **处理器** | `server/command/handler/ClassTimeTableReqHandler.java`<br>`server/command/handler/HandshakeReqHandler.java`<br>`server/command/handler/HeartbeatReqHandler.java` | 请求处理器 |

### 3.2 可选保留（根据部署需求）

| 功能 | 说明 | 决策建议 |
|------|------|----------|
| **WebSocket** | 支持 ws/wss 连接 | 班牌设备如支持 WebSocket 则保留 |
| **HTTP** | 嵌入式 HTTP 服务 | 如需 Web 管理界面则保留 |
| **集群** | 多节点部署 | 如需高可用则保留 |
---

## 四、重构建议

### 4.1 短期（最小改动）

1. **删除已注释代码**
   - `server/helper/mongo/MongoMessageHelper.java`

3. **清理空文件**
   - `core/cache/ehcache/EhcacheConst.java`

### 4.2 中期（功能简化）

1. **移除好友相关代码**
   - `MessageHelper` 中的 `getRelatedClassTimeTables()`、`getAllRelatedClassTimeTables()`
   - `RedisMessageHelper` 中的 FRIENDS 缓存操作
   - `ImStatus` 中的 C10022、C10023

2. **简化 Message 类**
   - 移除 `createTime`、`id`、`extras` 字段
   - 保留 `cmd` 字段用于命令路由

3. **移除 IM 消息相关**
   - `core/packets/Message.java`（或大幅简化）
   - `core/packets/MsgType.java`

### 4.3 长期（架构优化）

1. **协议分离**
   - 如果只使用 TCP，移除 WebSocket 和 HTTP 支持
   - 减少依赖包（t-io 的 WS/HTTP 模块）

2. **缓存简化**
   - 移除 Caffeine 本地缓存
   - 统一使用 Redis 缓存

3. **代码重构**
   - 将 J-IM 框架改造为纯 RPC 框架
   - 移除 IM 相关命名（User → Device，Message → Request/Response）

---

## 五、依赖影响分析

### 5.1 可移除的 Maven 依赖

```xml
<!-- 如果移除 WebSocket/HTTP -->
<dependency>
    <groupId>org.t-io</groupId>
    <artifactId>tio-websocket-server</artifactId>
</dependency>

<!-- 如果移除 MongoDB -->
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver</artifactId>
</dependency>

<!-- 如果移除 Caffeine -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

### 5.2 需要保留的核心依赖

```xml
<!-- TCP 通信 -->
<dependency>
    <groupId>org.t-io</groupId>
    <artifactId>tio-core</artifactId>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>

<!-- JSON -->
<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2</artifactId>
</dependency>
```

---

## 六、总结

### 6.1 当前状态
- ✅ 核心功能（课表同步、人脸下发）已实现
- ⚠️ 代码中残留大量 IM 即时通讯功能
- ⚠️ 存在已注释但未删除的代码（MongoDB）
- ⚠️ 工具类重复（与 Hutool 功能重叠）

### 6.2 建议优先级

| 优先级 | 事项 | 预计减少代码量 |
|--------|------|----------------|
| P0 | 删除注释掉的 MongoDB 代码 | 107 行 |
| P0 | 移除无用工具类（ChatKit、BASE64Util、Md5） | 3 个文件 |
| P1 | 移除好友相关代码 | ~100 行 |
| P1 | 简化 Message 类 | ~50 行 |
| P2 | 移除 WebSocket/HTTP（如只用 TCP） | ~30 个文件 |
| P2 | 移除 Caffeine 缓存 | ~8 个文件 |

### 6.3 风险提示
- ⚠️ 移除 WebSocket/HTTP 需确认班牌设备连接方式
- ⚠️ 修改 Message 类可能影响序列化（需测试兼容性）
- ⚠️ 移除好友功能前确认无业务依赖

---

*报告生成时间：2026-03-18*
*基于代码版本：class-time-table-core & class-time-table-server*
