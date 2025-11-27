package xyz.jasenon.lab.mqtt.setnx;

import lombok.Getter;
import lombok.SneakyThrows;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MqttNx {

    @Getter
    private final String key = UUID.randomUUID().toString();
    private final RedissonClient redissonClient;
    private final String prefix = "mqttnx:";
    private Long timeout = 500L;
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    
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

    @SneakyThrows
    public boolean tryLock(){
        RLock rlock = redissonClient.getSpinLock(prefix + key);
        return rlock.tryLock(timeout, timeUnit);
    }

    public void unlock(){
        RLock rlock = redissonClient.getSpinLock(prefix + key);
        rlock.unlock();
    }

}
