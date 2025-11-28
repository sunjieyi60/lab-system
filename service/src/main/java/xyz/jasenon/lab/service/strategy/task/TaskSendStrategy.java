package xyz.jasenon.lab.service.strategy.task;

import xyz.jasenon.lab.common.dto.task.Task;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
public interface TaskSendStrategy {

    void send(Task task);

}
