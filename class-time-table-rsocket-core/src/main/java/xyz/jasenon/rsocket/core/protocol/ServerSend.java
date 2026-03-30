package xyz.jasenon.rsocket.core.protocol;

/**
 * @author Jasenon_ce
 * @date 2026/3/22
 */
public interface ServerSend<T extends Message> {

    Command command();

    default void init(T t){
        t.setCommand(command());
        t.setTimestamp(System.currentTimeMillis());
    }
}
