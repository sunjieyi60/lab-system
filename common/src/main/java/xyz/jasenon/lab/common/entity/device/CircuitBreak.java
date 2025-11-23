package xyz.jasenon.lab.common.entity.device;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class CircuitBreak extends Device {
    public CircuitBreak() {
        this.deviceType = DeviceType.CircuitBreak;
    }

    /**
     * 电路断路器地址
     */
    private Integer address;

    /**
     * rs485网关id
     */
    private Long rs485GatewayId;

}
