package xyz.jasenon.lab.cache.service;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import xyz.jasenon.lab.cache.exception.CacheException;
import xyz.jasenon.lab.cache.properties.CacheProperties;
import xyz.jasenon.lab.cache.spi.Cache;
import xyz.jasenon.lab.cache.template.JedisTemplate;
import xyz.jasenon.lab.cache.util.SerializationUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 基于 Redis 的缓存服务实现
 * 使用 Supplier 模式实现按需加载
 * 支持 Redis 所有数据结构：String、Hash、List、Set、Sorted Set
 *
 * @author Jasenon
 */
@Slf4j
public class RedisCache implements Cache {

    private final JedisTemplate jedisTemplate;
    private final RedissonClient redissonClient;
    private final SerializationUtil serializationUtil;
    private final CacheProperties properties;

    // 空值标记
    private static final String NULL_VALUE = "@@NULL@@";

    public RedisCache(JedisTemplate jedisTemplate,
                      RedissonClient redissonClient,
                      SerializationUtil serializationUtil,
                      CacheProperties properties) {
        this.jedisTemplate = jedisTemplate;
        this.redissonClient = redissonClient;
        this.serializationUtil = serializationUtil;
        this.properties = properties;
    }

    @Override
    public void clearAll() {
        jedisTemplate.flushDB();
        log.info("缓存已清空");
    }

    // ==================== String 操作 ====================

    @Override
    public <T> T get(String key, Supplier<T> supplier, Class<T> clazz) {
        return get(key, supplier, clazz,
                properties.getDefaultExpiration().getSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public <T> T get(String key, Supplier<T> supplier, Class<T> clazz, long timeout, TimeUnit unit) {
        // 先从 Redis 获取
        String value = jedisTemplate.get(key);
        if (value != null) {
            if (NULL_VALUE.equals(value)) {
                return null;
            }
            return deserialize(value, clazz);
        }

        // 获取分布式锁，防止缓存击穿
        String lockKey = key + ":lock";
        return withLock(lockKey, 10, TimeUnit.SECONDS, () -> {
            // 双重检查
            String cached = jedisTemplate.get(key);
            if (cached != null) {
                return NULL_VALUE.equals(cached) ? null : deserialize(cached, clazz);
            }

            // 加载数据
            T result = supplier.get();

            // 写入缓存
            if (result == null) {
                if (properties.isCacheNullValues()) {
                    jedisTemplate.setex(key, properties.getNullValueExpiration(), NULL_VALUE);
                }
            } else {
                String serialized = serialize(result);
                jedisTemplate.setex(key, (int) unit.toSeconds(timeout), serialized);
            }

            return result;
        });
    }

    @Override
    public <T> T get(String key, Supplier<T> supplier, TypeReference<T> typeRef) {
        return get(key, supplier, typeRef, properties.getDefaultExpiration().getSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public <T> T get(String key, Supplier<T> supplier, TypeReference<T> typeRef, long timeout, TimeUnit unit) {
        String value = jedisTemplate.get(key);
        if (value != null) {
            if (NULL_VALUE.equals(value)) {
                return null;
            }
            return deserialize(value, typeRef);
        }

        String lockKey = key + ":lock";
        return withLock(lockKey, 10, TimeUnit.SECONDS, () -> {
            String cached = jedisTemplate.get(key);
            if (cached != null) {
                return NULL_VALUE.equals(cached) ? null : deserialize(cached, typeRef);
            }

            T result = supplier.get();
            if (result == null) {
                if (properties.isCacheNullValues()) {
                    jedisTemplate.setex(key, properties.getNullValueExpiration(), NULL_VALUE);
                }
            } else {
                jedisTemplate.setex(key, (int) unit.toSeconds(timeout), serialize(result));
            }
            return result;
        });
    }

    @Override
    public void set(String key, Object value) {
        set(key, value,
                properties.getDefaultExpiration().getSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        if (value == null) {
            if (properties.isCacheNullValues()) {
                jedisTemplate.setex(key, properties.getNullValueExpiration(), NULL_VALUE);
            }
        } else {
            String serialized = serialize(value);
            jedisTemplate.setex(key, (int) unit.toSeconds(timeout), serialized);
        }
    }

    @Override
    public <T> Map<String, T> mget(Collection<String> keys, Supplier<Map<String, T>> supplier, Class<T> clazz) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        // 批量获取
        List<String> values = jedisTemplate.mget(keys.toArray(new String[0]));

        Map<String, T> result = new HashMap<>();
        Set<String> missingKeys = new HashSet<>();

        Iterator<String> keyIter = keys.iterator();
        Iterator<String> valueIter = values.iterator();

        while (keyIter.hasNext() && valueIter.hasNext()) {
            String key = keyIter.next();
            String value = valueIter.next();

            if (value == null) {
                missingKeys.add(key);
            } else if (!NULL_VALUE.equals(value)) {
                result.put(key, deserialize(value, clazz));
            }
        }

        // 加载缺失的数据
        if (!missingKeys.isEmpty()) {
            Map<String, T> loaded = supplier.get();
            if (loaded != null) {
                for (String mk : missingKeys) {
                    T loadedValue = loaded.get(mk);
                    if (loadedValue != null || properties.isCacheNullValues()) {
                        set(mk, loadedValue);
                    }
                    if (loadedValue != null) {
                        result.put(mk, loadedValue);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public <T> Map<String, T> mget(Collection<String> keys, Supplier<Map<String, T>> supplier, TypeReference<T> typeRef) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> values = jedisTemplate.mget(keys.toArray(new String[0]));

        Map<String, T> result = new HashMap<>();
        Set<String> missingKeys = new HashSet<>();

        Iterator<String> keyIter = keys.iterator();
        Iterator<String> valueIter = values.iterator();

        while (keyIter.hasNext() && valueIter.hasNext()) {
            String key = keyIter.next();
            String value = valueIter.next();

            if (value == null) {
                missingKeys.add(key);
            } else if (!NULL_VALUE.equals(value)) {
                result.put(key, deserialize(value, typeRef));
            }
        }

        if (!missingKeys.isEmpty()) {
            Map<String, T> loaded = supplier.get();
            if (loaded != null) {
                for (String mk : missingKeys) {
                    T loadedValue = loaded.get(mk);
                    if (loadedValue != null || properties.isCacheNullValues()) {
                        set(mk, loadedValue);
                    }
                    if (loadedValue != null) {
                        result.put(mk, loadedValue);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void mset(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        List<String> keyValues = new ArrayList<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String value = entry.getValue() == null ? NULL_VALUE : serialize(entry.getValue());
            keyValues.add(entry.getKey());
            keyValues.add(value);
        }

        jedisTemplate.mset(keyValues.toArray(new String[0]));
    }

    @Override
    public boolean delete(String key) {
        return jedisTemplate.del(key) > 0;
    }

    @Override
    public long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        return jedisTemplate.del(new ArrayList<>(keys));
    }

    @Override
    public boolean hasKey(String key) {
        return jedisTemplate.exists(key);
    }

    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return jedisTemplate.expire(key, (int) unit.toSeconds(timeout)) == 1;
    }

    @Override
    public long getExpire(String key) {
        return jedisTemplate.ttl(key);
    }

    // ==================== Hash 操作 ====================

    @Override
    public <T> T hGet(String key, String field, Supplier<T> supplier, Class<T> clazz) {
        String value = jedisTemplate.hget(key, field);
        if (value != null) {
            return NULL_VALUE.equals(value) ? null : deserialize(value, clazz);
        }

        // 加载数据
        T result = supplier.get();

        if (result == null) {
            if (properties.isCacheNullValues()) {
                jedisTemplate.hset(key, field, NULL_VALUE);
            }
        } else {
            jedisTemplate.hset(key, field, serialize(result));
        }

        return result;
    }

    @Override
    public <T> T hGet(String key, String field, Supplier<T> supplier, TypeReference<T> typeRef) {
        String value = jedisTemplate.hget(key, field);
        if (value != null) {
            return NULL_VALUE.equals(value) ? null : deserialize(value, typeRef);
        }

        T result = supplier.get();

        if (result == null) {
            if (properties.isCacheNullValues()) {
                jedisTemplate.hset(key, field, NULL_VALUE);
            }
        } else {
            jedisTemplate.hset(key, field, serialize(result));
        }

        return result;
    }

    @Override
    public void hSet(String key, String field, Object value) {
        if (value == null) {
            if (properties.isCacheNullValues()) {
                jedisTemplate.hset(key, field, NULL_VALUE);
            }
        } else {
            jedisTemplate.hset(key, field, serialize(value));
        }
    }

    @Override
    public void hSetAll(String key, Map<String, Object> map) {
        Map<String, String> serialized = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            serialized.put(entry.getKey(),
                    entry.getValue() == null ? NULL_VALUE : serialize(entry.getValue()));
        }
        jedisTemplate.hset(key, serialized);
    }

    @Override
    public <T> Map<String, T> hGetAll(String key, Supplier<Map<String, T>> supplier, Class<T> clazz) {
        Map<String, String> entries = jedisTemplate.hgetAll(key);
        if (entries != null && !entries.isEmpty()) {
            Map<String, T> result = new HashMap<>();
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                if (!NULL_VALUE.equals(entry.getValue())) {
                    result.put(entry.getKey(), deserialize(entry.getValue(), clazz));
                }
            }
            return result;
        }

        // 加载数据
        Map<String, T> result = supplier.get();
        if (result != null && !result.isEmpty()) {
            hSetAll(key, new HashMap<>(result));
        }

        return result != null ? result : Collections.emptyMap();
    }

    @Override
    public <T> Map<String, T> hGetAll(String key, Supplier<Map<String, T>> supplier, TypeReference<T> typeRef) {
        Map<String, String> entries = jedisTemplate.hgetAll(key);
        if (entries != null && !entries.isEmpty()) {
            Map<String, T> result = new HashMap<>();
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                if (!NULL_VALUE.equals(entry.getValue())) {
                    result.put(entry.getKey(), deserialize(entry.getValue(), typeRef));
                }
            }
            return result;
        }

        Map<String, T> result = supplier.get();
        if (result != null && !result.isEmpty()) {
            hSetAll(key, new HashMap<>(result));
        }

        return result != null ? result : Collections.emptyMap();
    }

    @Override
    public <T> List<T> hMultiGet(String key, Collection<String> fields, Class<T> clazz) {
        List<String> values = jedisTemplate.hmget(key, fields.toArray(new String[0]));

        List<T> result = new ArrayList<>();
        for (String value : values) {
            if (value != null && !NULL_VALUE.equals(value)) {
                result.add(deserialize(value, clazz));
            } else {
                result.add(null);
            }
        }
        return result;
    }

    @Override
    public <T> List<T> hMultiGet(String key, Collection<String> fields, TypeReference<T> typeRef) {
        List<String> values = jedisTemplate.hmget(key, fields.toArray(new String[0]));

        List<T> result = new ArrayList<>();
        for (String value : values) {
            if (value != null && !NULL_VALUE.equals(value)) {
                result.add(deserialize(value, typeRef));
            } else {
                result.add(null);
            }
        }
        return result;
    }

    @Override
    public long hDelete(String key, Object... fields) {
        String[] fieldStrs = Arrays.stream(fields)
                .map(Object::toString)
                .toArray(String[]::new);
        return jedisTemplate.hdel(key, fieldStrs);
    }

    @Override
    public boolean hHasKey(String key, String field) {
        return jedisTemplate.hexists(key, field);
    }

    @Override
    public Set<String> hKeys(String key) {
        return jedisTemplate.hkeys(key);
    }

    @Override
    public <T> List<T> hValues(String key, Class<T> clazz) {
        List<String> values = jedisTemplate.hvals(key);
        return values.stream()
                .filter(v -> !NULL_VALUE.equals(v))
                .map(v -> deserialize(v, clazz))
                .collect(Collectors.toList());
    }

    @Override
    public <T> List<T> hValues(String key, TypeReference<T> typeRef) {
        List<String> values = jedisTemplate.hvals(key);
        return values.stream()
                .filter(v -> !NULL_VALUE.equals(v))
                .map(v -> deserialize(v, typeRef))
                .collect(Collectors.toList());
    }

    @Override
    public long hSize(String key) {
        return jedisTemplate.hlen(key);
    }

    // ==================== List 操作 ====================

    @Override
    public long lLeftPush(String key, Object value) {
        return jedisTemplate.lpush(key, serialize(value));
    }

    @Override
    public long lLeftPushAll(String key, Collection<?> values) {
        String[] serialized = values.stream()
                .map(this::serialize)
                .toArray(String[]::new);
        return jedisTemplate.lpush(key, serialized);
    }

    @Override
    public long lRightPush(String key, Object value) {
        return jedisTemplate.rpush(key, serialize(value));
    }

    @Override
    public long lRightPushAll(String key, Collection<?> values) {
        String[] serialized = values.stream()
                .map(this::serialize)
                .toArray(String[]::new);
        return jedisTemplate.rpush(key, serialized);
    }

    @Override
    public <T> T lLeftPop(String key, Class<T> clazz) {
        String value = jedisTemplate.lpop(key);
        return value == null ? null : deserialize(value, clazz);
    }

    @Override
    public <T> T lLeftPop(String key, TypeReference<T> typeRef) {
        String value = jedisTemplate.lpop(key);
        return value == null ? null : deserialize(value, typeRef);
    }

    @Override
    public <T> T lRightPop(String key, Class<T> clazz) {
        String value = jedisTemplate.rpop(key);
        return value == null ? null : deserialize(value, clazz);
    }

    @Override
    public <T> T lRightPop(String key, TypeReference<T> typeRef) {
        String value = jedisTemplate.rpop(key);
        return value == null ? null : deserialize(value, typeRef);
    }

    @Override
    public <T> List<T> lRange(String key, long start, long end, Class<T> clazz) {
        List<String> values = jedisTemplate.lrange(key, start, end);
        return values.stream()
                .map(v -> deserialize(v, clazz))
                .collect(Collectors.toList());
    }

    @Override
    public <T> List<T> lRange(String key, long start, long end, TypeReference<T> typeRef) {
        List<String> values = jedisTemplate.lrange(key, start, end);
        return values.stream()
                .map(v -> deserialize(v, typeRef))
                .collect(Collectors.toList());
    }

    @Override
    public <T> List<T> lGetAll(String key, Supplier<List<T>> supplier, Class<T> clazz) {
        long size = jedisTemplate.llen(key);
        if (size > 0) {
            return lRange(key, 0, -1, clazz);
        }

        List<T> result = supplier.get();
        if (result != null && !result.isEmpty()) {
            lRightPushAll(key, result);
        }

        return result != null ? result : Collections.emptyList();
    }

    @Override
    public <T> List<T> lGetAll(String key, Supplier<List<T>> supplier, TypeReference<T> typeRef) {
        long size = jedisTemplate.llen(key);
        if (size > 0) {
            return lRange(key, 0, -1, typeRef);
        }

        List<T> result = supplier.get();
        if (result != null && !result.isEmpty()) {
            lRightPushAll(key, result);
        }

        return result != null ? result : Collections.emptyList();
    }

    @Override
    public void lSet(String key, long index, Object value) {
        jedisTemplate.lset(key, index, serialize(value));
    }

    @Override
    public <T> T lIndex(String key, long index, Class<T> clazz) {
        String value = jedisTemplate.lindex(key, index);
        return value == null ? null : deserialize(value, clazz);
    }

    @Override
    public <T> T lIndex(String key, long index, TypeReference<T> typeRef) {
        String value = jedisTemplate.lindex(key, index);
        return value == null ? null : deserialize(value, typeRef);
    }

    @Override
    public long lRemove(String key, long count, Object value) {
        return jedisTemplate.lrem(key, count, serialize(value));
    }

    @Override
    public long lSize(String key) {
        return jedisTemplate.llen(key);
    }

    @Override
    public void lTrim(String key, long start, long end) {
        jedisTemplate.ltrim(key, start, end);
    }

    // ==================== Set 操作 ====================

    @Override
    public long sAdd(String key, Object member) {
        return jedisTemplate.sadd(key, serialize(member));
    }

    @Override
    public long sAddAll(String key, Collection<?> members) {
        String[] serialized = members.stream()
                .map(this::serialize)
                .toArray(String[]::new);
        return jedisTemplate.sadd(key, serialized);
    }

    @Override
    public long sRemove(String key, Object member) {
        return jedisTemplate.srem(key, serialize(member));
    }

    @Override
    public long sRemoveAll(String key, Collection<?> members) {
        String[] serialized = members.stream()
                .map(this::serialize)
                .toArray(String[]::new);
        return jedisTemplate.srem(key, serialized);
    }

    @Override
    public boolean sIsMember(String key, Object member) {
        return jedisTemplate.sismember(key, serialize(member));
    }

    @Override
    public <T> Set<T> sMembers(String key, Supplier<Set<T>> supplier, Class<T> clazz) {
        Set<String> members = jedisTemplate.smembers(key);
        if (members != null && !members.isEmpty()) {
            return members.stream()
                    .map(m -> deserialize(m, clazz))
                    .collect(Collectors.toSet());
        }

        Set<T> result = supplier.get();
        if (result != null && !result.isEmpty()) {
            sAddAll(key, result);
        }

        return result != null ? result : Collections.emptySet();
    }

    @Override
    public <T> Set<T> sMembers(String key, Supplier<Set<T>> supplier, TypeReference<T> typeRef) {
        Set<String> members = jedisTemplate.smembers(key);
        if (members != null && !members.isEmpty()) {
            return members.stream()
                    .map(m -> deserialize(m, typeRef))
                    .collect(Collectors.toSet());
        }

        Set<T> result = supplier.get();
        if (result != null && !result.isEmpty()) {
            sAddAll(key, result);
        }

        return result != null ? result : Collections.emptySet();
    }

    @Override
    public long sSize(String key) {
        return jedisTemplate.scard(key);
    }

    @Override
    public <T> T sRandomMember(String key, Class<T> clazz) {
        String member = jedisTemplate.srandmember(key);
        return member == null ? null : deserialize(member, clazz);
    }

    @Override
    public <T> T sRandomMember(String key, TypeReference<T> typeRef) {
        String member = jedisTemplate.srandmember(key);
        return member == null ? null : deserialize(member, typeRef);
    }

    @Override
    public <T> Set<T> sDistinctRandomMembers(String key, long count, Class<T> clazz) {
        Set<String> members = jedisTemplate.srandmember(key, (int) count);
        return members.stream()
                .map(m -> deserialize(m, clazz))
                .collect(Collectors.toSet());
    }

    @Override
    public <T> Set<T> sDistinctRandomMembers(String key, long count, TypeReference<T> typeRef) {
        Set<String> members = jedisTemplate.srandmember(key, (int) count);
        return members.stream()
                .map(m -> deserialize(m, typeRef))
                .collect(Collectors.toSet());
    }

    @Override
    public <T> T sPop(String key, Class<T> clazz) {
        String member = jedisTemplate.spop(key);
        return member == null ? null : deserialize(member, clazz);
    }

    @Override
    public <T> T sPop(String key, TypeReference<T> typeRef) {
        String member = jedisTemplate.spop(key);
        return member == null ? null : deserialize(member, typeRef);
    }

    @Override
    public <T> Set<T> sIntersect(String key, String otherKey, Class<T> clazz) {
        Set<String> members = jedisTemplate.sinter(key, otherKey);
        return members.stream()
                .map(m -> deserialize(m, clazz))
                .collect(Collectors.toSet());
    }

    @Override
    public <T> Set<T> sIntersect(String key, String otherKey, TypeReference<T> typeRef) {
        Set<String> members = jedisTemplate.sinter(key, otherKey);
        return members.stream()
                .map(m -> deserialize(m, typeRef))
                .collect(Collectors.toSet());
    }

    @Override
    public <T> Set<T> sUnion(String key, String otherKey, Class<T> clazz) {
        Set<String> members = jedisTemplate.sunion(key, otherKey);
        return members.stream()
                .map(m -> deserialize(m, clazz))
                .collect(Collectors.toSet());
    }

    @Override
    public <T> Set<T> sUnion(String key, String otherKey, TypeReference<T> typeRef) {
        Set<String> members = jedisTemplate.sunion(key, otherKey);
        return members.stream()
                .map(m -> deserialize(m, typeRef))
                .collect(Collectors.toSet());
    }

    @Override
    public <T> Set<T> sDifference(String key, String otherKey, Class<T> clazz) {
        Set<String> members = jedisTemplate.sdiff(key, otherKey);
        return members.stream()
                .map(m -> deserialize(m, clazz))
                .collect(Collectors.toSet());
    }

    @Override
    public <T> Set<T> sDifference(String key, String otherKey, TypeReference<T> typeRef) {
        Set<String> members = jedisTemplate.sdiff(key, otherKey);
        return members.stream()
                .map(m -> deserialize(m, typeRef))
                .collect(Collectors.toSet());
    }

    // ==================== Sorted Set (ZSet) 操作 ====================

    @Override
    public boolean zAdd(String key, Object value, double score) {
        return jedisTemplate.zadd(key, score, serialize(value)) > 0;
    }

    @Override
    public long zAddAll(String key, Map<Object, Double> tuples) {
        Map<String, Double> serialized = new HashMap<>();
        for (Map.Entry<Object, Double> entry : tuples.entrySet()) {
            serialized.put(serialize(entry.getKey()), entry.getValue());
        }
        return jedisTemplate.zadd(key, serialized);
    }

    @Override
    public long zRemove(String key, Object member) {
        return jedisTemplate.zrem(key, serialize(member));
    }

    @Override
    public long zRemoveAll(String key, Collection<?> members) {
        String[] serialized = members.stream()
                .map(this::serialize)
                .toArray(String[]::new);
        return jedisTemplate.zrem(key, serialized);
    }

    @Override
    public long zRemoveRange(String key, long start, long end) {
        return jedisTemplate.zremrangeByRank(key, start, end);
    }

    @Override
    public long zRemoveRangeByScore(String key, double min, double max) {
        return jedisTemplate.zremrangeByScore(key, min, max);
    }

    @Override
    public double zIncrementScore(String key, Object value, double delta) {
        return jedisTemplate.zincrby(key, delta, serialize(value));
    }

    @Override
    public Long zRank(String key, Object member) {
        return jedisTemplate.zrank(key, serialize(member));
    }

    @Override
    public Long zReverseRank(String key, Object member) {
        return jedisTemplate.zrevrank(key, serialize(member));
    }

    @Override
    public <T> Set<T> zRange(String key, long start, long end, Supplier<Set<T>> supplier, Class<T> clazz) {
        List<String> members = jedisTemplate.zrange(key, start, end);
        if (members != null && !members.isEmpty()) {
            return members.stream()
                    .map(m -> deserialize(m, clazz))
                    .collect(Collectors.toSet());
        }

        Set<T> result = supplier.get();
        if (result != null && !result.isEmpty()) {
            int i = 0;
            for (T item : result) {
                zAdd(key, item, i++);
            }
        }

        return result != null ? result : Collections.emptySet();
    }

    @Override
    public <T> Set<T> zRange(String key, long start, long end, Supplier<Set<T>> supplier, TypeReference<T> typeRef) {
        List<String> members = jedisTemplate.zrange(key, start, end);
        if (members != null && !members.isEmpty()) {
            return members.stream()
                    .map(m -> deserialize(m, typeRef))
                    .collect(Collectors.toSet());
        }

        Set<T> result = supplier.get();
        if (result != null && !result.isEmpty()) {
            int i = 0;
            for (T item : result) {
                zAdd(key, item, i++);
            }
        }

        return result != null ? result : Collections.emptySet();
    }

    @Override
    public <T> Map<T, Double> zRangeWithScores(String key, long start, long end, Supplier<Map<T, Double>> supplier, Class<T> clazz) {
        Map<String, Double> entries = jedisTemplate.zrangeWithScores(key, start, end);
        if (entries != null && !entries.isEmpty()) {
            Map<T, Double> result = new LinkedHashMap<>();
            for (Map.Entry<String, Double> entry : entries.entrySet()) {
                result.put(deserialize(entry.getKey(), clazz), entry.getValue());
            }
            return result;
        }

        Map<T, Double> result = supplier.get();
        if (result != null && !result.isEmpty()) {
            zAddAll(key, new HashMap<>(result));
        }

        return result != null ? result : Collections.emptyMap();
    }

    @Override
    public <T> Map<T, Double> zRangeWithScores(String key, long start, long end, Supplier<Map<T, Double>> supplier, TypeReference<T> typeRef) {
        Map<String, Double> entries = jedisTemplate.zrangeWithScores(key, start, end);
        if (entries != null && !entries.isEmpty()) {
            Map<T, Double> result = new LinkedHashMap<>();
            for (Map.Entry<String, Double> entry : entries.entrySet()) {
                result.put(deserialize(entry.getKey(), typeRef), entry.getValue());
            }
            return result;
        }

        Map<T, Double> result = supplier.get();
        if (result != null && !result.isEmpty()) {
            zAddAll(key, new HashMap<>(result));
        }

        return result != null ? result : Collections.emptyMap();
    }

    @Override
    public <T> Set<T> zRangeByScore(String key, double min, double max, Supplier<Set<T>> supplier, Class<T> clazz) {
        List<String> members = jedisTemplate.zrangeByScore(key, min, max);
        if (members != null && !members.isEmpty()) {
            return members.stream()
                    .map(m -> deserialize(m, clazz))
                    .collect(Collectors.toSet());
        }

        Set<T> result = supplier.get();
        if (result != null && !result.isEmpty()) {
            int i = 0;
            for (T item : result) {
                zAdd(key, item, i++);
            }
        }

        return result != null ? result : Collections.emptySet();
    }

    @Override
    public <T> Set<T> zRangeByScore(String key, double min, double max, Supplier<Set<T>> supplier, TypeReference<T> typeRef) {
        List<String> members = jedisTemplate.zrangeByScore(key, min, max);
        if (members != null && !members.isEmpty()) {
            return members.stream()
                    .map(m -> deserialize(m, typeRef))
                    .collect(Collectors.toSet());
        }

        Set<T> result = supplier.get();
        if (result != null && !result.isEmpty()) {
            int i = 0;
            for (T item : result) {
                zAdd(key, item, i++);
            }
        }

        return result != null ? result : Collections.emptySet();
    }

    @Override
    public <T> Map<T, Double> zRangeByScoreWithScores(String key, double min, double max, Supplier<Map<T, Double>> supplier, Class<T> clazz) {
        Map<String, Double> entries = jedisTemplate.zrangeByScoreWithScores(key, min, max);
        if (entries != null && !entries.isEmpty()) {
            Map<T, Double> result = new LinkedHashMap<>();
            for (Map.Entry<String, Double> entry : entries.entrySet()) {
                result.put(deserialize(entry.getKey(), clazz), entry.getValue());
            }
            return result;
        }

        Map<T, Double> result = supplier.get();
        if (result != null && !result.isEmpty()) {
            zAddAll(key, new HashMap<>(result));
        }

        return result != null ? result : Collections.emptyMap();
    }

    @Override
    public <T> Map<T, Double> zRangeByScoreWithScores(String key, double min, double max, Supplier<Map<T, Double>> supplier, TypeReference<T> typeRef) {
        Map<String, Double> entries = jedisTemplate.zrangeByScoreWithScores(key, min, max);
        if (entries != null && !entries.isEmpty()) {
            Map<T, Double> result = new LinkedHashMap<>();
            for (Map.Entry<String, Double> entry : entries.entrySet()) {
                result.put(deserialize(entry.getKey(), typeRef), entry.getValue());
            }
            return result;
        }

        Map<T, Double> result = supplier.get();
        if (result != null && !result.isEmpty()) {
            zAddAll(key, new HashMap<>(result));
        }

        return result != null ? result : Collections.emptyMap();
    }

    @Override
    public <T> Set<T> zReverseRange(String key, long start, long end, Supplier<Set<T>> supplier, Class<T> clazz) {
        List<String> members = jedisTemplate.zrevrange(key, start, end);
        if (members != null && !members.isEmpty()) {
            return members.stream()
                    .map(m -> deserialize(m, clazz))
                    .collect(Collectors.toSet());
        }

        Set<T> result = supplier.get();
        if (result != null && !result.isEmpty()) {
            int i = result.size();
            for (T item : result) {
                zAdd(key, item, i--);
            }
        }

        return result != null ? result : Collections.emptySet();
    }

    @Override
    public <T> Set<T> zReverseRange(String key, long start, long end, Supplier<Set<T>> supplier, TypeReference<T> typeRef) {
        List<String> members = jedisTemplate.zrevrange(key, start, end);
        if (members != null && !members.isEmpty()) {
            return members.stream()
                    .map(m -> deserialize(m, typeRef))
                    .collect(Collectors.toSet());
        }

        Set<T> result = supplier.get();
        if (result != null && !result.isEmpty()) {
            int i = result.size();
            for (T item : result) {
                zAdd(key, item, i--);
            }
        }

        return result != null ? result : Collections.emptySet();
    }

    @Override
    public Double zScore(String key, Object member) {
        return jedisTemplate.zscore(key, serialize(member));
    }

    @Override
    public long zSize(String key) {
        return jedisTemplate.zcard(key);
    }

    @Override
    public long zCount(String key, double min, double max) {
        return jedisTemplate.zcount(key, min, max);
    }

    // ==================== Keys 操作 ====================

    @Override
    public Set<String> keys(String pattern) {
        return jedisTemplate.keys(pattern);
    }

    @Override
    public Set<String> scan(String pattern, long count) {
        return jedisTemplate.scan(pattern, count);
    }

    @Override
    public long deleteByPattern(String pattern) {
        return jedisTemplate.delByPattern(pattern);
    }

    @Override
    public long dbSize() {
        return jedisTemplate.dbSize();
    }

    @Override
    public void flushDb() {
        jedisTemplate.flushDB();
    }

    @Override
    public void flushAll() {
        jedisTemplate.flushAll();
    }

    // ==================== 分布式锁 ====================

    @Override
    public boolean tryLock(String lockKey, long timeout, TimeUnit unit) {
        String fullLockKey = "lock:" + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);
        try {
            return lock.tryLock(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean unlock(String lockKey) {
        String fullLockKey = "lock:" + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            return true;
        }
        return false;
    }

    @Override
    public <T> T withLock(String lockKey, long timeout, TimeUnit unit, Supplier<T> supplier) {
        String fullLockKey = "lock:" + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);
        try {
            if (lock.tryLock(timeout, unit)) {
                try {
                    return supplier.get();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
            throw new CacheException("获取锁失败: " + lockKey);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CacheException("获取锁被中断: " + lockKey, e);
        }
    }

    // ==================== 序列化工具方法（核心修复） ====================

    private String serialize(Object value) {
        return serializationUtil.serializeToString(value);
    }

    /**
     * 简单类型反序列化（非泛型类）
     */
    private <T> T deserialize(String value, Class<T> clazz) {
        if (value == null || NULL_VALUE.equals(value)) {
            return null;
        }
        return serializationUtil.deserialize(value, clazz);
    }

    /**
     * 复杂泛型反序列化（如 List<User>, Map<String, Device>）
     */
    private <T> T deserialize(String value, TypeReference<T> typeRef) {
        if (value == null || NULL_VALUE.equals(value)) {
            return null;
        }
        return serializationUtil.deserialize(value, typeRef);
    }
}