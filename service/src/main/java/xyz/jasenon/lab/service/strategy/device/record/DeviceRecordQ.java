package xyz.jasenon.lab.service.strategy.device.record;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.record.BaseRecord;
import xyz.jasenon.lab.service.vo.DeviceRecordVo;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Slf4j
public abstract class DeviceRecordQ<RM extends BaseMapper<R>, R extends BaseRecord> {

    private final RM recordMapper;
    private final DeviceType deviceType;

    public DeviceRecordQ(RM recordMapper, DeviceType deviceType) {
        this.recordMapper = recordMapper;
        this.deviceType = deviceType;
    }

    protected void register(){
        DeviceRecordFactory.registerDeviceRecordMethod(deviceType,this);
        log.info("register device record method:{}",deviceType);
    }

    public DeviceRecordVo<R> getRecord(Long deviceId){
        var r = recordMapper.selectOne(
                new LambdaQueryWrapper<R>().eq(BaseRecord::getDeviceId,deviceId)
                        .orderByDesc(BaseRecord::getId)
                        .last("limit 1")
        );
        DeviceRecordVo<R> vo = new DeviceRecordVo<>();
        vo.setData(r);
        vo.setDeviceType(deviceType);
        return vo;
    }

}
