package xyz.jasenon.lab.service.strategy.device.record.ex;

import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.record.LightRecord;
import xyz.jasenon.lab.service.mapper.record.LightRecordMapper;
import xyz.jasenon.lab.service.strategy.device.record.DeviceRecordQ;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Component
public class LightRecordQ extends DeviceRecordQ<LightRecordMapper, LightRecord> {
    public LightRecordQ(LightRecordMapper recordMapper, RedissonClient client) {
        super(recordMapper, DeviceType.Light, client);
        register();
    }
}
