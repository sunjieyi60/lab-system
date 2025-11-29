package xyz.jasenon.lab.service.dto.gateway;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Getter
@Setter
@Accessors(chain = true)
public class DeleteSocketGateway {

    /**
     * 网关id
     */
    @NotNull
    private Long socketGatewayId;

}
