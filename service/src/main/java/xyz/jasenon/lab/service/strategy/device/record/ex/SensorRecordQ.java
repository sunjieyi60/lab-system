package xyz.jasenon.lab.service.strategy.device.record.ex;

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
    public SensorRecordQ(SensorRecordMapper recordMapper) {
        super(recordMapper, DeviceType.Sensor);
        register();
    }
}
