package xyz.jasenon.lab.common.entity.class_time_table;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;

@Setter
@Getter
@TableName("teacher")
public class Teacher extends BaseEntity {

    /**
     * 教师姓名
     */
    private String teacherName;

}
