package xyz.jasenon.lab.service.strategy.device;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Slf4j
@Component
@DependsOn({"mqttTaskSendStrategy","socketTaskSendStrategy"})
public class PollingScheduleExecutorPool {

    private final ScheduledThreadPoolExecutor threadPoolExecutor;
    private final PollingProperties pollingProperties;
    // 存储 deviceId -> Future 的映射，用于管理轮询任务
    private final ConcurrentHashMap<Long, Future<?>> devicePollingFutures = new ConcurrentHashMap<>();

    public PollingScheduleExecutorPool(PollingProperties pollingProperties) {
        this.pollingProperties = pollingProperties;
        this.threadPoolExecutor = new ScheduledThreadPoolExecutor(
                pollingProperties.getCorePoolSize(),
                new PollingThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        // 设置线程池在关闭时等待任务完成
        threadPoolExecutor.setRemoveOnCancelPolicy(true);
        log.info("轮询调度线程池初始化完成，核心线程数: {}, 初始延迟: {} {}, 执行间隔: {} {}", 
                pollingProperties.getCorePoolSize(),
                pollingProperties.getInitialDelay(), 
                pollingProperties.getPollingTimeUnit(),
                pollingProperties.getPeriod(),
                pollingProperties.getPollingTimeUnit());
    }

    /**
     * 提交定时任务，按照固定间隔重复执行
     * 注意：如果任务执行时抛出未捕获的异常，ScheduledThreadPoolExecutor 会自动取消后续调度
     * 这是设计意图：当设备被删除时，通过抛出异常来停止该设备的轮询任务
     * 
     * @param deviceId 设备ID，用于管理任务
     * @param task 要执行的任务
     * @return Future对象，可用于取消任务
     */
    public Future<?> submit(Long deviceId, Runnable task){
        log.debug("提交轮询任务，deviceId: {}, 初始延迟: {} {}, 执行间隔: {} {}", 
                deviceId,
                pollingProperties.getInitialDelay(), 
                pollingProperties.getPollingTimeUnit(),
                pollingProperties.getPeriod(),
                pollingProperties.getPollingTimeUnit());
        
        // 如果该设备已有轮询任务，先取消旧的
        Future<?> oldFuture = devicePollingFutures.remove(deviceId);
        if (oldFuture != null && !oldFuture.isCancelled()) {
            log.info("取消设备 {} 的旧轮询任务", deviceId);
            oldFuture.cancel(false);
        }
        
        Future<?> future = threadPoolExecutor.scheduleAtFixedRate(
                wrapTaskWithExceptionHandling(deviceId, task),
                pollingProperties.getInitialDelay(),
                pollingProperties.getPeriod(),
                pollingProperties.getPollingTimeUnit());
        
        devicePollingFutures.put(deviceId, future);
        log.debug("轮询任务已提交，deviceId: {}, Future: {}", deviceId, future);
        return future;
    }

    /**
     * 包装任务，当任务抛出异常时，自动清理 Future 映射
     */
    private Runnable wrapTaskWithExceptionHandling(Long deviceId, Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                // 任务抛出异常时，清理 Future 映射
                Future<?> future = devicePollingFutures.remove(deviceId);
                if (future != null) {
                    log.info("设备 {} 的轮询任务因异常终止，已清理 Future 映射。异常: {}", deviceId, e.getMessage());
                }
                // 重新抛出异常，让 ScheduledThreadPoolExecutor 知道任务失败，停止后续调度
                throw e;
            }
        };
    }

    /**
     * 取消指定设备的轮询任务
     * @param deviceId 设备ID
     * @return 是否成功取消
     */
    public boolean cancelPolling(Long deviceId) {
        Future<?> future = devicePollingFutures.remove(deviceId);
        if (future != null && !future.isCancelled()) {
            boolean cancelled = future.cancel(false);
            log.info("取消设备 {} 的轮询任务，结果: {}", deviceId, cancelled);
            return cancelled;
        }
        log.debug("设备 {} 没有正在运行的轮询任务", deviceId);
        return false;
    }

    /**
     * 关闭线程池（应用关闭时调用）
     */
    public void shutdown() {
        log.info("开始关闭轮询调度线程池，当前有 {} 个活跃的轮询任务", devicePollingFutures.size());
        // 取消所有任务
        devicePollingFutures.values().forEach(future -> {
            if (future != null && !future.isCancelled()) {
                future.cancel(false);
            }
        });
        devicePollingFutures.clear();
        threadPoolExecutor.shutdown();
    }

}
