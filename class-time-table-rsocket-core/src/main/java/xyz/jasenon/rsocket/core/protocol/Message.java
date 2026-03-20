package xyz.jasenon.rsocket.core.protocol;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class Message<T> {
    /**
     * 路由
     */
    private String route;
    /**
     *  消息的类型
     */
    private Type type;
    /**
     * 消息来自
     */
    private Long from;
    /**
     * 发向
     */
    private Long to;
    /**
     * 负载
     */
    private T payload;
    /**
     * 消息状态
     */
    private Status status;
    /**
     * 时间戳
     */
    private Instant timestamp;

    public enum Type {
        /** 请求-响应 */
        REQUEST_RESPONSE,
        /** 单向通知 */
        FIRE_AND_FORGET,
        /** 请求-流 */
        REQUEST_STREAM,
        /** 双向流 */
        REQUEST_CHANNEL
    }

}
