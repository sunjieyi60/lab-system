package xyz.jasenon.lab.common.entity.device;

import lombok.Getter;
import xyz.jasenon.lab.common.entity.record.*;

public enum DeviceType {

    AirCondition(0x1F,0x28,"AirCondition:Record:", AirConditionRecord.class),
    CircuitBreak(0x0B,0X1E,"CircuitBreak:Record:", CircuitBreakRecord.class),
    Light(0x29,0X3C,"Light:Record:", LightRecord.class),
    Sensor(0x3D,0X50,"Sensor:Record:", SensorRecord.class),
    Access(0x01,0x0A,"Access:Record:", AccessRecord.class);

    private final Integer startAddress;
    private final Integer endAddress;
    @Getter
    private final String redisPrefix;
    @Getter
    private final Class<? extends BaseRecord> recordClass;

    DeviceType(Integer startAddress, Integer endAddress, String redisPrefix, Class<? extends BaseRecord> recordClass) {
        this.startAddress = startAddress;
        this.endAddress = endAddress;
        this.redisPrefix = redisPrefix;
        this.recordClass = recordClass;
    }

    public static DeviceType parseAddress(Integer address){
        for (DeviceType deviceType : DeviceType.values()) {
            if (address >= deviceType.startAddress && address <= deviceType.endAddress) {
                return deviceType;
            }
        }
        throw new IllegalArgumentException("Invalid address: " + address);
    }

}
