package xyz.jasenon.lab.service.dto;

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
@Accessors(fluent = true)
public class EditLaboratory {

    /**
     * 实验室ID
     */
    @NotNull
    private Long id;

    /**
     * 实验室编号
     */
    @Pattern(regexp = "^\\d+-\\d+", message = "实验室编号格式不正确")
    private String laboratoryId;

    /**
     * 实验室名称
     */
    private String laboratoryName;

    /**
     * 实验室所属部门
     */
    private List<Long> belongToDeptIds;

    /**
     * 实验室所属楼栋
     */
    private Long belongToBuilding;

    /**
     * 实验室安全等级
     */
    private String securityLevel;

    /**
     * 实验室容纳班级数量
     */
    private Integer classCapacity;

    /**
     * 实验室面积（平方米）
     */
    private Integer area;

}
