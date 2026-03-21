package xyz.jasenon.rsocket.core.utils;

import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Status;

import java.time.Instant;

/**
 * 响应 Message 构造工具类
 * 
 * 用于在 RSocket Controller 中快速构造响应消息
 * 
 * 使用示例：
 * <pre>
 * return Mono.just(MessageResponse.success(responseData));
 * 
 * return Mono.just(MessageResponse.error(Status.C10001, "注册失败"));
 * </pre>
 */
public final class MessageResponse {

    private MessageResponse() {
        // 工具类，禁止实例化
    }

    /**
     * 构造成功响应
     * 
     * @param data 业务数据
     * @return Message 响应
     * @param <T> 数据类型
     */
    public static <T> Message<T> success(T data) {
        return Message.<T>builder()
                .type(Message.Type.REQUEST_RESPONSE)
                .data(data)
                .status(Status.C10000)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 构造成功响应（自定义状态）
     * 
     * @param data 业务数据
     * @param status 自定义状态
     * @return Message 响应
     * @param <T> 数据类型
     */
    public static <T> Message<T> success(T data, Status status) {
        return Message.<T>builder()
                .type(Message.Type.REQUEST_RESPONSE)
                .data(data)
                .status(status)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 构造错误响应
     * 
     * @param status 错误状态
     * @param errorMessage 错误信息
     * @return Message 响应
     * @param <T> 数据类型
     */
    public static <T> Message<T> error(Status status, String errorMessage) {
        return Message.<T>builder()
                .type(Message.Type.REQUEST_RESPONSE)
                .data(null)
                .status(status)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 构造错误响应（带部分数据）
     * 
     * @param data 部分数据（可能为 null）
     * @param status 错误状态
     * @return Message 响应
     * @param <T> 数据类型
     */
    public static <T> Message<T> error(T data, Status status) {
        return Message.<T>builder()
                .type(Message.Type.REQUEST_RESPONSE)
                .data(data)
                .status(status)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 构造 Fire-and-Forget 确认响应（空响应）
     * 
     * @return Message 响应
     */
    public static Message<Void> ack() {
        return Message.<Void>builder()
                .type(Message.Type.FIRE_AND_FORGET)
                .status(Status.C10000)
                .timestamp(Instant.now())
                .build();
    }
}
