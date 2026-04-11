package xyz.jasenon.lab.cache.annotation;

import org.springframework.context.annotation.Import;
import xyz.jasenon.lab.cache.config.CacheAutoConfiguration;

import java.lang.annotation.*;

/**
 * 启用实验室缓存组件
 * 在 Spring Boot 应用主类上添加此注解以启用缓存功能
 *
 * @author Jasenon
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CacheAutoConfiguration.class)
public @interface EnableLabCache {

    /**
     * 缓存名称
     */
    String cacheName() default "default";

    /**
     * 是否启用缓存
     */
    boolean enabled() default true;
}
