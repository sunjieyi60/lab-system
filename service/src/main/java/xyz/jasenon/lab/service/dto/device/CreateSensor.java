package xyz.jasenon.lab.service.dto.device;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class CreateSensor extends CreateDevice {

    /**
     * 传感器地址
     */
    @NotNull(message = "传感器地址不能为空")
    private Integer address;

    /**
     * 地址下传感器编号
     */
    @NotNull(message = "传感器编号不能为空")
    private Integer selfId;

    /**
     * rs485网关id
     */
    @NotNull(message = "rs485网关id不能为空")
    private Long rs485GatewayId;

}
