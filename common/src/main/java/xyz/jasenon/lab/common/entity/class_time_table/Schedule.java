package xyz.jasenon.lab.common.entity.class_time_table;

import java.time.LocalTime;
import java.util.List;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.BaseEntity;

@Getter
@Setter
public class Schedule extends BaseEntity {

    /**
     * 课程id
     */
    private Long semesterId;

    /**
     * 实验室id
     */
    private Long laboratoryId;

    /**
     * 周次类型
     */
    private WeekType weekType;

    /**
     * 起始周
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
     * 上课星期 1,2,3,4,5,6,7 周一至周日
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Integer> weekdays;

}
