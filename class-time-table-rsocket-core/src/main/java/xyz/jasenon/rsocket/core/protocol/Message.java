package xyz.jasenon.rsocket.core.protocol;

import com.alibaba.fastjson2.JSON;
import lombok.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * RSocket 消息基类 - 自有协议
 * 
 * 所有 packet 类继承此类，不再使用泛型设计
 * 解决泛型推断导致 Mono 构造困难的问题
 * 
 * 自有协议设计：
 * - 业务实体（如 RegisterRequest, RegisterResponse）直接继承 Message
 * - 状态包装：使用 Status 枚举（C10000 形式命名）
 * - 错误信息传递：Status.code + Status.msg + error（详细堆栈）
 * - 扩展字段：extras（用于传递 traceId、requestId 等上下文）
 * 
 * 静态工厂方法使用规则：
 * - 带 route 的方法：用于 client 向 server 发送请求
 * - 带 command 的方法：用于 server 向 client 发送请求
 * 
 * @author Jasenon_ce
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    /** 路由（client -> server 时使用） */
    private String route;
    
    /** 消息来自 */
    private String from;
    
    /** 发向 */
    private String to;
    
    /** 指令类型（server -> client 时使用） */
    private Command command;
    
    /** 
     * 业务状态码（来自 Status 枚举，C10000 形式）
     * 10000 - 成功
     * 其他 - 各类错误码（见 Status 枚举）
     */
    @Builder.Default
    private Integer code = Status.C10000.getCode();
    
    /** 
     * 状态描述（来自 Status 枚举的 desc）
     */
    private String desc;
    
    /** 
     * 提示信息（来自 Status 枚举的 msg 或自定义）
     */
    private String msg;
    
    /** 
     * 详细错误信息
     * 用于存放详细错误描述、异常堆栈等
     */
    private String error;
    
    /** 
     * 扩展字段
     * 用于传递额外的上下文信息，如 traceId、requestId 等
     */
    @Builder.Default
    private Map<String, Object> extras = new HashMap<>();
    
    /** 时间戳 */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * 序列化为字节数组
     */
    public byte[] toByte(){
        return JSON.toJSONBytes(this);
    }

    // ==================== Status 包装方法 ====================

    /**
     * 设置状态（使用 Status 枚举）
     */
    public void setStatus(Status status) {
        if (status != null) {
            this.code = status.getCode();
            this.desc = status.getDesc();
            this.msg = status.getMsg();
        }
    }

    /**
     * 设置状态（使用 Status 枚举 + 自定义消息）
     */
    public void setStatus(Status status, String customMsg) {
        if (status != null) {
            this.code = status.getCode();
            this.desc = status.getDesc();
            this.msg = customMsg != null ? customMsg : status.getMsg();
        }
    }

    /**
     * 设置状态（使用 Status 枚举 + 自定义消息 + 详细错误）
     */
    public void setStatus(Status status, String customMsg, String errorDetail) {
        if (status != null) {
            this.code = status.getCode();
            this.desc = status.getDesc();
            this.msg = customMsg != null ? customMsg : status.getMsg();
            this.error = errorDetail;
        }
    }

    /**
     * 获取状态（根据 code 反查 Status 枚举）
     */
    public Status getStatus() {
        return Status.of(this.code);
    }

    /**
     * 判断是否成功（code = 10000）
     */
    public boolean isSuccess() {
        return code != null && code.equals(Status.C10000.getCode());
    }

    /**
     * 判断是否失败
     */
    public boolean isError() {
        return !isSuccess();
    }

    // ==================== 静态工厂方法（client -> server，必须带 route）====================

    /**
     * 创建 client -> server 的成功消息（带 route）
     */
    public static Message success(String route) {
        return Message.builder()
                .route(route)
                .code(Status.C10000.getCode())
                .desc(Status.C10000.getDesc())
                .msg(Status.C10000.getMsg())
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 创建 client -> server 的成功消息（带 route + 自定义消息）
     */
    public static Message success(String route, String customMsg) {
        return Message.builder()
                .route(route)
                .code(Status.C10000.getCode())
                .desc(Status.C10000.getDesc())
                .msg(customMsg)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 创建 client -> server 的错误消息（带 route + Status）
     */
    public static Message error(String route, Status status) {
        return Message.builder()
                .route(route)
                .code(status.getCode())
                .desc(status.getDesc())
                .msg(status.getMsg())
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 创建 client -> server 的错误消息（带 route + Status + 自定义消息）
     */
    public static Message error(String route, Status status, String customMsg) {
        return Message.builder()
                .route(route)
                .code(status.getCode())
                .desc(status.getDesc())
                .msg(customMsg)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 创建 client -> server 的错误消息（带 route + Status + 自定义消息 + 详细错误）
     */
    public static Message error(String route, Status status, String customMsg, String errorDetail) {
        return Message.builder()
                .route(route)
                .code(status.getCode())
                .desc(status.getDesc())
                .msg(customMsg)
                .error(errorDetail)
                .timestamp(Instant.now())
                .build();
    }

    // ==================== 静态工厂方法（server -> client，必须带 command）====================

    /**
     * 创建 server -> client 的成功消息（带 command）
     */
    public static Message success(Command command) {
        return Message.builder()
                .command(command)
                .code(Status.C10000.getCode())
                .desc(Status.C10000.getDesc())
                .msg(Status.C10000.getMsg())
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 创建 server -> client 的成功消息（带 command + 自定义消息）
     */
    public static Message success(Command command, String customMsg) {
        return Message.builder()
                .command(command)
                .code(Status.C10000.getCode())
                .desc(Status.C10000.getDesc())
                .msg(customMsg)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 创建 server -> client 的错误消息（带 command + Status）
     */
    public static Message error(Command command, Status status) {
        return Message.builder()
                .command(command)
                .code(status.getCode())
                .desc(status.getDesc())
                .msg(status.getMsg())
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 创建 server -> client 的错误消息（带 command + Status + 自定义消息）
     */
    public static Message error(Command command, Status status, String customMsg) {
        return Message.builder()
                .command(command)
                .code(status.getCode())
                .desc(status.getDesc())
                .msg(customMsg)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 创建 server -> client 的错误消息（带 command + Status + 自定义消息 + 详细错误）
     */
    public static Message error(Command command, Status status, String customMsg, String errorDetail) {
        return Message.builder()
                .command(command)
                .code(status.getCode())
                .desc(status.getDesc())
                .msg(customMsg)
                .error(errorDetail)
                .timestamp(Instant.now())
                .build();
    }

    // ==================== 从异常创建（需指定 route 或 command）====================

    /**
     * 从异常创建 client -> server 的错误消息（带 route）
     */
    public static Message error(String route, Throwable throwable) {
        return Message.builder()
                .route(route)
                .code(Status.C10001.getCode())
                .desc(Status.C10001.getDesc())
                .msg(throwable.getMessage())
                .error(getStackTrace(throwable))
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 从异常创建 server -> client 的错误消息（带 command）
     */
    public static Message error(Command command, Throwable throwable) {
        return Message.builder()
                .command(command)
                .code(Status.C10001.getCode())
                .desc(Status.C10001.getDesc())
                .msg(throwable.getMessage())
                .error(getStackTrace(throwable))
                .timestamp(Instant.now())
                .build();
    }

    // ==================== 便捷方法 ====================

    /**
     * 添加扩展字段
     */
    public Message addExtra(String key, Object value) {
        if (this.extras == null) {
            this.extras = new HashMap<>();
        }
        this.extras.put(key, value);
        return this;
    }

    /**
     * 获取扩展字段
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtra(String key) {
        if (this.extras == null) {
            return null;
        }
        return (T) this.extras.get(key);
    }

    // ==================== 私有工具方法 ====================

    private static String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element).append("\n");
        }
        // 处理 cause
        Throwable cause = throwable.getCause();
        if (cause != null) {
            sb.append("Caused by: ").append(getStackTrace(cause));
        }
        return sb.toString();
    }

    /**
     * 使用 Status 枚举构造
     */
    public Message(Command command, Status status){
        this.command = command;
        this.code = status.getCode();
        this.desc = status.getDesc();
        this.msg = status.getMsg();
        this.timestamp = Instant.now();
    }

    /**
     * 使用 Status 枚举 + 自定义消息构造
     */
    public Message(Command command, Status status, String msg){
        this.command = command;
        this.code = status.getCode();
        this.desc = status.getDesc();
        this.msg = msg;
        this.timestamp = Instant.now();
    }
}
