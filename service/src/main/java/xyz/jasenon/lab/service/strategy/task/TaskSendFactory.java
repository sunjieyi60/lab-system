package xyz.jasenon.lab.service.strategy.task;

import xyz.jasenon.lab.common.command.SendType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
public class TaskSendFactory {

    private static final Map<SendType,TaskSendStrategy> STRATEGY = new ConcurrentHashMap<>();

    public static void register(SendType type, TaskSendStrategy strategy) {
        STRATEGY.put(type, strategy);
    }

    public static TaskSendStrategy getStrategy(SendType type) {
        return STRATEGY.get(type);
    }

}
