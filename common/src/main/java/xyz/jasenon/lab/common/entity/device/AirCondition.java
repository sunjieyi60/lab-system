package xyz.jasenon.lab.common.entity.device;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.UUID;

@Getter
@Setter
@Accessors(chain = true)
@TableName("device")
public class AirCondition extends Device {
    public AirCondition() {
        this.deviceType = DeviceType.AirCondition;
    }

    /**
     * 空调地址
     */
    private Integer address;

    /**
     * 地址下空调编号 
     */
    private Integer selfId;

    /**
     * rs485网关id
     */
    private Long rs485GatewayId;

    /**
     * socket网关id
     */
    private Long socketGatewayId;

    /**
     * 机组id
     */
    private String groupId = UUID.randomUUID().toString();

    /**
     * 是否锁定
     */
    private Boolean isLock;
}
