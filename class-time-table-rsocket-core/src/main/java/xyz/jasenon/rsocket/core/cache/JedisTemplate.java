package xyz.jasenon.rsocket.core.cache;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.*;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.GeoRadiusResponse;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

/**
 * Jedis 模板类 - 完整版
 * 
 * 包含所有 Redis 数据结构的读写操作：
 * - String
 * - Hash
 * - List
 * - Set
 * - Sorted Set
 * - Bitmap
 * - HyperLogLog
 * - Geo
 * - Stream
 * 
 * 参考 J-IM 的 JedisTemplate 设计
 * 
 * @author Jasenon_ce
 */
@Slf4j
@RequiredArgsConstructor
public class JedisTemplate implements Serializable {

    private static final long serialVersionUID = 9135301078135982677L;

    private final JedisPool jedisPool;

    /**
     * 执行器抽象类
     * 封装获取 Jedis、执行操作、释放资源的流程
     */
    public abstract class Executor<T> {
        protected Jedis jedis;

        public Executor() {
            this.jedis = getJedis();
        }

        /**
         * 执行具体 Redis 操作
         */
        public abstract T execute();

        /**
         * 获取执行结果
         * 确保执行完成后释放 Jedis 连接
         */
        public T getResult() {
            T result = null;
            try {
                result = execute();
            } catch (Exception e) {
                log.error("Redis execute exception", e);
                throw new RuntimeException("Redis execute exception", e);
            } finally {
                close(jedis);
            }
            return result;
        }
    }

    /**
     * 获取 Jedis 连接
     */
    public Jedis getJedis() {
        Jedis jedis = null;
        int retryCount = 0;
        int maxRetries = 3;
        
        while (jedis == null && retryCount < maxRetries) {
            try {
                jedis = jedisPool.getResource();
            } catch (Exception e) {
                log.error("获取 Jedis 连接失败，重试次数: {}", retryCount, e);
                retryCount++;
            }
        }
        
        if (jedis == null) {
            throw new RuntimeException("无法获取 Jedis 连接");
        }
        
        return jedis;
    }

    /**
     * 关闭 Jedis 连接
     */
    public void close(Jedis jedis) {
        if (jedis != null && jedis.isConnected()) {
            jedis.close();
        }
    }

    // ==================== String 操作 ====================

    /**
     * 设置字符串值
     */
    public String set(final String key, final String value) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.set(key, value);
            }
        }.getResult();
    }

    /**
     * 设置字符串值（带参数）
     */
    public String set(final String key, final String value, final SetParams params) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.set(key, value, params);
            }
        }.getResult();
    }

    /**
     * 设置字符串值（带过期时间）
     */
    public String setex(final String key, final int seconds, final String value) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.setex(key, seconds, value);
            }
        }.getResult();
    }

    /**
     * 设置字符串值（如果 key 不存在）
     */
    public Long setnx(final String key, final String value) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.setnx(key, value);
            }
        }.getResult();
    }

    /**
     * 获取字符串值
     */
    public String get(final String key) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.get(key);
            }
        }.getResult();
    }

    /**
     * 批量获取
     */
    public List<String> mget(final String... keys) {
        return new Executor<List<String>>() {
            @Override
            public List<String> execute() {
                return jedis.mget(keys);
            }
        }.getResult();
    }

    /**
     * 批量设置
     */
    public String mset(final String... keyvalues) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.mset(keyvalues);
            }
        }.getResult();
    }

    /**
     * 获取子字符串
     */
    public String getrange(final String key, final long startOffset, final long endOffset) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.getrange(key, startOffset, endOffset);
            }
        }.getResult();
    }

    /**
     * 追加字符串
     */
    public Long append(final String key, final String value) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.append(key, value);
            }
        }.getResult();
    }

    /**
     * 获取字符串长度
     */
    public Long strlen(final String key) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.strlen(key);
            }
        }.getResult();
    }

    /**
     * 设置新值并返回旧值
     */
    public String getSet(final String key, final String value) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.getSet(key, value);
            }
        }.getResult();
    }

    /**
     * 设置指定偏移量的值
     */
    public Long setrange(final String key, final long offset, final String value) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.setrange(key, offset, value);
            }
        }.getResult();
    }

    // ==================== Hash 操作 ====================

    /**
     * 设置 Hash 字段
     */
    public Long hset(final String key, final String field, final String value) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.hset(key, field, value);
            }
        }.getResult();
    }

    /**
     * 批量设置 Hash
     */
    public Long hset(final String key, final Map<String, String> hash) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.hset(key, hash);
            }
        }.getResult();
    }

    /**
     * 获取 Hash 字段
     */
    public String hget(final String key, final String field) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.hget(key, field);
            }
        }.getResult();
    }

    /**
     * 批量获取 Hash
     */
    public List<String> hmget(final String key, final String... fields) {
        return new Executor<List<String>>() {
            @Override
            public List<String> execute() {
                return jedis.hmget(key, fields);
            }
        }.getResult();
    }

    /**
     * 批量设置 Hash
     */
    public String hmset(final String key, final Map<String, String> hash) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.hmset(key, hash);
            }
        }.getResult();
    }

    /**
     * 获取所有 Hash
     */
    public Map<String, String> hgetAll(final String key) {
        return new Executor<Map<String, String>>() {
            @Override
            public Map<String, String> execute() {
                return jedis.hgetAll(key);
            }
        }.getResult();
    }

    /**
     * 获取所有 Hash 字段名
     */
    public Set<String> hkeys(final String key) {
        return new Executor<Set<String>>() {
            @Override
            public Set<String> execute() {
                return jedis.hkeys(key);
            }
        }.getResult();
    }

    /**
     * 获取所有 Hash 值
     */
    public List<String> hvals(final String key) {
        return new Executor<List<String>>() {
            @Override
            public List<String> execute() {
                return jedis.hvals(key);
            }
        }.getResult();
    }

    /**
     * 删除 Hash 字段
     */
    public Long hdel(final String key, final String... fields) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.hdel(key, fields);
            }
        }.getResult();
    }

    /**
     * 判断 Hash 字段是否存在
     */
    public Boolean hexists(final String key, final String field) {
        return new Executor<Boolean>() {
            @Override
            public Boolean execute() {
                return jedis.hexists(key, field);
            }
        }.getResult();
    }

    /**
     * 获取 Hash 字段数量
     */
    public Long hlen(final String key) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.hlen(key);
            }
        }.getResult();
    }

    /**
     * Hash 字段自增
     */
    public Long hincrBy(final String key, final String field, final long value) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.hincrBy(key, field, value);
            }
        }.getResult();
    }

    /**
     * Hash 字段自增（浮点数）
     */
    public Double hincrByFloat(final String key, final String field, final double value) {
        return new Executor<Double>() {
            @Override
            public Double execute() {
                return jedis.hincrByFloat(key, field, value);
            }
        }.getResult();
    }

    // ==================== List 操作 ====================

    /**
     * 从左侧推入列表
     */
    public Long lpush(final String key, final String... values) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.lpush(key, values);
            }
        }.getResult();
    }

    /**
     * 从右侧推入列表
     */
    public Long rpush(final String key, final String... values) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.rpush(key, values);
            }
        }.getResult();
    }

    /**
     * 从左侧弹出
     */
    public String lpop(final String key) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.lpop(key);
            }
        }.getResult();
    }

    /**
     * 从右侧弹出
     */
    public String rpop(final String key) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.rpop(key);
            }
        }.getResult();
    }

    /**
     * 从左侧阻塞弹出
     */
    public List<String> blpop(final int timeout, final String... keys) {
        return new Executor<List<String>>() {
            @Override
            public List<String> execute() {
                return jedis.blpop(timeout, keys);
            }
        }.getResult();
    }

    /**
     * 从右侧阻塞弹出
     */
    public List<String> brpop(final int timeout, final String... keys) {
        return new Executor<List<String>>() {
            @Override
            public List<String> execute() {
                return jedis.brpop(timeout, keys);
            }
        }.getResult();
    }

    /**
     * 获取列表指定范围
     */
    public List<String> lrange(final String key, final long start, final long stop) {
        return new Executor<List<String>>() {
            @Override
            public List<String> execute() {
                return jedis.lrange(key, start, stop);
            }
        }.getResult();
    }

    /**
     * 获取列表所有元素
     */
    public List<String> lrangeAll(final String key) {
        return lrange(key, 0, -1);
    }

    /**
     * 获取列表长度
     */
    public Long llen(final String key) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.llen(key);
            }
        }.getResult();
    }

    /**
     * 获取列表指定索引元素
     */
    public String lindex(final String key, final long index) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.lindex(key, index);
            }
        }.getResult();
    }

    /**
     * 设置列表指定索引值
     */
    public String lset(final String key, final long index, final String value) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.lset(key, index, value);
            }
        }.getResult();
    }

    /**
     * 删除列表指定元素
     */
    public Long lrem(final String key, final long count, final String value) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.lrem(key, count, value);
            }
        }.getResult();
    }

    /**
     * 修剪列表
     */
    public String ltrim(final String key, final long start, final long stop) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.ltrim(key, start, stop);
            }
        }.getResult();
    }

    /**
     * 从一个列表弹出并推入另一个列表
     */
    public String rpoplpush(final String sourceKey, final String destinationKey) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.rpoplpush(sourceKey, destinationKey);
            }
        }.getResult();
    }

    /**
     * 阻塞从一个列表弹出并推入另一个列表
     */
    public String brpoplpush(final String source, final String destination, final int timeout) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.brpoplpush(source, destination, timeout);
            }
        }.getResult();
    }

    // ==================== Set 操作 ====================

    /**
     * 添加 Set 成员
     */
    public Long sadd(final String key, final String... members) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.sadd(key, members);
            }
        }.getResult();
    }

    /**
     * 删除 Set 成员
     */
    public Long srem(final String key, final String... members) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.srem(key, members);
            }
        }.getResult();
    }

    /**
     * 获取 Set 所有成员
     */
    public Set<String> smembers(final String key) {
        return new Executor<Set<String>>() {
            @Override
            public Set<String> execute() {
                return jedis.smembers(key);
            }
        }.getResult();
    }

    /**
     * 判断成员是否在 Set 中
     */
    public Boolean sismember(final String key, final String member) {
        return new Executor<Boolean>() {
            @Override
            public Boolean execute() {
                return jedis.sismember(key, member);
            }
        }.getResult();
    }

    /**
     * 获取 Set 成员数量
     */
    public Long scard(final String key) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.scard(key);
            }
        }.getResult();
    }

    /**
     * 随机获取 Set 成员
     */
    public String srandmember(final String key) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.srandmember(key);
            }
        }.getResult();
    }

    /**
     * 随机获取多个 Set 成员
     */
    public List<String> srandmember(final String key, final int count) {
        return new Executor<List<String>>() {
            @Override
            public List<String> execute() {
                return jedis.srandmember(key, count);
            }
        }.getResult();
    }

    /**
     * 随机弹出 Set 成员
     */
    public String spop(final String key) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.spop(key);
            }
        }.getResult();
    }

    /**
     * 随机弹出多个 Set 成员
     */
    public Set<String> spop(final String key, final long count) {
        return new Executor<Set<String>>() {
            @Override
            public Set<String> execute() {
                return jedis.spop(key, count);
            }
        }.getResult();
    }

    /**
     * 获取两个 Set 的差集
     */
    public Set<String> sdiff(final String... keys) {
        return new Executor<Set<String>>() {
            @Override
            public Set<String> execute() {
                return jedis.sdiff(keys);
            }
        }.getResult();
    }

    /**
     * 获取两个 Set 的交集
     */
    public Set<String> sinter(final String... keys) {
        return new Executor<Set<String>>() {
            @Override
            public Set<String> execute() {
                return jedis.sinter(keys);
            }
        }.getResult();
    }

    /**
     * 获取两个 Set 的并集
     */
    public Set<String> sunion(final String... keys) {
        return new Executor<Set<String>>() {
            @Override
            public Set<String> execute() {
                return jedis.sunion(keys);
            }
        }.getResult();
    }

    // ==================== Sorted Set 操作 ====================

    /**
     * 添加 Sorted Set 成员
     */
    public Long zadd(final String key, final double score, final String member) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.zadd(key, score, member);
            }
        }.getResult();
    }

    /**
     * 批量添加 Sorted Set 成员
     */
    public Long zadd(final String key, final Map<String, Double> scoreMembers) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.zadd(key, scoreMembers);
            }
        }.getResult();
    }

    /**
     * 删除 Sorted Set 成员
     */
    public Long zrem(final String key, final String... members) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.zrem(key, members);
            }
        }.getResult();
    }

    /**
     * 按排名范围获取 Sorted Set（升序）
     */
    public List<String> zrange(final String key, final long start, final long stop) {
        return new Executor<List<String>>() {
            @Override
            public List<String> execute() {
                return jedis.zrange(key, start, stop);
            }
        }.getResult();
    }

    /**
     * 按排名范围获取 Sorted Set（降序）
     */
    public List<String> zrevrange(final String key, final long start, final long stop) {
        return new Executor<List<String>>() {
            @Override
            public List<String> execute() {
                return jedis.zrevrange(key, start, stop);
            }
        }.getResult();
    }

    /**
     * 按分数范围获取 Sorted Set（升序）
     */
    public List<String> zrangeByScore(final String key, final double min, final double max) {
        return new Executor<List<String>>() {
            @Override
            public List<String> execute() {
                return jedis.zrangeByScore(key, min, max);
            }
        }.getResult();
    }

    /**
     * 按分数范围获取 Sorted Set（降序）
     */
    public List<String> zrevrangeByScore(final String key, final double max, final double min) {
        return new Executor<List<String>>() {
            @Override
            public List<String> execute() {
                return jedis.zrevrangeByScore(key, max, min);
            }
        }.getResult();
    }

    /**
     * 获取成员分数
     */
    public Double zscore(final String key, final String member) {
        return new Executor<Double>() {
            @Override
            public Double execute() {
                return jedis.zscore(key, member);
            }
        }.getResult();
    }

    /**
     * 获取成员排名（升序）
     */
    public Long zrank(final String key, final String member) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.zrank(key, member);
            }
        }.getResult();
    }

    /**
     * 获取成员排名（降序）
     */
    public Long zrevrank(final String key, final String member) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.zrevrank(key, member);
            }
        }.getResult();
    }

    /**
     * 获取 Sorted Set 成员数量
     */
    public Long zcard(final String key) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.zcard(key);
            }
        }.getResult();
    }

    /**
     * 获取指定分数范围成员数量
     */
    public Long zcount(final String key, final double min, final double max) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.zcount(key, min, max);
            }
        }.getResult();
    }

    /**
     * 增加成员分数
     */
    public Double zincrby(final String key, final double increment, final String member) {
        return new Executor<Double>() {
            @Override
            public Double execute() {
                return jedis.zincrby(key, increment, member);
            }
        }.getResult();
    }

    /**
     * 删除指定排名范围的成员
     */
    public Long zremrangeByRank(final String key, final long start, final long stop) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.zremrangeByRank(key, start, stop);
            }
        }.getResult();
    }

    /**
     * 删除指定分数范围的成员
     */
    public Long zremrangeByScore(final String key, final double min, final double max) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.zremrangeByScore(key, min, max);
            }
        }.getResult();
    }

    // ==================== Bitmap 操作 ====================

    /**
     * 设置位图值
     */
    public Boolean setbit(final String key, final long offset, final boolean value) {
        return new Executor<Boolean>() {
            @Override
            public Boolean execute() {
                return jedis.setbit(key, offset, value);
            }
        }.getResult();
    }

    /**
     * 获取位图值
     */
    public Boolean getbit(final String key, final long offset) {
        return new Executor<Boolean>() {
            @Override
            public Boolean execute() {
                return jedis.getbit(key, offset);
            }
        }.getResult();
    }

    /**
     * 统计位图中 1 的个数
     */
    public Long bitcount(final String key) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.bitcount(key);
            }
        }.getResult();
    }

    /**
     * 统计位图指定范围中 1 的个数
     */
    public Long bitcount(final String key, final long start, final long end) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.bitcount(key, start, end);
            }
        }.getResult();
    }

    // ==================== HyperLogLog 操作 ====================

    /**
     * 添加 HyperLogLog
     */
    public Long pfadd(final String key, final String... elements) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.pfadd(key, elements);
            }
        }.getResult();
    }

    /**
     * 获取 HyperLogLog 基数估计值
     */
    public Long pfcount(final String key) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.pfcount(key);
            }
        }.getResult();
    }

    /**
     * 合并 HyperLogLog
     */
    public String pfmerge(final String destkey, final String... sourcekeys) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.pfmerge(destkey, sourcekeys);
            }
        }.getResult();
    }

    // ==================== Geo 操作 ====================

    /**
     * 添加地理位置
     */
    public Long geoadd(final String key, final double longitude, final double latitude, final String member) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.geoadd(key, longitude, latitude, member);
            }
        }.getResult();
    }

    /**
     * 获取地理位置
     */
    public List<GeoCoordinate> geopos(final String key, final String... members) {
        return new Executor<List<GeoCoordinate>>() {
            @Override
            public List<GeoCoordinate> execute() {
                return jedis.geopos(key, members);
            }
        }.getResult();
    }

    /**
     * 计算两点距离
     */
    public Double geodist(final String key, final String member1, final String member2) {
        return new Executor<Double>() {
            @Override
            public Double execute() {
                return jedis.geodist(key, member1, member2);
            }
        }.getResult();
    }

    /**
     * 获取指定范围内的成员（带坐标）
     */
    public List<GeoRadiusResponse> georadius(final String key, final double longitude, final double latitude, final double radius, final GeoUnit unit) {
        return new Executor<List<GeoRadiusResponse>>() {
            @Override
            public List<GeoRadiusResponse> execute() {
                return jedis.georadius(key, longitude, latitude, radius, unit);
            }
        }.getResult();
    }

    // ==================== 通用操作 ====================

    /**
     * 删除 Key
     */
    public Long del(final String key) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.del(key);
            }
        }.getResult();
    }

    /**
     * 批量删除 Keys
     */
    public Long del(final String... keys) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.del(keys);
            }
        }.getResult();
    }

    /**
     * 检查 Key 是否存在
     */
    public Boolean exists(final String key) {
        return new Executor<Boolean>() {
            @Override
            public Boolean execute() {
                return jedis.exists(key);
            }
        }.getResult();
    }

    /**
     * 设置过期时间
     */
    public Long expire(final String key, final int seconds) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.expire(key, seconds);
            }
        }.getResult();
    }

    /**
     * 设置过期时间戳
     */
    public Long expireAt(final String key, final long unixTime) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.expireAt(key, unixTime);
            }
        }.getResult();
    }

    /**
     * 获取剩余过期时间
     */
    public Long ttl(final String key) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.ttl(key);
            }
        }.getResult();
    }

    /**
     * 移除过期时间
     */
    public Long persist(final String key) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.persist(key);
            }
        }.getResult();
    }

    /**
     * 重命名 Key
     */
    public String rename(final String oldkey, final String newkey) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.rename(oldkey, newkey);
            }
        }.getResult();
    }

    /**
     * 模糊查询 Keys
     */
    public Set<String> keys(final String pattern) {
        return new Executor<Set<String>>() {
            @Override
            public Set<String> execute() {
                return jedis.keys(pattern);
            }
        }.getResult();
    }

    /**
     * 自增
     */
    public Long incr(final String key) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.incr(key);
            }
        }.getResult();
    }

    /**
     * 自增（指定步长）
     */
    public Long incrBy(final String key, final long increment) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.incrBy(key, increment);
            }
        }.getResult();
    }

    /**
     * 自增（浮点数）
     */
    public Double incrByFloat(final String key, final double increment) {
        return new Executor<Double>() {
            @Override
            public Double execute() {
                return jedis.incrByFloat(key, increment);
            }
        }.getResult();
    }

    /**
     * 自减
     */
    public Long decr(final String key) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.decr(key);
            }
        }.getResult();
    }

    /**
     * 自减（指定步长）
     */
    public Long decrBy(final String key, final long decrement) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.decrBy(key, decrement);
            }
        }.getResult();
    }

    /**
     * 获取 Key 类型
     */
    public String type(final String key) {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.type(key);
            }
        }.getResult();
    }

    // ==================== 对象操作（JSON序列化） ====================

    /**
     * 设置对象
     */
    public String setObject(final String key, final Object value) {
        return new Executor<String>() {
            @Override
            public String execute() {
                String json = JSON.toJSONString(value);
                return jedis.set(key, json);
            }
        }.getResult();
    }

    /**
     * 设置对象（带过期时间）
     */
    public String setObject(final String key, final Object value, final int expireSeconds) {
        return new Executor<String>() {
            @Override
            public String execute() {
                String json = JSON.toJSONString(value);
                return jedis.setex(key, expireSeconds, json);
            }
        }.getResult();
    }

    /**
     * 获取对象
     */
    public <T> T getObject(final String key, final Class<T> clazz) {
        return new Executor<T>() {
            @Override
            public T execute() {
                String json = jedis.get(key);
                if (json == null) {
                    return null;
                }
                return JSON.parseObject(json, clazz);
            }
        }.getResult();
    }

    /**
     * 设置 Hash 对象
     */
    public Long hsetObject(final String key, final String field, final Object value) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                String json = JSON.toJSONString(value);
                return jedis.hset(key, field, json);
            }
        }.getResult();
    }

    /**
     * 获取 Hash 对象
     */
    public <T> T hgetObject(final String key, final String field, final Class<T> clazz) {
        return new Executor<T>() {
            @Override
            public T execute() {
                String json = jedis.hget(key, field);
                if (json == null) {
                    return null;
                }
                return JSON.parseObject(json, clazz);
            }
        }.getResult();
    }

    // ==================== Pipeline 批量操作 ====================

    /**
     * 执行 Pipeline 操作
     */
    public List<Object> pipeline(final Function<Pipeline, Void> operations) {
        return new Executor<List<Object>>() {
            @Override
            public List<Object> execute() {
                Pipeline pipeline = jedis.pipelined();
                operations.apply(pipeline);
                return pipeline.syncAndReturnAll();
            }
        }.getResult();
    }

    /**
     * 批量设置 String（Pipeline）
     */
    public List<Object> batchSetString(final Map<String, String> keyValues) {
        return new Executor<List<Object>>() {
            @Override
            public List<Object> execute() {
                Pipeline pipeline = jedis.pipelined();
                for (Map.Entry<String, String> entry : keyValues.entrySet()) {
                    pipeline.set(entry.getKey(), entry.getValue());
                }
                return pipeline.syncAndReturnAll();
            }
        }.getResult();
    }

    /**
     * 批量获取 String（Pipeline）
     */
    public List<String> batchGetString(final List<String> keys) {
        return new Executor<List<String>>() {
            @Override
            public List<String> execute() {
                Pipeline pipeline = jedis.pipelined();
                List<Response<String>> responses = new ArrayList<>();
                for (String key : keys) {
                    responses.add(pipeline.get(key));
                }
                pipeline.sync();
                
                List<String> result = new ArrayList<>();
                for (Response<String> resp : responses) {
                    result.add(resp.get());
                }
                return result;
            }
        }.getResult();
    }

    /**
     * 批量删除（Pipeline）
     */
    public Long batchDel(final List<String> keys) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                Pipeline pipeline = jedis.pipelined();
                for (String key : keys) {
                    pipeline.del(key);
                }
                List<Object> results = pipeline.syncAndReturnAll();
                return results.stream().mapToLong(r -> (Long) r).sum();
            }
        }.getResult();
    }

    // ==================== 事务操作 ====================

    /**
     * 执行事务
     */
    public List<Object> transaction(final Function<Transaction, Void> operations) {
        return new Executor<List<Object>>() {
            @Override
            public List<Object> execute() {
                Transaction transaction = jedis.multi();
                operations.apply(transaction);
                return transaction.exec();
            }
        }.getResult();
    }

    // ==================== Pub/Sub 操作 ====================

    /**
     * 发布消息
     */
    public Long publish(final String channel, final String message) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.publish(channel, message);
            }
        }.getResult();
    }

    /**
     * 订阅频道
     */
    public void subscribe(final JedisPubSub jedisPubSub, final String... channels) {
        new Executor<Void>() {
            @Override
            public Void execute() {
                jedis.subscribe(jedisPubSub, channels);
                return null;
            }
        }.getResult();
    }

    /**
     * 按模式订阅
     */
    public void psubscribe(final JedisPubSub jedisPubSub, final String... patterns) {
        new Executor<Void>() {
            @Override
            public Void execute() {
                jedis.psubscribe(jedisPubSub, patterns);
                return null;
            }
        }.getResult();
    }

    // ==================== 分布式锁（简单实现） ====================

    /**
     * 尝试获取锁
     */
    public boolean tryLock(final String lockKey, final String requestId, final int expireSeconds) {
        return new Executor<Boolean>() {
            @Override
            public Boolean execute() {
                String result = jedis.set(lockKey, requestId, SetParams.setParams().nx().ex(expireSeconds));
                return "OK".equals(result);
            }
        }.getResult();
    }

    /**
     * 释放锁（使用 Lua 脚本保证原子性）
     */
    public boolean releaseLock(final String lockKey, final String requestId) {
        return new Executor<Boolean>() {
            @Override
            public Boolean execute() {
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
                return Long.valueOf(1).equals(result);
            }
        }.getResult();
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取 JedisPool
     */
    public JedisPool getJedisPool() {
        return jedisPool;
    }

    /**
     * 模糊删除 Keys
     */
    public Long delKeysLike(final String pattern) {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                Set<String> keys = jedis.keys(pattern + "*");
                if (keys == null || keys.isEmpty()) {
                    return 0L;
                }
                return jedis.del(keys.toArray(new String[0]));
            }
        }.getResult();
    }

    /**
     * 清空当前数据库
     */
    public String flushDB() {
        return new Executor<String>() {
            @Override
            public String execute() {
                return jedis.flushDB();
            }
        }.getResult();
    }

    /**
     * 获取数据库大小
     */
    public Long dbSize() {
        return new Executor<Long>() {
            @Override
            public Long execute() {
                return jedis.dbSize();
            }
        }.getResult();
    }
}
