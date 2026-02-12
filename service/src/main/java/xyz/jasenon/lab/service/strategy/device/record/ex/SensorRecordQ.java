package xyz.jasenon.lab.service.strategy.device.record.ex;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.record.SensorRecord;
import xyz.jasenon.lab.service.mapper.record.SensorRecordMapper;
import xyz.jasenon.lab.service.strategy.device.record.DeviceRecordQ;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Component
public class SensorRecordQ extends DeviceRecordQ<SensorRecordMapper, SensorRecord> {
    public SensorRecordQ(SensorRecordMapper recordMapper, RedissonClient client) {
        super(recordMapper, DeviceType.Sensor, client);
        register();
    }

    @Override
    protected LambdaQueryWrapper<SensorRecord> buildQueryWrapper(Long deviceId) {
        return new LambdaQueryWrapper<SensorRecord>()
                .eq(SensorRecord::getDeviceId, deviceId)
                .orderByDesc(SensorRecord::getId)
                .last("LIMIT 1");
    }
}
