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
public class CreateRS485Gateway {

    /**
     * 网关名称
     */
    @NotBlank
    private String gatewayName;

    /**
     * 发送主题
     */
    @NotBlank
    private String sendTopic;

    /**
     * 接收主题
     */
    @NotBlank
    private String acceptTopic;

    /**
     * 所属实验室ID
     */
    @NotNull
    private Long belongToLaboratoryId;

}
