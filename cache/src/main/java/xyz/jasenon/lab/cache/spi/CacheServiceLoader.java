package xyz.jasenon.lab.cache.spi;

import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * 缓存服务 SPI 加载器
 * 用于通过 SPI 机制加载缓存服务实现
 *
 * @author Jasenon
 */
@Slf4j
public class CacheServiceLoader {

    private static final ServiceLoader<Cache> LOADER = ServiceLoader.load(Cache.class);

    /**
     * 获取第一个可用的缓存服务
     *
     * @return 缓存服务实例，如果没有找到则返回 null
     */
    public static Cache load() {
        Iterator<Cache> iterator = LOADER.iterator();
        if (iterator.hasNext()) {
            Cache service = iterator.next();
            log.info("通过 SPI 加载缓存服务: {}", service.getClass().getName());
            return service;
        }
        log.warn("未找到可用的缓存服务实现");
        return null;
    }

    /**
     * 重新加载缓存服务
     * 用于动态更新场景
     */
    public static void reload() {
        LOADER.reload();
        log.info("缓存服务 SPI 已重新加载");
    }
}
