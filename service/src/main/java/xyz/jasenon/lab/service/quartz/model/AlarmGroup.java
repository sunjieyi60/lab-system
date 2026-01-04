package xyz.jasenon.lab.service.quartz.model;

import com.baomidou.mybatisplus.annotation.TableField;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
public class AlarmGroup {

    /**
     * 报警组ID
     */
    private String id;

    /**
     * 报警组名称
     */
    private String name;

    /**
     * 任务ID
     */
    private String scheduleTaskId;

    /**
     * 报警列表
     */
    @TableField(exist = false)
    private List<Alarm> alarms;

}
