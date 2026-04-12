package xyz.jasenon.lab.mqtt.config;

import cn.hutool.core.util.StrUtil;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

/**
 * @author Jasenon_ce
 * @date 2026/4/12
 */
@Configuration
public class RedissonConfig {

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

    @Bean
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
}
