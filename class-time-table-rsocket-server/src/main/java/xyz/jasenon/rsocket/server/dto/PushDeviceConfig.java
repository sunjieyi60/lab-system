package xyz.jasenon.rsocket.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.model.Config;

/**
 * 管理端下发班牌配置：设备 UUID 与配置体同一请求体
 */
@Getter
@Setter
public class PushDeviceConfig {

    @NotBlank(message = "设备 uuid 不能为空")
    private String uuid;

    @NotNull(message = "配置不能为空")
    private Config config;
}
