package xyz.jasenon.rsocket.core.rsocket.client.handler;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import xyz.jasenon.rsocket.core.protocol.Command;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler 管理器
 * 
 * 自动注册所有 Handler，提供命令路由功能
 */
@Slf4j
@Component
public class HandlerManager {

    private static final Map<Command, Handler> HANDLER_MAP = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private List<Handler> handlers;

    /**
     * 自动注册所有 Handler
     */
    @PostConstruct
    public void init() {
        if (handlers != null) {
            for (Handler handler : handlers) {
                register(handler);
            }
        }
        log.info("HandlerManager 初始化完成，共注册 {} 个处理器", HANDLER_MAP.size());
    }

    /**
     * 注册 Handler
     */
    public static void register(Handler handler) {
        if (handler == null) {
            log.warn("尝试注册空的 Handler");
            return;
        }
        Command command = handler.command();
        if (command == null) {
            log.warn("Handler {} 返回的 Command 为空", handler.getClass().getName());
            return;
        }
        HANDLER_MAP.put(command, handler);
        log.info("Handler 注册成功: {} -> {}", command, handler.getClass().getSimpleName());
    }

    /**
     * 获取 Handler
     */
    public static Handler get(Command command) {
        return HANDLER_MAP.get(command);
    }

    /**
     * 检查是否支持该命令
     */
    public static boolean supports(Command command) {
        return HANDLER_MAP.containsKey(command);
    }

    /**
     * 获取所有已注册的命令
     */
    public static Map<Command, Handler> getAllHandlers() {
        return new ConcurrentHashMap<>(HANDLER_MAP);
    }
}
