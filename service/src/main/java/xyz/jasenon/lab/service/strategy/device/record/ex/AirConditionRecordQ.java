package xyz.jasenon.lab.service.strategy.device.record.ex;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.record.AirConditionRecord;
import xyz.jasenon.lab.service.mapper.record.AirConditionRecordMapper;
import xyz.jasenon.lab.service.strategy.device.record.DeviceRecordQ;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Component
public class AirConditionRecordQ extends DeviceRecordQ<AirConditionRecordMapper, AirConditionRecord> {
    public AirConditionRecordQ(AirConditionRecordMapper recordMapper, RedissonClient client) {
        super(recordMapper, DeviceType.AirCondition, client);
        register();
    }

    @Override
    protected LambdaQueryWrapper<AirConditionRecord> buildQueryWrapper(Long deviceId) {
        return new LambdaQueryWrapper<AirConditionRecord>()
                .eq(AirConditionRecord::getDeviceId, deviceId)
                .orderByDesc(AirConditionRecord::getId)
                .last("LIMIT 1");
    }
}
