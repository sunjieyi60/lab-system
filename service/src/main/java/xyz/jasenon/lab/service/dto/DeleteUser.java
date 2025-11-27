package xyz.jasenon.lab.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Getter
@Setter
@Accessors(fluent = true)
public class DeleteUser {

    /**
     * 用户id
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

}
