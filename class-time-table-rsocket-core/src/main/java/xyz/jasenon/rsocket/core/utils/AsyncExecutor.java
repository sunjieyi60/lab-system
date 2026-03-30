package xyz.jasenon.rsocket.core.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * 全局静态线程池执行器
 * 支持 CompletableFuture 异步编排、超时控制、优雅关闭
 * 
 * 新增：TraceId 跨线程传递支持
 *
 * 设计原则：
 * 1. 静态单例，避免重复创建线程池
 * 2. 分离 IO 密集型与 CPU 密集型任务队列
 * 3. 支持 CompletableFuture 全链路异步
 * 4. 优雅关闭钩子，防止应用重启时线程泄漏
 * 5. 跨线程 TraceId 传递，保证链路追踪完整性
 *
 * @author jasenon
 * @since 2026-03-17
 */
@Slf4j
public final class AsyncExecutor {

    // ==================== 线程池配置常量 ====================

    /** CPU 核心数 */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    /** IO 密集型任务：核心线程数（2*CPU+1，充分利用 IO 等待时间） */
    private static final int IO_CORE_POOL_SIZE = CPU_COUNT * 2 + 1;

    /** IO 密集型任务：最大线程数 */
    private static final int IO_MAX_POOL_SIZE = IO_CORE_POOL_SIZE * 2;

    /** CPU 密集型任务：核心线程数（CPU+1，减少上下文切换） */
    private static final int CPU_CORE_POOL_SIZE = CPU_COUNT + 1;

    /** 空闲线程存活时间（秒） */
    private static final long KEEP_ALIVE_SECONDS = 60L;

    /** 队列容量（防止无限堆积导致 OOM） */
    private static final int QUEUE_CAPACITY = 1000;

    // ==================== 线程池实例 ====================

    /**
     * IO 密集型线程池（网络请求、文件读写、数据库操作）
     * 适用场景：MQTT 消息处理、RS485 通信、HTTP 调用
     */
    private static final ThreadPoolExecutor IO_EXECUTOR = new ThreadPoolExecutor(
            IO_CORE_POOL_SIZE,
            IO_MAX_POOL_SIZE,
            KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            new ThreadFactory() {
                private int count = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "io-pool-" + (++count));
                    t.setDaemon(false); // 非守护线程，确保任务完成
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：调用者线程执行
    );

    /**
     * CPU 密集型线程池（计算密集型任务）
     * 适用场景：数据处理、编解码、加密运算
     */
    private static final ThreadPoolExecutor CPU_EXECUTOR = new ThreadPoolExecutor(
            CPU_CORE_POOL_SIZE,
            CPU_CORE_POOL_SIZE, // 固定大小，避免上下文切换开销
            KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            new ThreadFactory() {
                private int count = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "cpu-pool-" + (++count));
                    t.setDaemon(false);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 调度线程池（定时任务、延迟执行）
     * 适用场景：心跳检测、定时上报、超时控制
     */
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(CPU_COUNT, r -> {
                Thread t = new Thread(r, "scheduler-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            });

    // ==================== 静态初始化块 ====================

    static {
        // 注册 JVM 关闭钩子，确保优雅停机
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("开始关闭异步执行器...");
            shutdownGracefully();
        }));

        log.info("AsyncExecutor 初始化完成 | CPU: {} | IO核心: {} | CPU核心: {}",
                CPU_COUNT, IO_CORE_POOL_SIZE, CPU_CORE_POOL_SIZE);
    }

    // ==================== 私有构造 ====================

    private AsyncExecutor() {
        throw new AssertionError("工具类禁止实例化");
    }

    // ==================== TraceId 包装工具 ====================

    /**
     * 包装 Runnable，传递 TraceId 到新线程
     */
    private static Runnable wrapWithTraceId(Runnable runnable) {
        String traceId = TraceIdContext.get();
        return () -> {
            try {
                if (traceId != null) {
                    TraceIdContext.put(traceId);
                }
                runnable.run();
            } finally {
                TraceIdContext.clear();
            }
        };
    }

    /**
     * 包装 Supplier，传递 TraceId 到新线程
     */
    private static <T> Supplier<T> wrapWithTraceId(Supplier<T> supplier) {
        String traceId = TraceIdContext.get();
        return () -> {
            try {
                if (traceId != null) {
                    TraceIdContext.put(traceId);
                }
                return supplier.get();
            } finally {
                TraceIdContext.clear();
            }
        };
    }

    // ==================== 核心 API：CompletableFuture 支持 ====================

    /**
     * 在 IO 线程池执行异步任务（带返回值）
     * 适用于：网络请求、数据库查询、文件操作
     *
     * @param supplier 任务提供者
     * @param <T> 返回类型
     * @return CompletableFuture 实例
     */
    public static <T> CompletableFuture<T> runAsyncIO(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(wrapWithTraceId(supplier), IO_EXECUTOR);
    }

    /**
     * 在 IO 线程池执行异步任务（无返回值）
     */
    public static CompletableFuture<Void> runAsyncIO(Runnable runnable) {
        return CompletableFuture.runAsync(wrapWithTraceId(runnable), IO_EXECUTOR);
    }

    /**
     * 在 CPU 线程池执行异步任务（带返回值）
     * 适用于：数据计算、加密解密、复杂算法
     */
    public static <T> CompletableFuture<T> runAsyncCPU(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(wrapWithTraceId(supplier), CPU_EXECUTOR);
    }

    /**
     * 在 CPU 线程池执行异步任务（无返回值）
     */
    public static CompletableFuture<Void> runAsyncCPU(Runnable runnable) {
        return CompletableFuture.runAsync(wrapWithTraceId(runnable), CPU_EXECUTOR);
    }

    // ==================== 超时控制 API ====================

    /**
     * 执行带超时的异步任务（IO 池）
     * 超时后自动取消，防止任务悬挂
     *
     * @param supplier 任务
     * @param timeout 超时时间
     * @param unit 时间单位
     * @param <T> 返回类型
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> runWithTimeout(
            Supplier<T> supplier,
            long timeout,
            TimeUnit unit) {

        CompletableFuture<T> task = runAsyncIO(supplier);

        // 调度超时检测
        SCHEDULER.schedule(() -> {
            if (!task.isDone()) {
                task.cancel(true);
                log.warn("任务执行超时 [{} {}] 已取消", timeout, unit);
            }
        }, timeout, unit);

        return task;
    }

    /**
     * 为现有 CompletableFuture 添加超时包装
     * 适用于链式调用中的超时控制
     */
    public static <T> CompletableFuture<T> withTimeout(
            CompletableFuture<T> future,
            long timeout,
            TimeUnit unit,
            T defaultValue) {

        return future.orTimeout(timeout, unit)
                .exceptionally(ex -> {
                    log.warn("Future 超时或异常，返回默认值: {}", ex.getMessage());
                    return defaultValue;
                });
    }

    // ==================== 批量任务 API ====================

    /**
     * 并行执行多个 Supplier，等待全部完成
     * 适用于：批量查询、聚合多个服务数据
     *
     * @param suppliers 任务列表
     * @param <T> 返回类型
     * @return CompletableFuture<List<T>>
     */
    @SafeVarargs
    public static <T> CompletableFuture<java.util.List<T>> allOf(Supplier<T>... suppliers) {
        @SuppressWarnings("unchecked")
        CompletableFuture<T>[] futures = new CompletableFuture[suppliers.length];

        for (int i = 0; i < suppliers.length; i++) {
            futures[i] = runAsyncIO(suppliers[i]);
        }

        return CompletableFuture.allOf(futures)
                .thenApply(v -> {
                    java.util.List<T> results = new java.util.ArrayList<>(suppliers.length);
                    for (CompletableFuture<T> f : futures) {
                        results.add(f.join()); // join 在 allOf 后不会阻塞
                    }
                    return results;
                });
    }

    /**
     * 竞争执行：多个任务取最快返回的结果
     * 适用于：多源数据获取、故障转移场景
     *
     * @param suppliers 任务列表
     * @param <T> 返回类型
     * @return CompletableFuture<T>
     */
    @SafeVarargs
    public static <T> CompletableFuture<T> anyOf(Supplier<T>... suppliers) {
        @SuppressWarnings("unchecked")
        CompletableFuture<T>[] futures = new CompletableFuture[suppliers.length];

        for (int i = 0; i < suppliers.length; i++) {
            futures[i] = runAsyncIO(suppliers[i]);
        }

        return CompletableFuture.anyOf(futures)
                .thenApply(result -> (T) result);
    }

    // ==================== 调度任务 API ====================

    /**
     * 延迟执行（异步）
     *
     * @param runnable 任务
     * @param delay 延迟时间
     * @param unit 时间单位
     * @return ScheduledFuture
     */
    public static ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit) {
        return SCHEDULER.schedule(wrapWithTraceId(runnable), delay, unit);
    }

    /**
     * 固定频率执行（异步）
     * 适用：定时心跳、状态上报
     *
     * @param runnable 任务
     * @param initialDelay 初始延迟
     * @param period 周期
     * @param unit 时间单位
     * @return ScheduledFuture
     */
    public static ScheduledFuture<?> scheduleAtFixedRate(
            Runnable runnable,
            long initialDelay,
            long period,
            TimeUnit unit) {
        return SCHEDULER.scheduleAtFixedRate(wrapWithTraceId(runnable), initialDelay, period, unit);
    }

    // ==================== 线程池监控 API ====================

    /**
     * 获取 IO 线程池状态
     */
    public static PoolStats getIOStats() {
        return new PoolStats(IO_EXECUTOR);
    }

    /**
     * 获取 CPU 线程池状态
     */
    public static PoolStats getCPUStats() {
        return new PoolStats(CPU_EXECUTOR);
    }

    /**
     * 线程池统计信息
     */
    public static class PoolStats {
        public final int activeCount;
        public final int poolSize;
        public final int corePoolSize;
        public final int largestPoolSize;
        public final long taskCount;
        public final long completedTaskCount;
        public final int queueSize;
        public final int remainingQueueCapacity;

        public PoolStats(ThreadPoolExecutor executor) {
            this.activeCount = executor.getActiveCount();
            this.poolSize = executor.getPoolSize();
            this.corePoolSize = executor.getCorePoolSize();
            this.largestPoolSize = executor.getLargestPoolSize();
            this.taskCount = executor.getTaskCount();
            this.completedTaskCount = executor.getCompletedTaskCount();
            this.queueSize = executor.getQueue().size();
            this.remainingQueueCapacity = executor.getQueue().remainingCapacity();
        }

        @Override
        public String toString() {
            return String.format(
                    "PoolStats{active=%d, pool=%d/%d, largest=%d, tasks=%d/%d, queue=%d/%d}",
                    activeCount, poolSize, corePoolSize, largestPoolSize,
                    completedTaskCount, taskCount,
                    queueSize, queueSize + remainingQueueCapacity
            );
        }
    }

    // ==================== 生命周期管理 ====================

    /**
     * 优雅关闭所有线程池
     * 等待正在执行的任务完成，拒绝新任务
     */
    public static void shutdownGracefully() {
        shutdownExecutor(IO_EXECUTOR, "IO-Executor");
        shutdownExecutor(CPU_EXECUTOR, "CPU-Executor");
        shutdownScheduler(SCHEDULER, "Scheduler");
    }

    private static void shutdownExecutor(ThreadPoolExecutor executor, String name) {
        if (executor.isShutdown()) {
            return;
        }

        log.info("正在关闭 {}...", name);
        executor.shutdown(); // 禁止新任务

        try {
            // 等待 60 秒让现有任务完成
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("{} 未在 60 秒内完成，强制关闭", name);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static void shutdownScheduler(ScheduledExecutorService scheduler, String name) {
        if (scheduler.isShutdown()) {
            return;
        }

        log.info("正在关闭 {}...", name);
        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 立即强制关闭（仅紧急情况下使用）
     */
    public static void shutdownNow() {
        IO_EXECUTOR.shutdownNow();
        CPU_EXECUTOR.shutdownNow();
        SCHEDULER.shutdownNow();
        log.warn("所有执行器已强制关闭");
    }
}
