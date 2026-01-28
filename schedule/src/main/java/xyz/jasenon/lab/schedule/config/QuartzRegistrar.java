package xyz.jasenon.lab.schedule.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.schedule.job.TaskGroupJob;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuartzRegistrar implements InitializingBean {

    private final Scheduler scheduler;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void afterPropertiesSet() throws Exception {
        List<TaskGroupRow> groups = jdbcTemplate.query(
                "select id, cron_expr, misfire_policy from task_group where enable=1",
                (rs, i) -> new TaskGroupRow(rs.getLong("id"), rs.getString("cron_expr"), rs.getString("misfire_policy")));
        for (TaskGroupRow g : groups) {
            scheduleGroup(g);
        }
    }

    private void scheduleGroup(TaskGroupRow g) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(TaskGroupJob.class)
                .withIdentity("taskGroupJob-" + g.id, "demo")
                .usingJobData("groupId", g.id)
                .build();

        CronScheduleBuilder cron = CronScheduleBuilder.cronSchedule(g.cron);
        cron = applyMisfire(cron, g.misfirePolicy);

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("taskGroupTrigger-" + g.id, "demo")
                .withSchedule(cron)
                .build();

        if (scheduler.checkExists(jobDetail.getKey())) {
            scheduler.deleteJob(jobDetail.getKey());
        }
        scheduler.scheduleJob(jobDetail, trigger);
        log.info("Scheduled task group {} with cron {}", g.id, g.cron);
    }

    private CronScheduleBuilder applyMisfire(CronScheduleBuilder cron, String policy) {
        if (policy == null) {
            return cron;
        }
        return switch (policy) {
            case "DO_NOTHING" -> cron.withMisfireHandlingInstructionDoNothing();
            case "FIRE_AND_PROCEED" -> cron.withMisfireHandlingInstructionFireAndProceed();
            default -> cron; // SMART_POLICY by default
        };
    }

    @Data
    @AllArgsConstructor
    static class TaskGroupRow {
        private Long id;
        private String cron;
        private String misfirePolicy;
    }
}


