package xyz.jasenon.rsocket.core.rsocket.client;

import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;
import xyz.jasenon.rsocket.core.utils.Convert;

/**
 * 客户端接口
 * 
 * 客户端向服务端发送消息
 * 支持类型安全的 MessageAdaptor 调用方式
 */
public interface Client {

    Mono<Void> connect();

    // ==================== 连接管理 ====================
    /**
     * 设置 RSocket 连接
     * 
     * @param requester RSocket 连接
     */
    void setRequester(RSocketRequester requester);

    /**
     * 检查连接是否建立
     * 
     * @return 是否已连接
     */
    boolean isConnected();

    /**
     * 关闭连接
     */
    void disconnect();

    // ==================== 基础发送方法（无类型安全） ====================

    /**
     * 发送消息到服务端
     * 
     * @param message 消息（包含 route、payload 等）
     * @return 响应消息
     */
    Mono<Message<?>> send(Message<?> message);

    // ==================== 类型安全方法（使用 MessageAdaptor） ====================

    /**
     * 发送消息到服务端（类型安全版本）
     * 
     * @param adaptor 消息适配器（包含请求和响应类型）
     * @return 类型安全的响应消息
     */
    default <T, R> Mono<Message<R>> send(MessageAdaptor<T, R> adaptor) {
        Message<T> message = adaptor.adaptor();
        return send(message)
                .map(msg -> Convert.castResponse(msg, adaptor));
    }

    // ==================== Fire-and-Forget 方法（无返回值） ====================

    /**
     * Fire-and-Forget 发送（不等待响应）
     * 
     * @param message 消息
     * @return Mono<Void>
     */
    Mono<Void> sendAndForget(Message<?> message);

    /**
     * Fire-and-Forget 发送（通过 MessageAdaptor）
     * 
     * @param adaptor 消息适配器
     * @return Mono<Void>
     */
    default Mono<Void> sendAndForget(MessageAdaptor<?, ?> adaptor) {
        return sendAndForget(adaptor.adaptor());
    }
}
