package xyz.jasenon.lab.service.dto.device;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class CreateAccess extends CreateDevice {

    @NotNull(message = "门禁地址不能为空")
    private Integer address;

    @NotNull(message = "门禁编号不能为空")
    private Integer selfId;

    @NotNull(message = "rs485网关ID不能为空")
    private Long rs485GatewayId;

}
