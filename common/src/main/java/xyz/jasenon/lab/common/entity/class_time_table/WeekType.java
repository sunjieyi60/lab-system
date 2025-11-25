package xyz.jasenon.lab.common.entity.class_time_table;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * @author Jasenon_ce
 * @date 2025/9/8
 */
@Getter
public enum WeekType {
    Single(0,"单周"),
    Double(1,"双周"),
    Both(2,"单周以及双周");

    @EnumValue
    private final Integer value;

    private final String description;
    WeekType(Integer value, String desc) {
        this.value = value;
        this.description = desc;
    }
}
