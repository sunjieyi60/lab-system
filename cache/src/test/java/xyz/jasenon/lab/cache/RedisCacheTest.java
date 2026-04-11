package xyz.jasenon.lab.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import xyz.jasenon.lab.cache.properties.CacheProperties;
import xyz.jasenon.lab.cache.service.RedisCache;
import xyz.jasenon.lab.cache.spi.Cache;
import xyz.jasenon.lab.cache.template.JedisTemplate;

import xyz.jasenon.lab.cache.util.SerializationUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 缓存服务测试类
 * 
 * 注意：运行此测试需要本地 Redis 服务
 * 如果没有 Redis，测试将被跳过
 *
 * @author Jasenon
 */
@Disabled("需要本地 Redis 服务，端口 6379")
class RedisCacheTest {

    private Cache cache;
    private int loadCount = 0;

    @BeforeEach
    void setUp() {
        loadCount = 0;
        
        // 初始化 Redis 连接
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        
        try {
            JedisPool jedisPool = new JedisPool(poolConfig, "localhost", 6379, 2000);
            JedisTemplate jedisTemplate = new JedisTemplate(jedisPool);
            
            Config redissonConfig = new Config();
            redissonConfig.useSingleServer()
                    .setAddress("redis://localhost:6379")
                    .setDatabase(0);
            RedissonClient redissonClient = Redisson.create(redissonConfig);
            
            CacheProperties properties = new CacheProperties();
            SerializationUtil serializationUtil = new SerializationUtil(new ObjectMapper(), "jackson");
            
            cache = new RedisCache(
                    jedisTemplate,
                    redissonClient,
                    serializationUtil,
                    properties
            );
            
            // 清空测试缓存
            cache.clearAll();
        } catch (Exception e) {
            throw new RuntimeException("无法连接到 Redis 服务，请确保 Redis 在 localhost:6379 运行", e);
        }
    }

    // ==================== String 操作测试 ====================

    @Test
    void testStringCacheWithSupplier() {
        String key = "test:user:1";
        Supplier<String> supplier = () -> {
            loadCount++;
            return "User_" + loadCount;
        };

        // 第一次获取，应该调用 supplier
        String value1 = cache.get(key, supplier, String.class);
        assertEquals("User_1", value1);
        assertEquals(1, loadCount);

        // 第二次获取，应该从缓存读取
        String value2 = cache.get(key, supplier, String.class);
        assertEquals("User_1", value2);
        assertEquals(1, loadCount); // supplier 不再被调用
    }

    @Test
    void testStringCacheWithExpiration() throws InterruptedException {
        String key = "test:temp";
        Supplier<String> supplier = () -> "temp_value";

        // 设置 1 秒过期
        cache.set(key, supplier.get(), 1, TimeUnit.SECONDS);

        // 立即获取应该存在
        assertTrue(cache.hasKey(key));

        // 等待 2 秒
        Thread.sleep(2000);

        // 应该已过期
        assertFalse(cache.hasKey(key));
    }

    @Test
    void testDelete() {
        String key = "test:delete";
        cache.set(key, "value");
        assertTrue(cache.hasKey(key));

        cache.delete(key);
        assertFalse(cache.hasKey(key));
    }

    // ==================== Hash 操作测试 ====================

    @Test
    void testHashCacheWithSupplier() {
        String key = "test:user:profile";
        String field = "name";
        Supplier<String> supplier = () -> {
            loadCount++;
            return "John Doe_" + loadCount;
        };

        // 第一次获取
        String value1 = cache.hGet(key, field, supplier, String.class);
        assertEquals("John Doe_1", value1);
        assertEquals(1, loadCount);

        // 第二次获取，从缓存
        String value2 = cache.hGet(key, field, supplier, String.class);
        assertEquals("John Doe_1", value2);
        assertEquals(1, loadCount);
    }

    @Test
    void testHashOperations() {
        String key = "test:hash:ops";

        // 设置字段
        cache.hSet(key, "field1", "value1");
        cache.hSet(key, "field2", "value2");

        // 获取所有字段
        Supplier<Map<String, String>> supplier = () -> new HashMap<>();
        Map<String, String> allFields = cache.hGetAll(key, supplier, String.class);

        assertEquals(2, allFields.size());
        assertEquals("value1", allFields.get("field1"));
        assertEquals("value2", allFields.get("field2"));

        // 检查字段存在
        assertTrue(cache.hHasKey(key, "field1"));
        assertFalse(cache.hHasKey(key, "field3"));

        // 获取所有 keys
        Set<String> keys = cache.hKeys(key);
        assertTrue(keys.contains("field1"));
        assertTrue(keys.contains("field2"));
    }

    // ==================== List 操作测试 ====================

    @Test
    void testListOperations() {
        String key = "test:list:ops";

        // 推入元素
        cache.lRightPush(key, "item1");
        cache.lRightPush(key, "item2");
        cache.lLeftPush(key, "item0");

        // 获取范围
        List<String> items = cache.lRange(key, 0, -1, String.class);
        assertEquals(3, items.size());
        assertEquals("item0", (String) items.get(0));
        assertEquals("item1", (String) items.get(1));
        assertEquals("item2", (String) items.get(2));

        // 获取大小
        assertEquals(3, cache.lSize(key));

        // 弹出元素
        String leftPop = cache.lLeftPop(key, String.class);
        assertEquals("item0", leftPop);

        String rightPop = cache.lRightPop(key, String.class);
        assertEquals("item2", rightPop);

        assertEquals(1, cache.lSize(key));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testListCacheWithSupplier() {
        String key = "test:list:supplier";
        Supplier<List<Object>> supplier = () -> {
            loadCount++;
            return Arrays.asList("a", "b", "c");
        };

        // 第一次获取
        List<Object> list1 = cache.lGetAll(key, supplier, Object.class);
        assertEquals(3, list1.size());
        assertEquals(1, loadCount);

        // 第二次获取，从缓存
        List<Object> list2 = cache.lGetAll(key, supplier, Object.class);
        assertEquals(3, list2.size());
        assertEquals(1, loadCount);
    }

    // ==================== Set 操作测试 ====================

    @Test
    void testSetOperations() {
        String key = "test:set:ops";

        // 添加成员
        cache.sAdd(key, "member1");
        cache.sAdd(key, "member2");
        cache.sAdd(key, "member3");

        // 检查成员
        assertTrue(cache.sIsMember(key, "member1"));
        assertFalse(cache.sIsMember(key, "member4"));

        // 获取大小
        assertEquals(3, cache.sSize(key));

        // 移除成员
        cache.sRemove(key, "member1");
        assertFalse(cache.sIsMember(key, "member1"));
        assertEquals(2, cache.sSize(key));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSetCacheWithSupplier() {
        String key = "test:set:supplier";
        Supplier<Set<Object>> supplier = () -> {
            loadCount++;
            Set<Object> set = new HashSet<>();
            set.add("x");
            set.add("y");
            set.add("z");
            return set;
        };

        // 第一次获取
        Set<Object> set1 = cache.sMembers(key, supplier, Object.class);
        assertEquals(3, set1.size());
        assertEquals(1, loadCount);

        // 第二次获取，从缓存
        Set<Object> set2 = cache.sMembers(key, supplier, Object.class);
        assertEquals(3, set2.size());
        assertEquals(1, loadCount);
    }

    @Test
    void testSetOperationsBetweenSets() {
        String key1 = "test:set:a";
        String key2 = "test:set:b";

        cache.sAddAll(key1, Arrays.asList("1", "2", "3", "4"));
        cache.sAddAll(key2, Arrays.asList("3", "4", "5", "6"));

        // 交集
        Set<String> intersect = cache.sIntersect(key1, key2, String.class);
        assertEquals(2, intersect.size());

        // 并集
        Set<String> union = cache.sUnion(key1, key2, String.class);
        assertEquals(6, union.size());

        // 差集
        Set<String> diff = cache.sDifference(key1, key2, String.class);
        assertEquals(2, diff.size());
    }

    // ==================== Sorted Set (ZSet) 操作测试 ====================

    @Test
    void testZSetOperations() {
        String key = "test:zset:ops";

        // 添加成员
        cache.zAdd(key, "member1", 1.0);
        cache.zAdd(key, "member2", 2.0);
        cache.zAdd(key, "member3", 3.0);

        // 获取排名
        assertEquals(Long.valueOf(0), cache.zRank(key, "member1"));
        assertEquals(Long.valueOf(2), cache.zRank(key, "member3"));

        // 获取分数
        assertEquals(2.0, cache.zScore(key, "member2"));

        // 获取大小
        assertEquals(3, cache.zSize(key));

        // 按排名范围获取
        Supplier<Set<String>> supplier = HashSet::new;
        Set<String> range = cache.zRange(key, 0, 1, supplier, String.class);
        assertEquals(2, range.size());
    }

    @Test
    void testZSetCacheWithSupplier() {
        String key = "test:zset:supplier";
        Supplier<Map<String, Double>> supplier = () -> {
            loadCount++;
            Map<String, Double> map = new LinkedHashMap<>();
            map.put("item1", 10.0);
            map.put("item2", 20.0);
            map.put("item3", 30.0);
            return map;
        };

        // 第一次获取
        Map<String, Double> map1 = cache.zRangeWithScores(key, 0, -1, supplier, String.class);
        assertEquals(3, map1.size());
        assertEquals(1, loadCount);

        // 第二次获取，从缓存
        Map<String, Double> map2 = cache.zRangeWithScores(key, 0, -1, supplier, String.class);
        assertEquals(3, map2.size());
        assertEquals(1, loadCount);
    }

    // ==================== Keys 操作测试 ====================

    @Test
    void testKeysOperations() {
        // 添加测试数据
        cache.set("test:key:1", "value1");
        cache.set("test:key:2", "value2");
        cache.set("test:other:1", "value3");

        // 匹配 keys
        Set<String> keys = cache.keys("key:*");
        assertEquals(2, keys.size());

        // 扫描 keys
        Set<String> scanned = cache.scan("key:*", 100);
        assertEquals(2, scanned.size());

        // 按模式删除
        long deleted = cache.deleteByPattern("key:*");
        assertEquals(2, deleted);

        assertFalse(cache.hasKey("test:key:1"));
        assertTrue(cache.hasKey("test:other:1"));
    }

    // ==================== 分布式锁测试 ====================

    @Test
    void testDistributedLock() {
        String lockKey = "test:lock:1";

        // 获取锁
        boolean acquired = cache.tryLock(lockKey, 10, TimeUnit.SECONDS);
        assertTrue(acquired);

        // 再次获取应该失败（在锁持有期间）
        boolean acquired2 = cache.tryLock(lockKey, 1, TimeUnit.SECONDS);
        assertFalse(acquired2);

        // 释放锁
        boolean released = cache.unlock(lockKey);
        assertTrue(released);
    }

    @Test
    void testWithLock() {
        String lockKey = "test:lock:2";
        Supplier<String> supplier = () -> {
            loadCount++;
            return "locked_result_" + loadCount;
        };

        String result = cache.withLock(lockKey, 10, TimeUnit.SECONDS, supplier);
        assertEquals("locked_result_1", result);
        assertEquals(1, loadCount);
    }

    // ==================== 批量操作测试 ====================

    @Test
    void testBatchOperations() {
        // 批量设置
        Map<String, Object> map = new HashMap<>();
        map.put("batch1", "value1");
        map.put("batch2", "value2");
        map.put("batch3", "value3");

        cache.mset(map);

        // 批量获取
        Supplier<Map<String, String>> supplier = () -> {
            loadCount++;
            return new HashMap<>();
        };

        Map<String, String> result = cache.mget(
                Arrays.asList("batch1", "batch2", "batch4"),
                supplier,
                String.class
        );

        assertEquals(2, result.size());
        assertEquals("value1", result.get("batch1"));
        assertEquals("value2", result.get("batch2"));
        assertEquals(1, loadCount);
    }
}
