package xyz.jasenon.lab.cache.template;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;
import xyz.jasenon.lab.cache.exception.CacheException;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/**
 * Jedis 模板类
 * 封装 Jedis 操作，提供统一的 Redis 访问接口
 *
 * @author Jasenon
 */
@Slf4j
public class JedisTemplate {

    private final JedisPool jedisPool;

    public JedisTemplate(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * 执行 Redis 操作
     *
     * @param action 操作函数
     * @param <T>    返回值类型
     * @return 操作结果
     */
    public <T> T execute(Function<Jedis, T> action) {
        try (Jedis jedis = jedisPool.getResource()) {
            return action.apply(jedis);
        } catch (Exception e) {
            log.error("Redis 操作失败: {}", e.getMessage(), e);
            throw new CacheException("Redis 操作失败", e);
        }
    }

    /**
     * 执行 Pipeline 操作
     *
     * @param action Pipeline 操作函数
     * @return 操作结果
     */
    public List<Object> executePipelined(Function<Pipeline, Void> action) {
        try (Jedis jedis = jedisPool.getResource()) {
            Pipeline pipeline = jedis.pipelined();
            action.apply(pipeline);
            return pipeline.syncAndReturnAll();
        } catch (Exception e) {
            log.error("Redis Pipeline 操作失败: {}", e.getMessage(), e);
            throw new CacheException("Redis Pipeline 操作失败", e);
        }
    }

    // ==================== String 操作 ====================

    public String set(String key, String value) {
        return execute(jedis -> jedis.set(key, value));
    }

    public String set(String key, byte[] value) {
        return execute(jedis -> jedis.set(key.getBytes(StandardCharsets.UTF_8), value));
    }

    public String setex(String key, int seconds, String value) {
        return execute(jedis -> jedis.setex(key, seconds, value));
    }

    public String setex(String key, int seconds, byte[] value) {
        return execute(jedis -> jedis.setex(key.getBytes(StandardCharsets.UTF_8), seconds, value));
    }

    public String get(String key) {
        return execute(jedis -> jedis.get(key));
    }

    public byte[] getBytes(String key) {
        return execute(jedis -> jedis.get(key.getBytes(StandardCharsets.UTF_8)));
    }

    public List<String> mget(String... keys) {
        return execute(jedis -> jedis.mget(keys));
    }

    public String mset(String... keysvalues) {
        return execute(jedis -> jedis.mset(keysvalues));
    }

    public Long del(String... keys) {
        return execute(jedis -> jedis.del(keys));
    }

    public Long del(Collection<String> keys) {
        return execute(jedis -> jedis.del(keys.toArray(new String[0])));
    }

    public Boolean exists(String key) {
        return execute(jedis -> jedis.exists(key));
    }

    public Long expire(String key, int seconds) {
        return execute(jedis -> jedis.expire(key, seconds));
    }

    public Long ttl(String key) {
        return execute(jedis -> jedis.ttl(key));
    }

    // ==================== Hash 操作 ====================

    public Long hset(String key, String field, String value) {
        return execute(jedis -> jedis.hset(key, field, value));
    }

    public Long hset(String key, Map<String, String> hash) {
        return execute(jedis -> jedis.hset(key, hash));
    }

    public String hget(String key, String field) {
        return execute(jedis -> jedis.hget(key, field));
    }

    public Map<String, String> hgetAll(String key) {
        return execute(jedis -> jedis.hgetAll(key));
    }

    public List<String> hmget(String key, String... fields) {
        return execute(jedis -> jedis.hmget(key, fields));
    }

    public Long hdel(String key, String... fields) {
        return execute(jedis -> jedis.hdel(key, fields));
    }

    public Boolean hexists(String key, String field) {
        return execute(jedis -> jedis.hexists(key, field));
    }

    public Set<String> hkeys(String key) {
        return execute(jedis -> jedis.hkeys(key));
    }

    public List<String> hvals(String key) {
        return execute(jedis -> jedis.hvals(key));
    }

    public Long hlen(String key) {
        return execute(jedis -> jedis.hlen(key));
    }

    // ==================== List 操作 ====================

    public Long lpush(String key, String... values) {
        return execute(jedis -> jedis.lpush(key, values));
    }

    public Long lpush(String key, Collection<String> values) {
        return execute(jedis -> jedis.lpush(key, values.toArray(new String[0])));
    }

    public Long rpush(String key, String... values) {
        return execute(jedis -> jedis.rpush(key, values));
    }

    public Long rpush(String key, Collection<String> values) {
        return execute(jedis -> jedis.rpush(key, values.toArray(new String[0])));
    }

    public String lpop(String key) {
        return execute(jedis -> jedis.lpop(key));
    }

    public String rpop(String key) {
        return execute(jedis -> jedis.rpop(key));
    }

    public List<String> lrange(String key, long start, long stop) {
        return execute(jedis -> jedis.lrange(key, start, stop));
    }

    public String lindex(String key, long index) {
        return execute(jedis -> jedis.lindex(key, index));
    }

    public String lset(String key, long index, String value) {
        return execute(jedis -> jedis.lset(key, index, value));
    }

    public Long lrem(String key, long count, String value) {
        return execute(jedis -> jedis.lrem(key, count, value));
    }

    public Long llen(String key) {
        return execute(jedis -> jedis.llen(key));
    }

    public String ltrim(String key, long start, long stop) {
        return execute(jedis -> jedis.ltrim(key, start, stop));
    }

    // ==================== Set 操作 ====================

    public Long sadd(String key, String... members) {
        return execute(jedis -> jedis.sadd(key, members));
    }

    public Long sadd(String key, Collection<String> members) {
        return execute(jedis -> jedis.sadd(key, members.toArray(new String[0])));
    }

    public Long srem(String key, String... members) {
        return execute(jedis -> jedis.srem(key, members));
    }

    public Long srem(String key, Collection<String> members) {
        return execute(jedis -> jedis.srem(key, members.toArray(new String[0])));
    }

    public Boolean sismember(String key, String member) {
        return execute(jedis -> jedis.sismember(key, member));
    }

    public Set<String> smembers(String key) {
        return execute(jedis -> jedis.smembers(key));
    }

    public Long scard(String key) {
        return execute(jedis -> jedis.scard(key));
    }

    public String srandmember(String key) {
        return execute(jedis -> jedis.srandmember(key));
    }

    public Set<String> srandmember(String key, int count) {
        List<String> members = execute(jedis -> jedis.srandmember(key, count));
        return new HashSet<>(members);
    }

    public String spop(String key) {
        return execute(jedis -> jedis.spop(key));
    }

    public Set<String> sinter(String... keys) {
        return execute(jedis -> jedis.sinter(keys));
    }

    public Set<String> sunion(String... keys) {
        return execute(jedis -> jedis.sunion(keys));
    }

    public Set<String> sdiff(String... keys) {
        return execute(jedis -> jedis.sdiff(keys));
    }

    // ==================== Sorted Set (ZSet) 操作 ====================

    public Long zadd(String key, double score, String member) {
        return execute(jedis -> jedis.zadd(key, score, member));
    }

    public Long zadd(String key, Map<String, Double> scoreMembers) {
        return execute(jedis -> jedis.zadd(key, scoreMembers));
    }

    public Long zrem(String key, String... members) {
        return execute(jedis -> jedis.zrem(key, members));
    }

    public Long zrem(String key, Collection<String> members) {
        return execute(jedis -> jedis.zrem(key, members.toArray(new String[0])));
    }

    public Long zremrangeByRank(String key, long start, long stop) {
        return execute(jedis -> jedis.zremrangeByRank(key, start, stop));
    }

    public Long zremrangeByScore(String key, double min, double max) {
        return execute(jedis -> jedis.zremrangeByScore(key, min, max));
    }

    public Double zincrby(String key, double increment, String member) {
        return execute(jedis -> jedis.zincrby(key, increment, member));
    }

    public Long zrank(String key, String member) {
        return execute(jedis -> jedis.zrank(key, member));
    }

    public Long zrevrank(String key, String member) {
        return execute(jedis -> jedis.zrevrank(key, member));
    }

    public List<String> zrange(String key, long start, long stop) {
        return execute(jedis -> jedis.zrange(key, start, stop));
    }

    public List<String> zrevrange(String key, long start, long stop) {
        return execute(jedis -> jedis.zrevrange(key, start, stop));
    }

    public List<String> zrangeByScore(String key, double min, double max) {
        return execute(jedis -> jedis.zrangeByScore(key, min, max));
    }

    public Map<String, Double> zrangeWithScores(String key, long start, long stop) {
        return execute(jedis -> {
            List<Tuple> tuples = jedis.zrangeWithScores(key, start, stop);
            Map<String, Double> result = new LinkedHashMap<>();
            for (Tuple tuple : tuples) {
                result.put(tuple.getElement(), tuple.getScore());
            }
            return result;
        });
    }

    public Map<String, Double> zrangeByScoreWithScores(String key, double min, double max) {
        return execute(jedis -> {
            List<Tuple> tuples = jedis.zrangeByScoreWithScores(key, min, max);
            Map<String, Double> result = new LinkedHashMap<>();
            for (Tuple tuple : tuples) {
                result.put(tuple.getElement(), tuple.getScore());
            }
            return result;
        });
    }

    public Double zscore(String key, String member) {
        return execute(jedis -> jedis.zscore(key, member));
    }

    public Long zcard(String key) {
        return execute(jedis -> jedis.zcard(key));
    }

    public Long zcount(String key, double min, double max) {
        return execute(jedis -> jedis.zcount(key, min, max));
    }

    // ==================== Keys 操作 ====================

    public Set<String> keys(String pattern) {
        return execute(jedis -> jedis.keys(pattern));
    }

    public Set<String> scan(String pattern, long count) {
        return execute(jedis -> {
            Set<String> result = new HashSet<>();
            ScanParams params = new ScanParams();
            params.match(pattern);
            params.count((int) count);
            String cursor = "0";
            do {
                ScanResult<String> scanResult = jedis.scan(cursor, params);
                result.addAll(scanResult.getResult());
                cursor = scanResult.getCursor();
            } while (!cursor.equals("0"));
            return result;
        });
    }

    public Long delByPattern(String pattern) {
        return execute(jedis -> {
            Set<String> keys = jedis.keys(pattern);
            if (keys.isEmpty()) {
                return 0L;
            }
            return jedis.del(keys.toArray(new String[0]));
        });
    }

    public Long dbSize() {
        return execute(Jedis::dbSize);
    }

    public String flushDB() {
        return execute(Jedis::flushDB);
    }

    public String flushAll() {
        return execute(Jedis::flushAll);
    }

    // ==================== 分布式锁 ====================

    public String setNxEx(String key, String value, int seconds) {
        return execute(jedis -> jedis.set(key, value, redis.clients.jedis.params.SetParams.setParams().nx().ex(seconds)));
    }

    public Long delete(String key) {
        return execute(jedis -> jedis.del(key));
    }

    // ==================== 事务支持 ====================

    public List<Object> executeTransaction(Function<Jedis, Void> action) {
        return execute(jedis -> {
            jedis.multi();
            action.apply(jedis);
            return jedis.multi().exec();
        });
    }
}
