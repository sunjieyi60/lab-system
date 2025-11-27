package xyz.jasenon.lab.service.dto.dept;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Getter
@Setter
public class CreateDept {

    /**
     * 部门名称
     */
    @NotBlank(message = "部门名称不能为空")
    private String deptName;

}
