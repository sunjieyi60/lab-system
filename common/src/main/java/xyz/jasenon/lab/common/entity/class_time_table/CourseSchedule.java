package xyz.jasenon.lab.common.entity.class_time_table;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import com.baomidou.mybatisplus.annotation.TableName;

@Getter
@Setter
@Accessors(chain = true)
@TableName("course_schedule")
public class CourseSchedule extends Schedule {

    /**
     * 课程id
     */
    private Long courseId;

    /**
     * 任课教师id
     */
    private Long teacherId;

    /**
     * 开课部门id
     */
    private Long deptId;

    /**
     * 课程开始节
     */
    private Integer startSection;

    /**
     * 课程结束节
     */
    private Integer endSection;

    /**
     * 课程备注
     */
    private String mark;

}
