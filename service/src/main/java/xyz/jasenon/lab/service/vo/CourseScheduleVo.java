package xyz.jasenon.lab.service.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.class_time_table.WeekType;

import java.time.LocalTime;
import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/29
 */
@Getter
@Setter
@Accessors(chain = true)
public class CourseScheduleVo {

    /**
     * 课程表ID
     */
    private Long id;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 教师ID
     */
    private Long teacherId;

    /**
     * 教师名称
     */
    private String teacherName;

    /**
     * 部门id
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 学期名称
     */
    private String semesterName;

    /**
     * 实验室ID
     */
    private Long laboratoryId;

    /**
     * 实验室名称
     */
    private String laboratoryName;

    /**
     * 周类型
     */
    private WeekType weekType;

    /**
     * 开始周
     */
    private Integer startWeek;

    /**
     * 结束周
     */
    private Integer endWeek;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 星期几
     */
    private List<Integer> weekdays;

    /**
     * 开始节次
     */
    private Integer startSection;

    /**
     * 结束节次
     */
    private Integer endSection;

    /**
     * 备注信息
     */
    private String mark;

}
