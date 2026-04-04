package xyz.jasenon.lab.service.quartz.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotBlank(message = "任务id不能为空")
    private String id;
    /**
     * 任务名称
     */
    @NotBlank(message = "任务")
    private String taskName;
    /**
     * cron表达式
     */
    @NotBlank(message = "定时任务")
    private String cron;
    /**
     * 是否启用
     */
    private Boolean enable = true;
    /**
     * 开始日期，结束日期
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDate startDate;
    @NotNull(message = "开始时间不能为空")
    private LocalDate endDate;
    /**
     * 实验室id
     */
    @NotNull(message = "实验室id")
    private Long laboratoryId;

}
