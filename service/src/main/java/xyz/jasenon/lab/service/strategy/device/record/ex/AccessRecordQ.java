package xyz.jasenon.lab.service.strategy.device.record.ex;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.record.AccessRecord;
import xyz.jasenon.lab.service.mapper.record.AccessRecordMapper;
import xyz.jasenon.lab.service.strategy.device.record.DeviceRecordQ;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Component
public class AccessRecordQ extends DeviceRecordQ<AccessRecordMapper, AccessRecord> {
    public AccessRecordQ(AccessRecordMapper recordMapper, RedissonClient client) {
        super(recordMapper, DeviceType.Access, client);
        register();
    }

    @Override
    protected LambdaQueryWrapper<AccessRecord> buildQueryWrapper(Long deviceId) {
        return new LambdaQueryWrapper<AccessRecord>()
                .eq(AccessRecord::getDeviceId, deviceId)
                .orderByDesc(AccessRecord::getId)
                .last("LIMIT 1");
    }
}
