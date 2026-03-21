package xyz.jasenon.rsocket.core.rsocket.strategy;

import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;
import xyz.jasenon.rsocket.core.utils.Convert;

/**
 * 消息发送策略接口
 * 
 * 基于不同的 RSocket 交互模型实现具体的发送策略：
 * - Request-Response：请求-响应模式
 * - Fire-and-Forget：单向发送模式
 * - Request-Stream：请求流模式
 * - Request-Channel：双向流模式
 */
public interface SendStrategy {

    /**
     * 获取策略支持的消息类型
     * @return 消息类型
     */
    Message.Type getType();

    /**
     * 发送消息（基础方法）
     * @param message 消息对象
     * @param requester RSocket 请求器
     * @param route 路由地址
     * @return 包含响应消息的 Mono（FireAndForget 返回 Mono.empty()）
     */
    default Mono<Message<?>> send(Message<?> message, RSocketRequester requester, String route) {
        throw new RuntimeException("not impl method");
    }

    /**
     * 发送消息（类型安全版本）
     * 
     * @param message 请求消息
     * @param responseType 响应数据类型
     * @param requester RSocket 请求器
     * @param route 路由地址
     * @return 包含类型安全响应的 Mono
     */
    default <R> Mono<Message<R>> send(Message<?> message, Class<R> responseType, 
                                       RSocketRequester requester, String route) {
        return send(message, requester, route)
                .map(msg -> Convert.castResponse(msg, responseType));
    }

    /**
     * 发送消息（通过 MessageAdaptor，自动类型匹配）
     * 
     * @param adaptor 消息适配器（包含请求和响应类型信息）
     * @param requester RSocket 请求器
     * @return 包含类型安全响应的 Mono
     */
    default <T, R> Mono<Message<R>> send(MessageAdaptor<T, R> adaptor, RSocketRequester requester) {
        Message<T> message = adaptor.adaptor();
        return send(message, requester, message.getRoute())
                .map(msg -> Convert.castResponse(msg, adaptor));
    }

    /**
     * Fire-and-Forget 发送（不等待响应）
     */
    default Mono<Void> sendAndForget(Message<?> message, RSocketRequester requester, String route) {
        throw new RuntimeException("not impl method");
    }

    /**
     * Fire-and-Forget 发送（通过 MessageAdaptor）
     */
    default Mono<Void> sendAndForget(MessageAdaptor<?, ?> adaptor, RSocketRequester requester) {
        Message<?> message = adaptor.adaptor();
        return sendAndForget(message, requester, message.getRoute());
    }
}
