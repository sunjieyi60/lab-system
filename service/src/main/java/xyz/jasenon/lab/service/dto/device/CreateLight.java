package xyz.jasenon.lab.service.dto.device;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class CreateLight extends CreateDevice {

    /**
     * 灯光地址
     */
    @NotNull(message = "灯光地址不能为空")
    private Integer address;

    /**
     * 灯光编号
     */
    @NotNull(message = "灯光编号不能为空")
    private Integer selfId;

    /**
     * rs485网关id
     */
    @NotNull(message = "rs485网关id不能为空")
    private Long rs485GatewayId;

}
