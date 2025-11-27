package xyz.jasenon.lab.service.dto.dept;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Getter
@Setter
public class DeleteDept {

    /**
     * 部门ID
     */
    @NotNull(message = "部门ID不能为空")
    private Long deptId;

}
