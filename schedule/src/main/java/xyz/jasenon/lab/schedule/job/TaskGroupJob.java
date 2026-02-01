package xyz.jasenon.lab.schedule.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.schedule.service.TaskRuntimeService;

@Component
@Slf4j
public class TaskGroupJob extends QuartzJobBean {

    @Autowired
    private TaskRuntimeService taskRuntimeService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long groupId = context.getMergedJobDataMap().getLong("groupId");
        if (groupId == null) {
            log.warn("TaskGroupJob missing groupId");
            return;
        }
        taskRuntimeService.runOnce(groupId);
    }
}




