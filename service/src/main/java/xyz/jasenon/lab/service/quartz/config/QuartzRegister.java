package xyz.jasenon.lab.service.quartz.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.service.quartz.job.TaskGroupJob;
import xyz.jasenon.lab.service.quartz.model.ScheduleConfigRoot;
import xyz.jasenon.lab.service.quartz.model.ScheduleTask;
import xyz.jasenon.lab.service.quartz.service.ConfigLoader;

import java.util.List;

@Component
@Slf4j
public class QuartzRegister implements InitializingBean {

    private final ConfigLoader configLoader;
    private final Scheduler scheduler;

    public QuartzRegister(ConfigLoader configLoader, Scheduler scheduler) {
        this.configLoader = configLoader;
        this.scheduler = scheduler;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        List<TaskGroupRow> taskGroupRows = configLoader.getAllTasks().stream().map(sc->{
            var row = new TaskGroupRow(sc.getId(),sc.getCron());
            return row;
        }).toList();
        for (TaskGroupRow taskGroupRow : taskGroupRows) {
            scheduleTask(taskGroupRow);
        }
    }

    public void scheduleTask(TaskGroupRow taskGroupRow) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(TaskGroupJob.class)
                .withIdentity("taskGroupJob-"+taskGroupRow.id)
                .usingJobData("taskId",taskGroupRow.id)
                .build();

        CronScheduleBuilder cron = CronScheduleBuilder.cronSchedule(taskGroupRow.cron);
        cron.withMisfireHandlingInstructionFireAndProceed();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("taskGroupTrigger-"+taskGroupRow.id)
                .withSchedule(cron)
                .build();

        if (scheduler.checkExists(jobDetail.getKey())){
            scheduler.deleteJob(jobDetail.getKey());
        }
        scheduler.scheduleJob(jobDetail,trigger);
        log.info("定时任务注册成功:{}",taskGroupRow);
    }

    public void scheduleTask(ScheduleConfigRoot cfg) throws SchedulerException {
        ScheduleTask task = cfg.getTask();
        var $ = new TaskGroupRow(task.getId(),task.getCron());
        scheduleTask($);
    }

    @AllArgsConstructor
    static class TaskGroupRow {
        private String id;
        private String cron;
    }
}
