package xyz.jasenon.lab.service.dto.gateway;

import jakarta.validation.constraints.NotBlank;
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
public class CreateSocketGateway {

    /**
     * 网关名称
     */
    @NotBlank
    private String gatewayName;

    /**
     * 网关mac地址
     */
    @NotBlank
    private String mac;

    /**
     * 网关所属实验室id
     */
    @NotNull
    private Long belongToLaboratoryId;

}
