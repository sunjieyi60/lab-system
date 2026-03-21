package xyz.jasenon.rsocket.core.protocol;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import lombok.*;

import java.time.Instant;

/**
 * RSocket 消息包装类
 * 
 * 支持泛型类型安全的序列化/反序列化
 * 使用 Jackson 的多态类型机制保留 data 的具体类型信息
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "dataType",
    visible = true
)
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
    private String from;
    /**
     * 发向
     */
    private String to;
    /**
     * 业务负载（泛型，类型安全）
     * 
     * 使用 @JsonTypeInfo 注解保留类型信息，反序列化时不会变成 LinkedHashMap
     */
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class",
        visible = false
    )
    private T data;
    /**
     * data 的类型标识（用于反序列化时确定类型）
     */
    private String dataType;
    /**
     * 指令类型
     */
    private Command command;
    /**
     * 消息状态
     */
    private Status status;
    /**
     * 时间戳
     */
    private Instant timestamp;

    /**
     * 获取数据并转换为指定类型（类型安全包装方法）
     * 
     * @param type 目标类型
     * @return 类型安全的数据，如果类型不匹配返回null
     */
    public <R> R getDataAs(Class<R> type) {
        if (data == null) {
            return null;
        }
        if (type.isInstance(data)) {
            return type.cast(data);
        }
        return null;
    }

    /**
     * 设置数据同时记录类型信息
     */
    public void setData(T data) {
        this.data = data;
        if (data != null) {
            this.dataType = data.getClass().getName();
        }
    }

    public byte[] toByte(){
        return JSON.toJSONBytes(this);
    }

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
