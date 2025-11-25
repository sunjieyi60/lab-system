package xyz.jasenon.lab.common.entity.device;

import lombok.Getter;

public enum DeviceType {

    AirCondition(0x1F,0x28,"AirCondition:Record:"),
    CircuitBreak(0x0B,0X1E,"CircuitBreak:Record:"),
    Light(0x29,0X3C,"Light:Record:"),
    Sensor(0x3D,0X50,"Sensor:Record:"),
    Access(0x01,0x0A,"Access:Record:");

    private final Integer startAddress;
    private final Integer endAddress;
    @Getter
    private final String redisPrefix;

    DeviceType(Integer startAddress, Integer endAddress, String redisPrefix) {
        this.startAddress = startAddress;
        this.endAddress = endAddress;
        this.redisPrefix = redisPrefix;
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
