package xyz.jasenon.lab.service.strategy.device;

import org.springframework.stereotype.Component;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Component
public class PollingScheduleExecutorPool {

    private final ScheduledThreadPoolExecutor threadPoolExecutor;
    private final PollingProperties pollingProperties;

    public PollingScheduleExecutorPool(PollingProperties pollingProperties) {
        this.threadPoolExecutor = new ScheduledThreadPoolExecutor(
                pollingProperties.getCorePoolSize(),
                new PollingThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        this.pollingProperties = pollingProperties;
    }

    public Future<?> submit(Runnable task){
        return threadPoolExecutor.scheduleAtFixedRate(task,
                pollingProperties.getInitialDelay(),
                pollingProperties.getPeriod(),
                pollingProperties.getPollingTimeUnit());
    }

}
