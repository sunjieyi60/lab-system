package xyz.jasenon.lab.service.quartz.model;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.class_time_table.Schedule;
import xyz.jasenon.lab.common.entity.class_time_table.WeekType;
import xyz.jasenon.lab.service.time_order.TimeOrder;

import java.time.LocalTime;
import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Getter
@Setter
@TableName(value = "time_rule", autoResultMap = true)
public class TimeRule {

    /**
     * id
     */
    private String id;

    /**
     * 任务id
     */
    private String scheduleTaskId;

    /**
     * 学期id
     */
    private Long semesterId;

    /**
     * 星期遮罩
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Integer> weekdays;

    /**
     * 开始周，结束周
     */
    private Integer startWeek;
    private Integer endWeek;

    /**
     * 单双周
     */
    private WeekType weekType;

    /**
     * 开始时间，结束时间
     */
    @TimeOrder(order = 0)
    private LocalTime startTime;
    @TimeOrder(order = 1)
    private LocalTime endTime;

    public TimeRule courseSchedule2TimeRule(Schedule schedule){
        TimeRule timeRule = new TimeRule();
        String id = IdUtil.getSnowflakeNextIdStr();
        timeRule.setId(id);
        timeRule.setSemesterId(schedule.getSemesterId());
        timeRule.setWeekdays(schedule.getWeekdays());
        timeRule.setStartWeek(schedule.getStartWeek());
        timeRule.setEndWeek(schedule.getEndWeek());
        timeRule.setStartTime(schedule.getStartTime());
        timeRule.setEndTime(schedule.getEndTime());
        timeRule.setWeekType(schedule.getWeekType());
        return timeRule;
    }

}
