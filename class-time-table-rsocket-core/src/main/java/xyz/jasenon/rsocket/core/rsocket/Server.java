package xyz.jasenon.rsocket.core.rsocket;

import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;
import xyz.jasenon.rsocket.core.utils.Convert;

/**
 * 服务端接口
 * 
 * 服务端向客户端发送消息
 * 支持类型安全的 MessageAdaptor 调用方式
 */
public interface Server {

    // ==================== 基础方法（无类型安全） ====================

    /**
     * 向指定设备发送消息
     * 
     * @param message 消息（包含 route、payload 等）
     * @param requester RSocket 连接
     * @return 响应消息
     */
    Mono<Message<?>> send(Message<?> message, RSocketRequester requester);

    /**
     * 向指定设备发送消息（通过设备ID）
     * 
     * @param deviceId 设备ID
     * @param message 消息
     * @return 响应消息
     */
    Mono<Message<?>> sendTo(String deviceId, Message<?> message);

    /**
     * 广播消息给所有在线设备
     * 
     * @param message 消息
     * @return 发送成功的设备数量
     */
    Mono<Integer> broadcast(Message<?> message);

    // ==================== 类型安全方法（使用 MessageAdaptor） ====================

    /**
     * 向指定设备发送消息（类型安全版本）
     * 
     * @param adaptor 消息适配器（包含请求和响应类型）
     * @param requester RSocket 连接
     * @return 类型安全的响应消息
     */
    default <T, R> Mono<Message<R>> send(MessageAdaptor<T, R> adaptor, RSocketRequester requester) {
        Message<T> message = adaptor.adaptor();
        return send(message, requester)
                .map(msg -> Convert.castResponse(msg, adaptor));
    }

    /**
     * 向指定设备发送消息（通过设备ID，类型安全版本）
     * 
     * @param deviceId 设备ID
     * @param adaptor 消息适配器
     * @return 类型安全的响应消息
     */
    default <T, R> Mono<Message<R>> sendTo(String deviceId, MessageAdaptor<T, R> adaptor) {
        Message<T> message = adaptor.adaptor();
        return sendTo(deviceId, message)
                .map(msg -> Convert.castResponse(msg, adaptor));
    }

    /**
     * 广播消息（类型安全版本）
     * 
     * @param adaptor 消息适配器
     * @return 发送成功的设备数量
     */
    default <T, R> Mono<Integer> broadcast(MessageAdaptor<T, R> adaptor) {
        return broadcast(adaptor.adaptor());
    }

    // ==================== Fire-and-Forget 方法（无返回值） ====================

    /**
     * Fire-and-Forget 发送（不等待响应）
     * 
     * @param message 消息
     * @param requester RSocket 连接
     * @return Mono<Void>
     */
    Mono<Void> sendAndForget(Message<?> message, RSocketRequester requester);

    /**
     * Fire-and-Forget 发送（通过 MessageAdaptor）
     * 
     * @param adaptor 消息适配器
     * @param requester RSocket 连接
     * @return Mono<Void>
     */
    default Mono<Void> sendAndForget(MessageAdaptor<?, ?> adaptor, RSocketRequester requester) {
        return sendAndForget(adaptor.adaptor(), requester);
    }

    /**
     * Fire-and-Forget 发送到指定设备
     * 
     * @param deviceId 设备ID
     * @param message 消息
     * @return Mono<Void>
     */
    Mono<Void> sendAndForgetTo(String deviceId, Message<?> message);

    /**
     * Fire-and-Forget 发送到指定设备（通过 MessageAdaptor）
     * 
     * @param deviceId 设备ID
     * @param adaptor 消息适配器
     * @return Mono<Void>
     */
    default Mono<Void> sendAndForgetTo(String deviceId, MessageAdaptor<?, ?> adaptor) {
        return sendAndForgetTo(deviceId, adaptor.adaptor());
    }

    /**
     * Fire-and-Forget 广播
     * 
     * @param message 消息
     * @return 发送成功的设备数量
     */
    Mono<Integer> broadcastAndForget(Message<?> message);

    /**
     * Fire-and-Forget 广播（通过 MessageAdaptor）
     * 
     * @param adaptor 消息适配器
     * @return 发送成功的设备数量
     */
    default Mono<Integer> broadcastAndForget(MessageAdaptor<?, ?> adaptor) {
        return broadcastAndForget(adaptor.adaptor());
    }
}
