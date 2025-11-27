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
public class CreateCourseSchedule extends CreateSchedule {

    /**
     * 课程ID
     */
    @NotNull
    private Long courseId;

    /**
     * 教师ID
     */
    @NotNull
    private Long teacherId;

    /**
     * 所属部门id
     */
    @NotNull
    private Long belongToDeptId;

    /**
     * 起始节数
     */
    @NotNull
    private Integer startSection;

    /**
     * 结束节数
     */
    @NotNull
    private Integer endSection;

    /**
     * 备注信息
     */
    @NotNull
    private String mark;

}
