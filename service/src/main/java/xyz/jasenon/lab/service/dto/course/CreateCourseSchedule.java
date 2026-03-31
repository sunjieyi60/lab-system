package xyz.jasenon.lab.service.dto.course;

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
@Accessors(chain = true)
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
    private Long belongToDeptId;

    /**
     * 专业班级
     */
    private String majorClass;

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
    private String mark;

}
