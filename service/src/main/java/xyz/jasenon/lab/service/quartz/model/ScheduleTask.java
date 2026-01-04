package xyz.jasenon.lab.service.quartz.model;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
public class ScheduleTask {

    /**
     * 任务id
     */
    private String id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * cron表达式
     */
    private String cron;

    /**
     * 是否启用
     */
    private String enable;

    /**
     * 实验室id
     */
    private Long laboratoryId;

}
