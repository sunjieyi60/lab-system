//package xyz.jasenon.lab.class_time_table.cache;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.TypeReference;
//import lombok.extern.slf4j.Slf4j;
//import org.redisson.api.RLock;
//import org.redisson.api.RedissonClient;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.concurrent.TimeUnit;
//import java.util.function.Supplier;
//
///**
// * 简易缓存工具（防并发击穿）
// *
// * 核心功能：
// * 1. 缓存读取
// * 2. 分布式锁防并发重建
// * 3. 随机过期时间防雪崩
// *
// * @author Jasenon_ce
// * @date 2026/3/17
// */
//@Slf4j
//@Component
//public class Cache {
//
//    @Autowired
//    private StringRedisTemplate redisTemplate;
//
//    @Autowired
//    private RedissonClient redissonClient;
//
//    /**
//     * 缓存空值的过期时间（分钟）- 防止缓存穿透
//     */
//    private static final long NULL_CACHE_MINUTES = 5;
//
//    /**
//     * 获取缓存数据（带防击穿保护）
//     *
//     * @param key      缓存 key
//     * @param dbLoader 数据库查询逻辑（Supplier 包装）
//     * @param minutes  缓存过期时间（分钟）
//     * @return 数据（可能为 null）
//     *
//     * 使用示例：
//     * <pre>
//     * MusicianVO musician = simpleCache.get(
//     *     "musician:" + id,                          // key
//     *     () -> musicianMapper.selectById(id),       // 数据库查询逻辑
//     *     30                                         // 缓存 30 分钟
//     * );
//     * </pre>
//     */
//    public <T> T get(String key, Supplier<T> dbLoader, long minutes) {
//        // 1. 查缓存
//        String json = redisTemplate.opsForValue().get(key);
//        if (json != null) {
//            // 如果是空值占位符，直接返回 null
//            if ("null".equals(json)) {
//                return null;
//            }
//            return JSON.parseObject(json, new TypeReference<T>() {});
//        }
//
//        // 2. 缓存未命中，需要加锁重建
//        String lockKey = "lock:cache:" + key;
//        RLock lock = redissonClient.getLock(lockKey);
//
//        try {
//            // 尝试获取锁（最多等 3 秒，锁持有 10 秒）
//            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
//
//            if (!locked) {
//                // 没拿到锁，说明其他线程在重建，等 200ms 再查一次
//                log.warn("缓存重建锁竞争: {}", key);
//                Thread.sleep(200);
//
//                json = redisTemplate.opsForValue().get(key);
//                if (json != null && !"null".equals(json)) {
//                    return JSON.parseObject(json, new TypeReference<T>() {});
//                }
//                // 还是没命中，返回 null（降级）
//                return null;
//            }
//
//            // 3. 拿到锁，双重检查（可能其他线程已重建）
//            json = redisTemplate.opsForValue().get(key);
//            if (json != null) {
//                return "null".equals(json) ? null : JSON.parseObject(json, new TypeReference<T>() {});
//            }
//
//            // 4. 执行数据库查询（这里才真正调用 dbLoader.get()）
//            log.info("缓存未命中，查询数据库: {}", key);
//            T data = dbLoader.get();
//
//            // 5. 写入缓存（空值也缓存，防止穿透）
//            if (data != null) {
//                // 随机过期时间：基础时间 + 0-5 分钟随机（防雪崩）
//                long ttlSeconds = minutes * 60 + (long) (Math.random() * 300);
//                redisTemplate.opsForValue().set(key, JSON.toJSONString(data), ttlSeconds, TimeUnit.SECONDS);
//                log.info("缓存已写入: {}, TTL: {}秒", key, ttlSeconds);
//            } else {
//                // 空值缓存 5 分钟（防止恶意 ID 反复查库）
//                redisTemplate.opsForValue().set(key, "null", NULL_CACHE_MINUTES, TimeUnit.MINUTES);
//                log.info("数据库无数据，缓存空值: {}", key);
//            }
//
//            return data;
//
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            log.error("获取缓存锁被中断: {}", key, e);
//            return null;
//        } catch (Exception e) {
//            log.error("缓存加载异常: {}", key, e);
//            // 出错时直接查数据库（兜底）
//            return dbLoader.get();
//        } finally {
//            // 6. 释放锁
//            if (lock.isHeldByCurrentThread()) {
//                lock.unlock();
//            }
//        }
//    }
//
//    /**
//     * 删除缓存
//     */
//    public void delete(String key) {
//        redisTemplate.delete(key);
//        log.info("缓存已删除: {}", key);
//    }
//
//    /**
//     * 批量删除（支持通配符）
//     */
//    public void deletePattern(String pattern) {
//        var keys = redisTemplate.keys(pattern);
//        if (keys != null && !keys.isEmpty()) {
//            redisTemplate.delete(keys);
//            log.info("批量删除缓存: {}，数量: {}", pattern, keys.size());
//        }
//    }
//
//    /**
//     * 主动设置缓存
//     */
//    public <T> void set(String key, T data, long minutes) {
//        if (data == null) {
//            redisTemplate.opsForValue().set(key, "null", NULL_CACHE_MINUTES, TimeUnit.MINUTES);
//        } else {
//            long ttlSeconds = minutes * 60 + (long) (Math.random() * 300);
//            redisTemplate.opsForValue().set(key, JSON.toJSONString(data), ttlSeconds, TimeUnit.SECONDS);
//        }
//    }
//}
