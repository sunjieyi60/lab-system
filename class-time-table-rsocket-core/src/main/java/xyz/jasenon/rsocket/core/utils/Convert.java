package xyz.jasenon.rsocket.core.utils;

import com.alibaba.fastjson2.JSON;
import io.rsocket.Payload;
import io.rsocket.util.ByteBufPayload;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;

import java.nio.ByteBuffer;

/**
 * Message 类型转换工具类
 * 
 * 提供类型安全的 Message 转换方法
 */
public final class Convert {

    private Convert() {
        // 工具类，禁止实例化
    }

    /**
     * 将 Message<?> 转换为类型安全的 Message<R>
     * 
     * @param message 原始消息
     * @param responseType 目标响应类型
     * @return 类型安全的消息
     * @param <R> 响应数据类型
     */
    @SuppressWarnings("unchecked")
    public static <R> Message<R> castResponse(Message<?> message, Class<R> responseType) {
        if (message == null) {
            return null;
        }
        
        Message<R> typedMessage = new Message<>();
        typedMessage.setRoute(message.getRoute());
        typedMessage.setType(message.getType());
        typedMessage.setFrom(message.getFrom());
        typedMessage.setTo(message.getTo());
        typedMessage.setStatus(message.getStatus());
        typedMessage.setTimestamp(message.getTimestamp());

        Object data = message.getData();
        if (responseType.isInstance(data)) {
            typedMessage.setData(responseType.cast(data));
        } else {
            typedMessage.setData((R) data);
        }
        
        return typedMessage;
    }

    /**
     * 将 Message<?> 转换为类型安全的 Message<R>（使用 MessageAdaptor 获取响应类型）
     * 
     * @param message 原始消息
     * @param adaptor 消息适配器（提供响应类型）
     * @return 类型安全的消息
     * @param <T> 请求数据类型
     * @param <R> 响应数据类型
     */
    public static <T, R> Message<R> castResponse(Message<?> message, MessageAdaptor<T, R> adaptor) {
        return castResponse(message, adaptor.getResponseType());
    }

    public static Payload castPayload(Message<?> message){
        byte[] buf = message.toByte();
        Payload payload = ByteBufPayload.create(buf);
        return payload;
    }

    public static Message<?> castMessage(Payload payload){
        ByteBuffer buf = payload.getData();
        Message<?> message = JSON.parseObject(buf
                .array(), Message.class);
        return message;
    }
}
