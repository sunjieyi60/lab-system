package xyz.jasenon.rsocket.core.rsocket.client.handler;

import lombok.extern.slf4j.Slf4j;
import xyz.jasenon.rsocket.core.protocol.Command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class HandlerManager {

    private final static Map<Command, Handler> FACTORY = new ConcurrentHashMap<>();

    public static void register(Handler handler){
        if (handler == null){
            log.warn("非法的处理器注册");
            return;
        }
        FACTORY.put(handler.command(),handler);
        log.info("Command:{} 处理器注册成功 {}", handler.command(),handler);
    }

    public static Handler get(Command command){
        return FACTORY.getOrDefault(command, null);
    }

}
