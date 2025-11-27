package xyz.jasenon.lab.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Getter
@Setter
@Accessors(fluent = true)
public class CreateBuilding {

    /**
     * 楼名
     */
    @NotBlank(message = "楼名不能为空")
    private String buildingName;

    /**
     * 部门id
     */
    @NotEmpty(message = "部门id不能为空")
    private List<Long> deptIds;

}
