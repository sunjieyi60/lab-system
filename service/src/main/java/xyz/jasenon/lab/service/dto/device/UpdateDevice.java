package xyz.jasenon.lab.service.dto.device;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 编辑设备请求：设备名称、是否启用轮询检测。
 * 启用检测：更新为 true 并注册轮询；取消检测：更新为 false 并取消轮询。
 */
@Getter
@Setter
@Accessors(chain = true)
public class UpdateDevice {

    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    /**
     * 设备名称，可选
     */
    private String deviceName;

    /**
     * 是否启用轮询检测，可选；传 true/false 时联动 startPolling / cancelPolling
     */
    private Boolean pollingEnabled;
}
