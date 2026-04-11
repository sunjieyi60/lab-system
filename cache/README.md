# Lab Cache - Redis 缓存组件

基于 `Supplier<?>` 的 Redis 一级缓存组件，使用 SPI 方式注入到 Spring 生态中。

## 特性

- ✅ **Supplier 模式**：支持按需加载数据，自动缓存结果
- ✅ **完整 Redis 数据结构**：String、Hash、List、Set、Sorted Set、Keys
- ✅ **SPI 注入**：通过 Java SPI 机制自动发现和加载
- ✅ **Spring Boot 自动配置**：零配置开箱即用
- ✅ **分布式锁**：基于 Redisson 的分布式锁支持
- ✅ **空值缓存**：防止缓存穿透

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>xyz.jasenon.lab</groupId>
    <artifactId>cache</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 启用缓存

在 Spring Boot 启动类上添加注解：

```java
@SpringBootApplication
@EnableLabCache
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 配置 Redis

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379

lab:
  cache:
    enabled: true
    default-expiration: 30m
    cache-null-values: true
```

### 4. 使用缓存

```java
@Service
public class UserService {
    
    @Autowired
    private CacheService cacheService;
    
    public User getUserById(Long id) {
        // 直接使用原始 key
        return cacheService.get("user:" + id, () -> {
            return userRepository.findById(id).orElse(null);
        });
    }
    
    public void updateUser(User user) {
        userRepository.save(user);
        // 直接删除
        cacheService.delete("user:" + user.getId());
    }
}
```

## 详细使用

### String 操作

```java
// 获取缓存，使用 Supplier 加载
cacheService.get("key", () -> "value");

// 带过期时间
cacheService.get("key", () -> "value", 10, TimeUnit.MINUTES);

// 直接设置
cacheService.set("key", "value");
cacheService.set("key", "value", 1, TimeUnit.HOURS);
```

### Hash 操作

```java
// 获取 Hash 字段
cacheService.hGet("user:1", "name", () -> "John");

// 设置 Hash 字段
cacheService.hSet("user:1", "name", "John");

// 获取所有字段
Map<String, Object> all = cacheService.hGetAll("user:1", () -> loadUserMap());
```

### List 操作

```java
// 推入元素
cacheService.lRightPush("queue", "item1");

// 获取列表（带缓存加载）
List<Object> items = cacheService.lGetAll("queue", () -> loadQueue());
```

### Set 操作

```java
// 添加成员
cacheService.sAdd("tags", "java");

// 获取成员（带缓存加载）
Set<Object> tags = cacheService.sMembers("tags", () -> loadTags());

// 集合运算
Set<Object> intersect = cacheService.sIntersect("set1", "set2");
```

### Sorted Set (ZSet) 操作

```java
// 添加成员
cacheService.zAdd("rank", "player1", 100.0);

// 按排名获取（带缓存加载）
Set<Object> top10 = cacheService.zRange("rank", 0, 9, () -> loadRank());
```

### Keys 操作

```java
// 匹配 keys
Set<String> keys = cacheService.keys("user:*");

// 按模式删除
long deleted = cacheService.deleteByPattern("temp:*");
```

### 分布式锁

```java
// 使用 withLock 简化
String result = cacheService.withLock("order:123", 10, TimeUnit.SECONDS, () -> {
    // 执行业务逻辑
    return "success";
});
```

## SPI 使用方式

如果不使用 Spring Boot，可以通过 SPI 方式加载：

```java
// 加载缓存服务
CacheService cacheService = CacheServiceLoader.load();
```

在 `META-INF/services/xyz.jasenon.lab.cache.spi.Cache` 中添加：

```
xyz.jasenon.lab.cache.service.RedisCache
```

## 配置属性

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `lab.cache.enabled` | true | 是否启用缓存 |
| `lab.cache.default-expiration` | 30m | 默认过期时间 |
| `lab.cache.cache-null-values` | true | 是否缓存空值 |
| `lab.cache.serializer` | jackson | 序列化方式 |

## 注意事项

1. **Key 直接透传**：用户传入的 key 直接作为 Redis key 使用，不做任何前缀处理
2. **分布式锁**：使用 Redisson 实现，确保 Redis 服务可用
3. **序列化**：默认使用 Jackson，确保缓存对象可序列化
