package xyz.jasenon.lab.service.quartz.model;

import cn.hutool.core.util.IdUtil;
import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.class_time_table.Schedule;
import xyz.jasenon.lab.common.entity.class_time_table.WeekType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Getter
@Setter
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
    private LocalTime startTime;
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
