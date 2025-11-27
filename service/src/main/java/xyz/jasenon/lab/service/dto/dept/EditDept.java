package xyz.jasenon.lab.service.dto.dept;

import jakarta.validation.constraints.NotBlank;
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
public class EditDept {

    /**
     * 部门ID
     */
    @NotNull(message = "部门ID不能为空")
    private Long deptId;

    /**
     * 部门名称
     */
    @NotBlank(message = "部门名称不能为空")
    private String deptName;

}
