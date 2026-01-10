package xyz.jasenon.lab.service.quartz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Quartz任务执行与看门狗线程池配置
 */
@Configuration
public class TaskExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService taskRuntimeExecutor() {
        int core = Math.max(Runtime.getRuntime().availableProcessors(), 4);
        return new ThreadPoolExecutor(
                core,
                core * 2,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200),
                new NamedThreadFactory("task-runtime"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService watchDogScheduler() {
        return Executors.newScheduledThreadPool(2, new NamedThreadFactory("watchdog"));
    }

    /**
     * 简单的自定义线程工厂，用于设置线程名前缀与守护属性。
     */
    static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger seq = new AtomicInteger(1);

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName(prefix + "-" + seq.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
