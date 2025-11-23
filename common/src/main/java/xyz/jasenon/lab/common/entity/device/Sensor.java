package xyz.jasenon.lab.common.entity.device;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class Sensor extends Device {
    public Sensor() {
        this.deviceType = DeviceType.Sensor;
    }

    /**
     * 传感器地址
     */
    private Integer address;

    /**
     * rs485网关id
     */
    private Long rs485GatewayId;

}
