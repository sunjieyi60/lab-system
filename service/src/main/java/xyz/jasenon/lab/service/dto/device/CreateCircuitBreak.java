package xyz.jasenon.lab.service.dto.device;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class CreateCircuitBreak extends CreateDevice {

    /**
     * 电路断路器地址
     */
    @NotNull(message = "电路断路器地址不能为空")
    private Integer address;

    /**
     * rs485网关id
     */
    @NotNull(message = "rs485网关id不能为空")
    private Long rs485GatewayId;

}
