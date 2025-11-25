package xyz.jasenon.lab.common.entity.class_time_table;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.BaseEntity;

@Setter
@Getter
public class Teacher extends BaseEntity {

    /**
     * 教师姓名
     */
    private String teacherName;

}
