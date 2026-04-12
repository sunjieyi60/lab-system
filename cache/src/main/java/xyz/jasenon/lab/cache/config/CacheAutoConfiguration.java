package xyz.jasenon.lab.cache.config;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import xyz.jasenon.lab.cache.properties.CacheProperties;
import xyz.jasenon.lab.cache.service.RedisCache;
import xyz.jasenon.lab.cache.spi.Cache;
import xyz.jasenon.lab.cache.template.JedisTemplate;
import xyz.jasenon.lab.cache.util.SerializationUtil;

import java.io.IOException;

/**
 * 缓存自动配置类
 * 自动配置 Redis 连接、JedisTemplate、SerializationUtil 等 Bean
 * 从 Spring 环境读取 Redis 配置 (spring.data.redis.*)
 *
 * @author Jasenon
 */
@Configuration
@ConditionalOnClass({JedisPool.class, RedissonClient.class, ObjectMapper.class})
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(prefix = "lab.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class CacheAutoConfiguration {

    private final CacheProperties cacheProperties;

    public CacheAutoConfiguration(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private Integer port;

    @Value("${spring.data.redis.database:0}")
    private Integer database;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.timeout:2000}")
    private Integer timeout;

    /**
     * Jedis 连接池配置
     */
    @Bean
    @ConditionalOnMissingBean(JedisPoolConfig.class)
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(100);
        config.setMaxIdle(50);
        config.setMinIdle(10);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setJmxEnabled(false);
        return config;
    }

    /**
     * Jedis 连接池
     * 从 Spring 环境读取 Redis 配置
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(JedisPool.class)
    public JedisPool jedisPool(JedisPoolConfig jedisPoolConfig) {
        // 密码为空时，不使用密码连接
        if (StrUtil.isBlank(password)) {
            return new JedisPool(jedisPoolConfig, host, port, timeout, null, database);
        }
        return new JedisPool(jedisPoolConfig, host, port, timeout, password, database);
    }

    /**
     * Redisson 客户端
     * 从 Spring 环境读取 Redis 配置
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() throws IOException {
        // 优先加载自定义配置 redisson.yaml
        ClassPathResource resource = new ClassPathResource("redisson.yaml");
        if (resource.exists()) {
            return Redisson.create(Config.fromYAML(resource.getInputStream()));
        }

        // 使用 Spring 配置的 Redis 地址
        Config config = new Config();
        String redisUrl = String.format("redis://%s:%d", host, port);
        config.setCodec(new JsonJacksonCodec());
        
        config.useSingleServer()
                .setAddress(redisUrl)
                .setDatabase(database)
                .setConnectionMinimumIdleSize(10)
                .setConnectionPoolSize(64);
        
        // 设置密码（如果配置了）
        if (StrUtil.isNotBlank(password)) {
            config.useSingleServer().setPassword(password);
        }
        
        return Redisson.create(config);
    }

    /**
     * Jedis 模板
     */
    @Bean
    @ConditionalOnMissingBean(JedisTemplate.class)
    public JedisTemplate jedisTemplate(JedisPool jedisPool) {
        return new JedisTemplate(jedisPool);
    }

    /**
     * 序列化工具
     */
    @Bean
    @ConditionalOnMissingBean(SerializationUtil.class)
    public SerializationUtil serializationUtil(ObjectMapper objectMapper) {
        return new SerializationUtil(objectMapper, cacheProperties.getSerializer());
    }

    /**
     * 缓存服务
     */
    @Bean
    @ConditionalOnMissingBean(Cache.class)
    public Cache cacheService(
            JedisTemplate jedisTemplate,
            RedissonClient redissonClient,
            SerializationUtil serializationUtil) {
        return new RedisCache(
                jedisTemplate,
                redissonClient,
                serializationUtil,
                cacheProperties
        );
    }
}
