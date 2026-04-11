package xyz.jasenon.lab.cache.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Redis 缓存配置属性
 *
 * @author Jasenon
 */
@Data
@ConfigurationProperties(prefix = "lab.cache")
public class CacheProperties {

    /**
     * 是否启用缓存
     */
    private boolean enabled = true;

    /**
     * 默认过期时间
     */
    private Duration defaultExpiration = Duration.ofMinutes(30);

    /**
     * 缓存空值（防止缓存穿透）
     */
    private boolean cacheNullValues = true;

    /**
     * 空值缓存过期时间（秒）
     */
    private int nullValueExpiration = 60;

    /**
     * 序列化方式：jackson/jdk
     */
    private String serializer = "jackson";

}
