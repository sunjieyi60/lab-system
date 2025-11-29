package xyz.jasenon.lab.common.entity.class_time_table;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.experimental.Accessors;

@Setter
@Getter
@TableName("teacher")
@Accessors(chain = true)
public class Teacher extends BaseEntity {

    /**
     * 教师姓名
     */
    private String teacherName;

}
