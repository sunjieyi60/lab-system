package xyz.jasenon.lab.mqtt.setnx;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MqttNx {

    @Getter
    private final String key = UUID.randomUUID().toString();
    private final RedissonClient redissonClient;
    private final String prefix = "mqttnx:";
    private final String latchPrefix = "mqttnx-latch:";
    private Long timeout = 500L;
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    /** 是否有未确认的发送（用于检测超时释放：tryLock 再次成功且此前未 unlock 则视为超时） */
    private volatile boolean pendingSend = false;
    
    /** 用于跨线程等待/唤醒的 CountDownLatch */
    private RCountDownLatch countDownLatch;

    public MqttNx(RedissonClient redissonClient, Long timeout,
        TimeUnit timeUnit
    ) {
        this.redissonClient = redissonClient;
        this.timeout = timeout > 0 ? timeout : 500;
        this.timeUnit = timeUnit;
    }
    public MqttNx(RedissonClient redissonClient){
        this.redissonClient = redissonClient;
    }

    /**
     * 尝试加锁：只有key不存在时才能设置成功（上一条已确认或已超时）
     */
    public boolean tryLock() throws InterruptedException {
        RBucket<String> bucket = redissonClient.getBucket(prefix + key);
        // trySet = SET NX EX（原子操作）
//        RReadWriteLock rlock = redissonClient.getReadWriteLock(prefix + key);
        // 只有当key不存在（上条已确认）时才设置成功，同时设置过期时间
        boolean result = bucket.setIfAbsent("locked", Duration.of(timeout, timeUnit.toChronoUnit()));
        if (result) {
            // 加锁成功，创建或重置 CountDownLatch 用于等待应答
            String latchKey = latchPrefix + key;
            countDownLatch = redissonClient.getCountDownLatch(latchKey);
            // 如果 latch 已存在，先删除再创建（确保是新的）
            if (countDownLatch.getCount() > 0) {
                countDownLatch.delete();
                countDownLatch = redissonClient.getCountDownLatch(latchKey);
            }
            // 设置初始值为1，等待解锁时 countDown
            countDownLatch.trySetCount(1);
            log.info("mqttNx加锁成功，key: {}, latchKey: {}", prefix + key, latchKey);
        } else {
            log.info("mqttNx加锁失败（key已存在），key: {}", prefix + key);
        }
        return result;
    }

    /**
     * 解锁：收到ACK后删除key，并清除"待确认"标记，同时唤醒等待的线程
     */
    public void unlock() {
        pendingSend = false;
//        RReadWriteLock rlock = redissonClient.getReadWriteLock(prefix + key);
//        if (rlock.writeLock() != null && rlock.writeLock().isHeldByCurrentThread()){
//            rlock.writeLock().unlock();
//        }
        RBucket<String> bucket = redissonClient.getBucket(prefix + key);
        boolean deleted = bucket.delete();
        
        // 唤醒等待的线程
        if (countDownLatch != null) {
            try {
                long remaining = countDownLatch.getCount();
                if (remaining > 0) {
                    countDownLatch.countDown();
                    log.info("mqttNx唤醒等待线程，key: {}, latchKey: {}, remaining: {}", 
                            prefix + key, latchPrefix + key, remaining - 1);
                }
            } catch (Exception e) {
                log.warn("mqttNx唤醒等待线程失败，key: {}, error: {}", prefix + key, e.getMessage());
            }
        }
        
        if (deleted) {
            log.info("mqttNx解锁成功，key: {}", prefix + key);
        } else {
            log.warn("mqttNx解锁失败（key不存在或已过期），key: {}", prefix + key);
        }
    }
    
    /**
     * 等待解锁信号（类似 Condition.await），支持超时
     * 在 tryLock 成功并发送消息后调用，等待 acceptCallback 的 unlock 或超时
     */
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        if (countDownLatch == null) {
            log.warn("mqttNx await 失败，countDownLatch 为 null，key: {}", prefix + key);
            return false;
        }
        String latchKey = latchPrefix + key;
        log.debug("mqttNx开始等待，key: {}, latchKey: {}, timeout: {} {}", 
                prefix + key, latchKey, timeout, unit);
        try {
            boolean result = countDownLatch.await(timeout, unit);
            if (result) {
                log.debug("mqttNx等待完成（收到解锁信号），key: {}, latchKey: {}", 
                        prefix + key, latchKey);
            } else {
                log.warn("mqttNx等待超时，key: {}, latchKey: {}, timeout: {} {}", 
                        prefix + key, latchKey, timeout, unit);
            }
            return result;
        } catch (InterruptedException e) {
            log.warn("mqttNx等待被中断，key: {}, latchKey: {}", prefix + key, latchKey);
            throw e;
        }
    }

    /** 标记当前有一次发送在等待 ACK（在 tryLock 成功并发送后调用） */
    public void markPendingSend() {
        this.pendingSend = true;
    }

    /** 清除“待确认”标记 */
    public void clearPendingSend() {
        this.pendingSend = false;
    }

    /** 是否曾有一次发送尚未收到 ACK（用于判断 tryLock 再次成功是否为超时释放） */
    public boolean wasPendingSend() {
        return pendingSend;
    }
    
    /** 获取超时时间 */
    public Long getTimeout() {
        return timeout;
    }
    
    /** 获取超时时间单位 */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

}
