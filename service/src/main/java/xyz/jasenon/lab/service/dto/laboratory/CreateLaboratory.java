package xyz.jasenon.lab.service.dto.laboratory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
@Accessors(chain = true)
public class CreateLaboratory {

    @NotBlank(message = "实验室ID不能为空")
    @Pattern(regexp = "^\\d+-\\d+", message = "实验室ID格式不正确，应为'楼栋号-房间号'")
    private String laboratoryId;

    @NotBlank(message = "实验室名称不能为空")
    private String laboratoryName;

    @NotEmpty(message = "所属部门ID不能为空")
    private List<Long> belongToDeptIds;

    @NotNull(message = "楼栋ID不能为空")
    private Long belongToBuilding;

    @NotBlank(message = "安全等级不能为空")
    private String securityLevel;

    @NotNull(message = "容纳人数不能为空")
    private Integer classCapacity;

    @NotNull(message = "面积不能为空")
    private Integer area;

    private String intro;

    private String username;

    private String phone;

}
