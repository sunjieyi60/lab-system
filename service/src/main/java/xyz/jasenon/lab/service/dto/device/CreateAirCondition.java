package xyz.jasenon.lab.service.dto.device;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class CreateAirCondition extends CreateDevice {

    /**
     * 空调地址
     */
    @NotNull(message = "空调地址不能为空")
    private Integer address;

    /**
     * 空调编号
     */
    @NotNull(message = "空调编号不能为空")
    private Integer selfId;

    /**
     * rs485网关id 两个网关至少要填其中一个
     */
    private Long rs485GatewayId;

    /**
     * socket网关id 两个网关至少要填其中一个
     */
    private Long socketGatewayId;

    /**
     * 机组id 机组id不填则随机生成
     */
    private String groupId;

}
