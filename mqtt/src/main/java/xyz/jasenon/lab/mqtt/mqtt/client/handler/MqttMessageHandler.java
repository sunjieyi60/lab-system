package xyz.jasenon.lab.mqtt.mqtt.client.handler;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import xyz.jasenon.lab.common.entity.BaseEntity;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.record.BaseRecord;

@Slf4j
public abstract class MqttMessageHandler<DM extends BaseMapper<D>, RM extends BaseMapper<R>, D extends BaseEntity, R extends BaseRecord> {

    protected final DM deviceMapper;
    protected final RM recordMapper;
    protected final RedissonClient redissonClient;

    public MqttMessageHandler(DM deviceMapper, RM recordMapper, RedissonClient redissonClient) {
        this.deviceMapper = deviceMapper;
        this.recordMapper = recordMapper;
        this.redissonClient = redissonClient;
    }

    public void handle(byte[] payloads, Long rs485Id, DeviceType deviceType) {
        if (!verify(payloads)) {
            log.warn("数据校验失败, 设备类型: {}, 数据: {}", deviceType, payloads);
            return;
        }
        D device = parse(payloads, rs485Id);
        Assert.notNull(device, "设备数据解析失败, 未知的设备");
        if (!needSave(payloads)) {
            return;
        }
        R record = decryptPayload(payloads, rs485Id);
        RBucket<R> recordSpace = redissonClient.getBucket(deviceType.getRedisPrefix() + device.getId());
        recordSpace.set(record);
        recordMapper.insert(record);
    }

    abstract R decryptPayload(byte[] payload, Long rs485Id);

    abstract D parse(byte[] payload, Long rs485Id);

    abstract boolean needSave(byte[] payload);

    abstract boolean verify(byte[] payload);

}
