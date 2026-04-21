package xyz.jasenon.lab.service.dto.course;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.class_time_table.WeekType;

import java.time.LocalTime;
import java.util.List;

/**
 * 编辑课程表（全量修改）。
 *
 * @author Jasenon_ce
 * @date 2026/03/20
 */
@Getter
@Setter
@Accessors(chain = true)
public class EditCourseSchedule {

    /**
     * 课程表ID
     */
    @NotNull(message = "课程表ID不能为空")
    private Long courseScheduleId;

    /**
     * 课程名称（用于查找或自动创建课程）
     */
    @NotNull(message = "课程名称不能为空")
    private String courseName;

    /**
     * 任课教师ID
     */
    @NotNull(message = "教师ID不能为空")
    private Long teacherId;

    /**
     * 上课周类型
     */
    @NotNull(message = "weekType不能为空")
    private WeekType weekType;

    /**
     * 开始周
     */
    @NotNull(message = "startWeek不能为空")
    private Integer startWeek;

    /**
     * 结束周
     */
    @NotNull(message = "endWeek不能为空")
    private Integer endWeek;

    /**
     * 开始时间
     */
    @NotNull(message = "startTime不能为空")
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "endTime不能为空")
    private LocalTime endTime;

    /**
     * 上课星期 1..7，周一到周日
     */
    @NotEmpty(message = "weekdays不能为空")
    private List<Integer> weekdays;

    /**
     * 起始节数
     */
    @NotNull(message = "startSection不能为空")
    private Integer startSection;

    /**
     * 结束节数
     */
    @NotNull(message = "endSection不能为空")
    private Integer endSection;

    /**
     * 备注信息
     */
    private String mark;

}
