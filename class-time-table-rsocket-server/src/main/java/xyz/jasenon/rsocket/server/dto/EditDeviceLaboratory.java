package xyz.jasenon.rsocket.server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 管理端修改班牌关联实验室：uuid 与 laboratoryId 同一请求体
 */
@Getter
@Setter
public class EditDeviceLaboratory {

    @NotBlank(message = "设备 uuid 不能为空")
    private String uuid;

    /**
     * 关联实验室主键，可为 null 表示解除绑定（与库表 laboratory_id 可空一致）
     */
    private Long laboratoryId;
}
