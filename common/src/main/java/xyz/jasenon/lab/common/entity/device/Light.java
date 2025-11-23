package xyz.jasenon.lab.common.entity.device;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class Light extends Device {
    public Light() {
        this.deviceType = DeviceType.Light;
    }

    /**
     * 灯地址
     */
    private Integer address;

    /**
     * 地址下灯编号
     */
    private Integer selfId;

    /**
     * rs485网关ID
     */
    private Long rs485GatewayId;

    /**
     * 是否锁定
     */
    private Boolean isLock;
}
