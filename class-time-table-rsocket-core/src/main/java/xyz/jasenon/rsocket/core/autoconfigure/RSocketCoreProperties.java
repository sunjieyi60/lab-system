package xyz.jasenon.rsocket.core.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RSocket Core 配置属性
 */
@Data
@ConfigurationProperties(prefix = "rsocket.core")
public class RSocketCoreProperties {

    /**
     * 是否启用自动配置
     */
    private boolean enabled = true;

    /**
     * Redis 配置
     */
    private RedisProperties redis = new RedisProperties();

    /**
     * 连接管理配置
     */
    private ConnectionProperties connection = new ConnectionProperties();

    @Data
    public static class RedisProperties {
        /**
         * 是否启用 JedisPool 自动配置
         */
        private boolean enabled = true;

        /**
         * 最大连接数
         */
        private int maxTotal = 20;

        /**
         * 最大空闲连接
         */
        private int maxIdle = 10;

        /**
         * 最小空闲连接
         */
        private int minIdle = 5;

        /**
         * 连接超时（毫秒）
         */
        private int timeout = 2000;
    }

    @Data
    public static class ConnectionProperties {
        /**
         * 是否启用连接管理
         */
        private boolean enabled = true;

        /**
         * 心跳检查间隔（秒）
         */
        private int heartbeatInterval = 30;

        /**
         * 连接超时时间（秒）
         */
        private int timeout = 60;
    }
}
