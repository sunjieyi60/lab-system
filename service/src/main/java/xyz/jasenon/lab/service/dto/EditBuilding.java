package xyz.jasenon.lab.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Getter
@Setter
public class EditBuilding {

    /**
     * 楼栋id
     */
    @NotNull(message = "楼栋ID不能为空")
    private Long buildingId;

    /**
     * 楼名
     */
    @NotBlank(message = "楼名不能为空")
    private String buildingName;

}
