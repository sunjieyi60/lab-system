package xyz.jasenon.lab.mqtt.setnx;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
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
    private Long timeout = 500L;
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    /** 是否有未确认的发送（用于检测超时释放：tryLock 再次成功且此前未 unlock 则视为超时） */
    private volatile boolean pendingSend = false;

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
    public boolean tryLock() {
        RBucket<String> bucket = redissonClient.getBucket(prefix + key);
        // trySet = SET NX EX（原子操作）
        // 只有当key不存在（上条已确认）时才设置成功，同时设置过期时间
        boolean result = bucket.setIfAbsent("locked", Duration.of(timeout, timeUnit.toChronoUnit()));
        return result;
    }

    /**
     * 解锁：收到ACK后删除key，并清除“待确认”标记
     */
    public void unlock() {
        pendingSend = false;
        redissonClient.getBucket(prefix + key).delete();
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

}
