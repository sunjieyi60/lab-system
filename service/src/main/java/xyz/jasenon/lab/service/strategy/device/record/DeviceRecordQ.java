package xyz.jasenon.lab.service.strategy.device.record;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.record.BaseRecord;
import xyz.jasenon.lab.common.entity.record.Origin;
import xyz.jasenon.lab.service.vo.DeviceRecordVo;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Slf4j
public abstract class DeviceRecordQ<RM extends BaseMapper<R>, R extends BaseRecord> {

    private final RM recordMapper;
    private final DeviceType deviceType;
    private final RedissonClient redissonClient;

    public DeviceRecordQ(RM recordMapper, DeviceType deviceType, RedissonClient redissonClient) {
        this.recordMapper = recordMapper;
        this.deviceType = deviceType;
        this.redissonClient = redissonClient;
    }

    protected void register(){
        DeviceRecordFactory.registerDeviceRecordMethod(deviceType,this);
        log.info("register device record method:{}",deviceType);
    }

    public DeviceRecordVo<R> getRecord(Long deviceId){
        DeviceType deviceType = this.deviceType;
        RBucket<? extends BaseRecord> rBucket = redissonClient.getBucket(deviceType.getRedisPrefix() + deviceId);
        boolean isOnline = rBucket.isExists();
        if (isOnline){
            DeviceRecordVo deviceRecordVo = new DeviceRecordVo();
            var object = rBucket.get();
            object.setOrigin(Origin.Redis);
            deviceRecordVo.setData(object);
            return deviceRecordVo;
        }
        DeviceRecordVo deviceRecordVo = new DeviceRecordVo();
        var object = recordMapper.selectOne(
                new LambdaQueryWrapper<R>()
                    .eq(BaseRecord::getDeviceId, deviceId)
                    .orderByDesc(BaseRecord::getId)
                    .last("limit 1")
        );
        object.setOrigin(Origin.MySql);
        deviceRecordVo.setData(object);
        return deviceRecordVo;
    }

}
