package xyz.jasenon.lab.common.entity.class_time_table;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.BaseEntity;

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
