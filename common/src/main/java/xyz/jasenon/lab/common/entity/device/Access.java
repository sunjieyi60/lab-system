package xyz.jasenon.lab.common.entity.device;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("device")
public class Access extends Device {
    public Access() {
        this.deviceType = DeviceType.Access;
    }

    /**
     * 门禁地址
     */
    private Integer address;

    /**
     * rs485网关ID
     */
    private Long rs485GatewayId;

    /**
     * 是否锁定
     */
    private Boolean isLock;

}
