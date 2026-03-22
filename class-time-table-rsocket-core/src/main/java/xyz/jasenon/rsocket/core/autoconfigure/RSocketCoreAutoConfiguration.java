package xyz.jasenon.rsocket.core.autoconfigure;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.rsocket.RSocketRequester;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import xyz.jasenon.rsocket.core.Api;
import xyz.jasenon.rsocket.core.cache.Cache;
import xyz.jasenon.rsocket.core.cache.JedisTemplate;
import xyz.jasenon.rsocket.core.rsocket.*;
import xyz.jasenon.rsocket.core.rsocket.client.Client;
import xyz.jasenon.rsocket.core.rsocket.client.ClientImpl;

/**
 * RSocket Core 自动配置类
 * 
 * 自动配置以下 Bean：
 * - JedisPool / JedisTemplate / Cache
 * - ConnectionManager
 * - Server / ServerImpl
 * - Client / ClientImpl
 * 
 * 注意：SendStrategy 已被移除，直接使用 RSocketRequester 发送消息
 */
@AutoConfiguration(before = JmxAutoConfiguration.class)
@EnableConfigurationProperties(RSocketCoreProperties.class)
@ConditionalOnClass(RSocketRequester.class)
@ConditionalOnProperty(prefix = "rsocket.core", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({
    RSocketCoreAutoConfiguration.RedisConfiguration.class,
    RSocketCoreAutoConfiguration.RSocketConfiguration.class,
    xyz.jasenon.rsocket.core.rsocket.client.handler.HandlerConfiguration.class
})
public class RSocketCoreAutoConfiguration {

    // ==================== Redis 配置 ====================

    @Configuration
    @ConditionalOnClass(JedisPool.class)
    static class RedisConfiguration {

        @Value("${spring.data.redis.host:localhost}")
        private String redisHost;

        @Value("${spring.data.redis.port:6379}")
        private int redisPort;

        @Value("${spring.data.redis.database:0}")
        private int redisDatabase;

        @Value("${spring.data.redis.timeout:2000}")
        private int redisTimeout;

        @Bean(destroyMethod = "close")
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "rsocket.core.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
        public JedisPool jedisPool(RSocketCoreProperties properties) {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            RSocketCoreProperties.RedisProperties redisProps = properties.getRedis();
            poolConfig.setMaxTotal(redisProps.getMaxTotal());
            poolConfig.setMaxIdle(redisProps.getMaxIdle());
            poolConfig.setMinIdle(redisProps.getMinIdle());
            // 禁用 JMX 避免与 Spring JMX 冲突
            poolConfig.setJmxEnabled(false);
            return new JedisPool(poolConfig, redisHost, redisPort, redisTimeout, null, redisDatabase);
        }

        @Bean
        @ConditionalOnMissingBean
        public JedisTemplate jedisTemplate(JedisPool jedisPool) {
            return new JedisTemplate(jedisPool);
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnClass(RedissonClient.class)
        public Cache cache(JedisTemplate jedisTemplate, RedissonClient redissonClient) {
            return new Cache(jedisTemplate, redissonClient);
        }
    }

    // ==================== RSocket 服务配置 ====================

    @Configuration
    static class RSocketConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "rsocket.core.connection", name = "enabled", havingValue = "true", matchIfMissing = true)
        public ConnectionManager connectionManager() {
            return new ConnectionManager();
        }

        @Bean
        @ConditionalOnMissingBean(Server.class)
        public ServerImpl serverImpl() {
            return new ServerImpl();
        }

        @Bean
        @ConditionalOnMissingBean(Client.class)
        public ClientImpl clientImpl() {
            return new ClientImpl();
        }

        @Bean
        @ConditionalOnMissingBean(Api.class)
        public Api api(ConnectionManager manager, Server server){
            return new Api(manager, server);
        }
    }

}
