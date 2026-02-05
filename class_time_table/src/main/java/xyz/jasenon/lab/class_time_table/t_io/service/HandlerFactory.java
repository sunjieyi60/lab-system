package xyz.jasenon.lab.class_time_table.t_io.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jasenon_ce
 * @date 2026/2/4
 */
public class HandlerFactory {

    private final static Map<Byte, Handler> FACTORIES = new ConcurrentHashMap<>();

    public static void register(Byte cmdType, Handler handler){
        FACTORIES.put(cmdType, handler);
    }

    public static Handler get(Byte cmdType){
        if (FACTORIES.containsKey(cmdType)){
            return FACTORIES.get(cmdType);
        }
        throw new IllegalArgumentException("未找到对应的处理器");
    }

}
