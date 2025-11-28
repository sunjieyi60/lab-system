package xyz.jasenon.lab.service.strategy.task;

import org.springframework.util.Assert;
import xyz.jasenon.lab.common.dto.task.Task;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
public class TaskDispatch {

    public static void dispatch(Task task){
        TaskSendStrategy taskSendStrategy = TaskSendFactory.getStrategy(task.getCommandLine().getCommand().getSendType());
        Assert.notNull(taskSendStrategy,"未找到合适的发送策略");
        taskSendStrategy.send(task);
    }

}
