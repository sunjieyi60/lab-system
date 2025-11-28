package xyz.jasenon.lab.service.strategy.device;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Slf4j
public class PollingThreadFactory implements ThreadFactory {

    private final AtomicInteger index = new AtomicInteger();

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName("polling-thread-" + index.incrementAndGet());
        thread.setUncaughtExceptionHandler((t, e) -> {
            log.error("Uncaught exception in polling thread: ", e);
            t.interrupt();
        });
        return thread;
    }
}
