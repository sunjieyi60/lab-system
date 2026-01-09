package xyz.jasenon.lab.service.quartz.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Getter
@Setter
public class ScheduleConfigRoot {

    /**
     * 任务主体
     */
    private ScheduleTask task;

    /**
     * 动作组
     */
    private List<ActionGroup> actionGroups;

    /**
     * 数据组
     */
    private List<Data> dataGroup;

    /**
     * 条件组
     */
    private List<ConditionGroup> conditionGroups;

    /**
     * 时间规则
     */
    private TimeRule timeRule;

    /**
     * 报警配置
     */
    private List<Alarm> alarmGroup;

    /**
     * 看门狗配置
     */
    private WatchDog watchDog;

}
