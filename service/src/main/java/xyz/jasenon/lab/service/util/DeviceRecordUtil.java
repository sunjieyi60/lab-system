package xyz.jasenon.lab.service.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.entity.device.Device;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.record.BaseRecord;
import xyz.jasenon.lab.common.entity.record.Origin;
import xyz.jasenon.lab.service.mapper.record.*;
import xyz.jasenon.lab.service.strategy.device.record.DeviceRecordFactory;
import xyz.jasenon.lab.service.vo.DeviceRecordVo;

import java.util.Map;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Component
public class DeviceRecordUtil {

    private final RedissonClient redissonClient;

    public DeviceRecordUtil(RedissonClient redissonClient, AirConditionRecordMapper m1, AccessRecordMapper m2, LightRecordMapper m3, CircuitBreakRecordMapper m4, SensorRecordMapper m5) {
        this.redissonClient = redissonClient;

    }

    public DeviceRecordVo getRecordVo(Device device){
        DeviceType deviceType = device.getDeviceType();
        RBucket<? extends BaseRecord> rBucket = redissonClient.getBucket(deviceType.getRedisPrefix() + device.getId());
        boolean isOnline = rBucket.isExists();
        if (isOnline){
            DeviceRecordVo deviceRecordVo = new DeviceRecordVo();
            var object = rBucket.get();
            object.setOrigin(Origin.Redis);
            deviceRecordVo.setData(object);
            return deviceRecordVo;
        }
        DeviceRecordVo deviceRecordVo = new DeviceRecordVo();
        var object = DeviceRecordFactory.getDeviceRecordMethod(deviceType).getRecord(device.getId());
        object.setOrigin(Origin.MySql);
        deviceRecordVo.setData(object);
        return deviceRecordVo;
    }

}
