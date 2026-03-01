package xyz.jasenon.lab.core.cache.redis;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.utils.SystemTimer;
import xyz.jasenon.lab.core.cache.ICache;
import xyz.jasenon.lab.core.cache.redis.JedisTemplate.Pair;
import xyz.jasenon.lab.core.cache.redis.JedisTemplate.PairEx;
import xyz.jasenon.lab.core.utils.JsonKit;

import java.io.Serializable;
import java.util.*;
/**
 *
 * @author wchao
 * 2017年8月10日 下午1:35:01
 */
public class RedisCache implements ICache {
	
	private Logger log = LoggerFactory.getLogger(RedisCache.class);
	
	public static String cacheKey(String cacheName, String key) {
		return keyPrefix(cacheName) + key;
	}

	public static String keyPrefix(String cacheName) {
		return cacheName + ":";
	}

	public static void main(String[] args) {
	}

	private String cacheName = null;

	private Integer timeToLiveSeconds = null;

	private Integer timeToIdleSeconds = null;

	private Integer timeout = null;

	public RedisCache(String cacheName, Integer timeToLiveSeconds, Integer timeToIdleSeconds) {
		this.cacheName = cacheName;
		this.timeToLiveSeconds = timeToLiveSeconds;
		this.timeToIdleSeconds = timeToIdleSeconds;
		this.timeout = this.timeToLiveSeconds == null ? this.timeToIdleSeconds : this.timeToLiveSeconds;

	}

	@Override
	public void clear() {
		long start = SystemTimer.currentTimeMillis();
		try {
			JedisTemplate.me().delKeysLike(keyPrefix(cacheName));
		} catch (Exception e) {
			log.error(e.toString(),e);
		}
		long end = SystemTimer.currentTimeMillis();
		long iv = end - start;
		log.info("clear cache {}, cost {}ms", cacheName, iv);
	}

	@Override
	public Serializable get(String key) {
		if (StringUtils.isBlank(key)) {
			return null;
		}
		Serializable value = null;
		try {
			value = JedisTemplate.me().get(cacheKey(cacheName, key), Serializable.class);
			if (timeToIdleSeconds != null) {
				if (value != null) {
					RedisExpireUpdateTask.add(cacheName, key, value ,timeout);
				}
			}
		} catch (Exception e) {
			log.error(e.toString(),e);
		}
		return value;
	}
	@Override
	public <T> T get(String key, Class<T> clazz) {
		if (StringUtils.isBlank(key)) {
			return null;
		}
		T value = null;
		try {
			value = JedisTemplate.me().get(cacheKey(cacheName, key),clazz);
			if (timeToIdleSeconds != null) {
				if (value != null) {
					RedisExpireUpdateTask.add(cacheName, key, (Serializable) value ,timeout);
				}
			}
		} catch (Exception e) {
			log.error(e.toString(),e);
		}
		return value;
	}
	@Override
	public Collection<String> keys() {
		try {
			return JedisTemplate.me().keys(keyPrefix(cacheName));
		} catch (Exception e) {
			log.error(e.toString(),e);
		}
		return null;
	}

	@Override
	public void put(String key, Serializable value) {
		if (StringUtils.isBlank(key)) {
			return;
		}
		try {
			JedisTemplate.me().set(cacheKey(cacheName, key), value, Integer.parseInt(timeout+""));
		}catch (Exception e) {
			log.error(e.toString(),e);
		}
	}
	public void putAll(List<Pair<String,Serializable>> values) {
		if (values == null || values.size() < 1) {
			return;
		}
		int expire = Integer.parseInt(timeout+"");
		try {
			List<PairEx<String,String,Integer>> pairDatas = new ArrayList<PairEx<String,String,Integer>>();
			for(Pair<String,Serializable> pair : values){
				pairDatas.add(JedisTemplate.me().makePairEx(cacheKey(cacheName, pair.getKey()), JsonKit.toJSONString(pair.getValue()),expire));
			}
			JedisTemplate.me().batchSetStringEx(pairDatas);
		}catch (Exception e) {
			log.error(e.toString(),e);
		}
	}
	public void listPushTail(String key, Serializable value) {
		if (StringUtils.isBlank(key)) {
			return;
		}
		try {
			String jsonValue = value instanceof String? (String)value:JsonKit.toJSONString(value);
			JedisTemplate.me().listPushTail(cacheKey(cacheName, key),jsonValue);
		}catch (Exception e) {
			log.error(e.toString(),e);
		}
	}
	public List<String> listGetAll(String key) {
		if (StringUtils.isBlank(key)) {
			return null;
		}
		try {
			return JedisTemplate.me().listGetAll(cacheKey(cacheName, key));
		}catch (Exception e) {
			log.error(e.toString(),e);
		}
		return null;
	}
	public Long listRemove(String key ,String value){
		if(StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
			return 0L;
		}
		try {
			return JedisTemplate.me().listRemove(cacheKey(cacheName, key), 0, value);
		} catch (Exception e) {
			log.error(e.toString(),e);
		}
		return 0L;
	}
	public void sortSetPush(String key ,double score , Serializable value){
		if (StringUtils.isBlank(key)) {
			return;
		}
		try {
			String jsonValue = value instanceof String? (String)value:JsonKit.toJSONString(value);
			JedisTemplate.me().sortSetPush(cacheKey(cacheName, key),score,jsonValue);
		}catch (Exception e) {
			log.error(e.toString(),e);
		}
	}
	public List<String> sortSetGetAll(String key){
		if (StringUtils.isBlank(key)) {
			return null;
		}
		try {
			Set<String> dataSet = JedisTemplate.me().sorSetRangeByScore(cacheKey(cacheName, key),Double.MIN_VALUE,Double.MAX_VALUE);
			if(dataSet == null) {
				return null;
			}
			return new ArrayList<String>(dataSet);
		}catch (Exception e) {
			log.error(e.toString(),e);
		}
		return null;
	}
	public List<String> sortSetGetAll(String key,double min,double max){
		if (StringUtils.isBlank(key)) {
			return null;
		}
		try {
			Set<String> dataSet = JedisTemplate.me().sorSetRangeByScore(cacheKey(cacheName, key),min,max);
			if(dataSet == null) {
				return null;
			}
			return new ArrayList<String>(dataSet);
		}catch (Exception e) {
			log.error(e.toString(),e);
		}
		return null;
	}
	public List<String> sortSetGetAll(String key,double min,double max,int offset ,int count){
		if (StringUtils.isBlank(key)) {
			return null;
		}
		try {
			Set<String> dataSet = JedisTemplate.me().sorSetRangeByScore(cacheKey(cacheName, key),min,max,offset,count);
			if(dataSet == null) {
				return null;
			}
			return new ArrayList<String>(dataSet);
		}catch (Exception e) {
			log.error(e.toString(),e);
		}
		return null;
	}
	@Override
	public void putTemporary(String key, Serializable value) {
		if (StringUtils.isBlank(key)) {
			return;
		}
		try {
			JedisTemplate.me().set(cacheKey(cacheName, key), value,10);
		} catch (Exception e) {
			log.error(e.toString(),e);
		}
	}

	@Override
	public void remove(String key) {
		if (StringUtils.isBlank(key)) {
			return;
		}
		try {
			JedisTemplate.me().delKey(cacheKey(cacheName, key));
		} catch (Exception e) {
			log.error(e.toString(),e);
		}
	}
	
	public String getCacheName() {
		return cacheName;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public Integer getTimeToIdleSeconds() {
		return timeToIdleSeconds;
	}

	public Integer getTimeToLiveSeconds() {
		return timeToLiveSeconds;
	}

	/* ======================================Hashes====================================== */

	/**
	 * 存储 Map<String, Object> 到 Redis Hash
	 * 将 Object 序列化为 JSON String 存储
	 *
	 * @param key   缓存 key
	 * @param map   要存储的 Map，value 需要实现 Serializable
	 */
	public void putHash(String key, Map<String, ? extends Serializable> map) {
		if (StringUtils.isBlank(key) || map == null) {
			return;
		}
		try {
			String cacheKey = cacheKey(cacheName, key);

			Map<String, String> hash = new HashMap<>();
			for (Map.Entry<String, ? extends Serializable> entry : map.entrySet()) {
				// Object 转 JSON String
				String jsonValue = entry.getValue() instanceof String
					? (String) entry.getValue()
					: JsonKit.toJSONString(entry.getValue());
				hash.put(entry.getKey(), jsonValue);
			}

			JedisTemplate.me().hashMultipleSet(cacheKey, hash);
		} catch (Exception e) {
			log.error(e.toString(), e);
		}
	}

	/**
	 * 获取整个 Hash 的所有字段
	 *
	 * @param key 缓存 key
	 * @return Map<String, String> 所有字段值对，如果 key 不存在返回 null
	 */
	public Map<String, String> getHash(String key) {
		if (StringUtils.isBlank(key)) {
			return null;
		}
		try {
			String cacheKey = cacheKey(cacheName, key);
			return JedisTemplate.me().hashGetAll(cacheKey);
		} catch (Exception e) {
			log.error(e.toString(), e);
			return null;
		}
	}

	/**
	 * 获取 Hash 中的单个字段，并反序列化为指定类型
	 *
	 * @param key   缓存 key
	 * @param field 字段名
	 * @param clazz 目标类型
	 * @param <T>   类型参数
	 * @return 字段值，如果不存在返回 null
	 */
	public <T> T getHashField(String key, String field, Class<T> clazz) {
		if (StringUtils.isBlank(key) || StringUtils.isBlank(field)) {
			return null;
		}
		try {
			String cacheKey = cacheKey(cacheName, key);
			String value = JedisTemplate.me().hashGet(cacheKey, field);

			if (value == null) {
				return null;
			}

			if (clazz == String.class) {
				return (T) value;
			}

			return JsonKit.toBean(value.getBytes(), clazz);
		} catch (Exception e) {
			log.error(e.toString(), e);
			return null;
		}
	}

	/**
	 * 设置 Hash 中的单个字段
	 *
	 * @param key   缓存 key
	 * @param field 字段名
	 * @param value 字段值，会被序列化为 JSON
	 */
	public void putHashField(String key, String field, Serializable value) {
		if (StringUtils.isBlank(key) || StringUtils.isBlank(field)) {
			return;
		}
		try {
			String cacheKey = cacheKey(cacheName, key);
			String jsonValue = value instanceof String
				? (String) value
				: JsonKit.toJSONString(value);

			JedisTemplate.me().hashSet(cacheKey, field, jsonValue);
		} catch (Exception e) {
			log.error(e.toString(), e);
		}
	}

	/**
	 * 删除 Hash 中的指定字段
	 *
	 * @param key    缓存 key
	 * @param fields 要删除的字段名数组
	 */
	public void removeHashField(String key, String... fields) {
		if (StringUtils.isBlank(key) || fields == null || fields.length == 0) {
			return;
		}
		try {
			String cacheKey = cacheKey(cacheName, key);
			JedisTemplate.me().hashDel(cacheKey, fields);
		} catch (Exception e) {
			log.error(e.toString(), e);
		}
	}

	/**
	 * 判断 Hash 中是否存在指定字段
	 *
	 * @param key   缓存 key
	 * @param field 字段名
	 * @return true-存在，false-不存在或 key 不存在
	 */
	public boolean hasHashField(String key, String field) {
		if (StringUtils.isBlank(key) || StringUtils.isBlank(field)) {
			return false;
		}
		try {
			String cacheKey = cacheKey(cacheName, key);
			return JedisTemplate.me().getJedis().hexists(cacheKey, field);
		} catch (Exception e) {
			log.error(e.toString(), e);
			return false;
		}
	}

	/**
	 * 获取 Hash 中所有字段的数量
	 *
	 * @param key 缓存 key
	 * @return 字段数量，如果 key 不存在返回 0
	 */
	public long getHashSize(String key) {
		if (StringUtils.isBlank(key)) {
			return 0;
		}
		try {
			String cacheKey = cacheKey(cacheName, key);
			return JedisTemplate.me().getJedis().hlen(cacheKey);
		} catch (Exception e) {
			log.error(e.toString(), e);
			return 0;
		}
	}

	/**
	 * 获取 Hash 中所有字段名
	 *
	 * @param key 缓存 key
	 * @return 字段名集合，如果 key 不存在返回 null
	 */
	public Set<String> getHashFields(String key) {
		if (StringUtils.isBlank(key)) {
			return null;
		}
		try {
			String cacheKey = cacheKey(cacheName, key);
			return JedisTemplate.me().getJedis().hkeys(cacheKey);
		} catch (Exception e) {
			log.error(e.toString(), e);
			return null;
		}
	}

	/**
	 * 批量获取 Hash 中的多个字段（带过期时间刷新）
	 *
	 * @param key    缓存 key
	 * @param fields 字段名数组
	 * @return 字段值列表，按 fields 顺序排列
	 */
	public List<String> getHashMultiField(String key, String... fields) {
		if (StringUtils.isBlank(key) || fields == null || fields.length == 0) {
			return null;
		}
		try {
			String cacheKey = cacheKey(cacheName, key);
			List<String> values = JedisTemplate.me().hashMultipleGet(cacheKey, fields);
			
			// 如果有 timeToIdleSeconds 配置，刷新过期时间
			if (timeToIdleSeconds != null && values != null && !values.isEmpty()) {
				JedisTemplate.me().expire(cacheKey, timeout);
			}
			
			return values;
		} catch (Exception e) {
			log.error(e.toString(), e);
			return null;
		}
	}
}
