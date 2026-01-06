package xyz.jasenon.lab.service.quartz.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.service.quartz.service.TaskRuntimeService;

/**
 * @author Jasenon_ce
 * @date 2026/1/6
 */
@Component
public class TaskGroupJob extends QuartzJobBean {

    @Autowired
    private TaskRuntimeService taskRuntimeService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {

    }
}
