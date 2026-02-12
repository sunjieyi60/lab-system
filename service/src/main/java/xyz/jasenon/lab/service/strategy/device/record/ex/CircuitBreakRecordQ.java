package xyz.jasenon.lab.service.strategy.device.record.ex;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.record.CircuitBreakRecord;
import xyz.jasenon.lab.service.mapper.record.CircuitBreakRecordMapper;
import xyz.jasenon.lab.service.strategy.device.record.DeviceRecordQ;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Component
public class CircuitBreakRecordQ extends DeviceRecordQ<CircuitBreakRecordMapper, CircuitBreakRecord> {
    public CircuitBreakRecordQ(CircuitBreakRecordMapper recordMapper, RedissonClient client) {
        super(recordMapper, DeviceType.CircuitBreak, client);
        register();
    }

    @Override
    protected LambdaQueryWrapper<CircuitBreakRecord> buildQueryWrapper(Long deviceId) {
        return new LambdaQueryWrapper<CircuitBreakRecord>()
                .eq(CircuitBreakRecord::getDeviceId, deviceId)
                .orderByDesc(CircuitBreakRecord::getId)
                .last("LIMIT 1");
    }
}
