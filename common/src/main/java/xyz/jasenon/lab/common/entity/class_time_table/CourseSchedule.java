package xyz.jasenon.lab.common.entity.class_time_table;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

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
     * 开课部门名称（非数据库字段，通过 dept_id 关联查询）
     */
    @TableField(exist = false)
    private String deptName;

    /**
     * 专业班级
     */
    private String majorClass;

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
