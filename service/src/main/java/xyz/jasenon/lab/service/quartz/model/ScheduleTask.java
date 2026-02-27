package xyz.jasenon.lab.service.quartz.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Getter
@Setter
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
    private Boolean enable;

    /**
     * 开始日期，结束日期
     */
    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * 实验室id
     */
    private Long laboratoryId;

}
