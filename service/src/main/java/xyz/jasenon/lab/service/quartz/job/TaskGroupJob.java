package xyz.jasenon.lab.service.quartz.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.service.quartz.service.TaskRuntimeService;

/**
 * @author Jasenon_ce
 * @date 2026/1/6
 */
@Component
@Slf4j
public class TaskGroupJob extends QuartzJobBean {

    @Autowired
    private TaskRuntimeService taskRuntimeService;

    @Override
    protected void executeInternal(@NonNull JobExecutionContext context) throws JobExecutionException {
        String taskId = context.getMergedJobDataMap().getString("taskId");
        if (taskId == null) {
            log.warn("taskId is missing");
            return;
        }
        taskRuntimeService.submit(taskId);
    }
}
