package xyz.jasenon.lab.class_time_table.rsocket.model;

import lombok.Builder;
import lombok.Data;
import xyz.jasenon.lab.class_time_table.rsocket.protocol.Command;

import java.io.Serializable;
import java.time.Instant;

/**
 * RSocket 通用消息封装
 * 支持请求/响应、推送、流式数据
 */
@Data
@Builder
public class RpcMessage<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息 ID（用于追踪）
     */
    private String messageId;
    
    /**
     * 消息类型
     */
    private MessageType type;
    
    /**
     * 命令/方法名（枚举类型，替代魔法值）
     */
    private Command command;
    
    /**
     * 状态码（枚举类型）
     */
    private StatusCode status;
    
    /**
     * 发送方 UUID（班牌设备或管理端）
     */
    private String fromUuid;
    
    /**
     * 目标 UUID（用于点对点发送）
     */
    private String targetUuid;
    
    /**
     * 时间戳
     */
    private Instant timestamp;
    
    /**
     * 消息体数据
     */
    private T payload;
    
    /**
     * 错误信息（响应时填充）
     */
    private String errorMsg;
    
    public enum MessageType {
        /** 请求-响应 */
        REQUEST_RESPONSE,
        /** 单向通知 */
        FIRE_AND_FORGET,
        /** 请求-流 */
        REQUEST_STREAM,
        /** 双向流 */
        REQUEST_CHANNEL
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return status != null && status.isSuccess();
    }
    
    /**
     * 判断是否客户端错误
     */
    public boolean isClientError() {
        return status != null && status.isClientError();
    }
    
    /**
     * 判断是否服务端错误
     */
    public boolean isServerError() {
        return status != null && status.isServerError();
    }
    
    // ==================== 便捷构造方法 ====================
    
    /**
     * 创建成功响应
     */
    public static <T> RpcMessage<T> success(Command command, T payload) {
        return RpcMessage.<T>builder()
                .messageId(generateMessageId())
                .type(MessageType.REQUEST_RESPONSE)
                .command(command)
                .status(StatusCode.SUCCESS)
                .timestamp(Instant.now())
                .payload(payload)
                .build();
    }
    
    /**
     * 创建成功响应（简化版）
     */
    public static <T> RpcMessage<T> success(Command command) {
        return success(command, null);
    }
    
    /**
     * 创建错误响应
     */
    public static <T> RpcMessage<T> error(Command command, StatusCode status, String errorMsg) {
        return RpcMessage.<T>builder()
                .messageId(generateMessageId())
                .type(MessageType.REQUEST_RESPONSE)
                .command(command)
                .status(status)
                .timestamp(Instant.now())
                .errorMsg(errorMsg)
                .build();
    }
    
    /**
     * 创建错误响应（使用默认 INTERNAL_ERROR）
     */
    public static <T> RpcMessage<T> error(Command command, String errorMsg) {
        return error(command, StatusCode.INTERNAL_ERROR, errorMsg);
    }
    
    /**
     * 创建 Fire-and-Forget 消息
     */
    public static <T> RpcMessage<T> fireAndForget(Command command, T payload) {
        return RpcMessage.<T>builder()
                .messageId(generateMessageId())
                .type(MessageType.FIRE_AND_FORGET)
                .command(command)
                .timestamp(Instant.now())
                .payload(payload)
                .build();
    }
    
    /**
     * 创建流式消息
     */
    public static <T> RpcMessage<T> stream(Command command, T payload) {
        return RpcMessage.<T>builder()
                .messageId(generateMessageId())
                .type(MessageType.REQUEST_STREAM)
                .command(command)
                .timestamp(Instant.now())
                .payload(payload)
                .build();
    }
    
    /**
     * 创建特定状态码的响应
     */
    public static <T> RpcMessage<T> ofStatus(Command command, StatusCode status, T payload) {
        return RpcMessage.<T>builder()
                .messageId(generateMessageId())
                .type(MessageType.REQUEST_RESPONSE)
                .command(command)
                .status(status)
                .timestamp(Instant.now())
                .payload(payload)
                .build();
    }
    
    private static String generateMessageId() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
