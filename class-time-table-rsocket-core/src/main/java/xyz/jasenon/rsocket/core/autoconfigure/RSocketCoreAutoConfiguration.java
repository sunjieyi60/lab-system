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
import xyz.jasenon.rsocket.core.rsocket.strategy.*;

/**
 * RSocket Core 自动配置类
 * 
 * 自动配置以下 Bean：
 * - JedisPool / JedisTemplate / Cache
 * - ConnectionManager
 * - Server / ServerImpl
 * - Client / ClientImpl
 * - SendStrategyManager 及所有策略
 */
@AutoConfiguration(before = JmxAutoConfiguration.class)
@EnableConfigurationProperties(RSocketCoreProperties.class)
@ConditionalOnClass(RSocketRequester.class)
@ConditionalOnProperty(prefix = "rsocket.core", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({
    RSocketCoreAutoConfiguration.RedisConfiguration.class,
    RSocketCoreAutoConfiguration.StrategyConfiguration.class,
    RSocketCoreAutoConfiguration.RSocketConfiguration.class
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

    // ==================== 策略配置 ====================

    @Configuration
    static class StrategyConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public SendStrategyManager sendStrategyManager(java.util.List<SendStrategy> strategies) {
            return new SendStrategyManager(strategies);
        }

        @Bean
        @ConditionalOnMissingBean
        public RequestResponseStrategy requestResponseStrategy() {
            return new RequestResponseStrategy();
        }

        @Bean
        @ConditionalOnMissingBean
        public FireAndForgetStrategy fireAndForgetStrategy() {
            return new FireAndForgetStrategy();
        }

        @Bean
        @ConditionalOnMissingBean
        public RequestStreamStrategy requestStreamStrategy() {
            return new RequestStreamStrategy();
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
