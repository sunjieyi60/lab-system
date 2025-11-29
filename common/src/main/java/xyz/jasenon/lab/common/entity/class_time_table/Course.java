package xyz.jasenon.lab.common.entity.class_time_table;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.experimental.Accessors;


@Getter
@Setter
@Accessors(chain = true)
@TableName("course")
public class Course extends BaseEntity {

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 课程容量
     */
    private Integer volumn;

    /**
     * 课程年级
     */
    private String grade;

}
