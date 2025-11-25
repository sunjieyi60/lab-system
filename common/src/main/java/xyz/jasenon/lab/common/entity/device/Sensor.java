package xyz.jasenon.lab.common.entity.device;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("device")
public class Sensor extends Device {
    public Sensor() {
        this.deviceType = DeviceType.Sensor;
    }

    /**
     * 传感器地址
     */
    private Integer address;

    /**
     * 地址下传感器编号
     */
    private Integer selfId;

    /**
     * rs485网关id
     */
    private Long rs485GatewayId;

}
