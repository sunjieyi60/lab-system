package xyz.jasenon.lab.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 日志专用线程池配置。
 */
@Configuration
public class LogExecutorConfig {

    @Bean(name = "logExecutor")
    public ExecutorService logExecutor() {
        int core = Runtime.getRuntime().availableProcessors();
        // 适中大小的线程池与有界队列，避免挤占业务线程。
        return new ThreadPoolExecutor(
                core,
                core * 2,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10_000),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("log-exec-" + t.getId());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.DiscardPolicy() // 队列满时丢弃最旧任务，避免打挂主流程
        );
    }
}

