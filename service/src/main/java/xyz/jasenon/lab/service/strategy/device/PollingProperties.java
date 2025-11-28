package xyz.jasenon.lab.service.strategy.device;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Configuration
@ConfigurationProperties(prefix = "task.polling")
@Getter
@Setter
public class PollingProperties {

    /**
     * 核心线程数
     */
    private Integer corePoolSize;

    /**
     * 轮询启动延迟时间
     */
    private Long initialDelay;

    /**
     * 轮询间隔时间
     */
    private Long period;

    /**
     * 轮询时间单位
     */
    private TimeUnit pollingTimeUnit;

}
