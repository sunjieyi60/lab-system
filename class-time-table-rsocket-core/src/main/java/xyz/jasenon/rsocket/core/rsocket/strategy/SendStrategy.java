package xyz.jasenon.rsocket.core.rsocket.strategy;

import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;

/**
 * 发送策略接口
 */
public interface SendStrategy {
    
    /**
     * 获取策略类型
     * 
     * @return 消息类型
     */
    Message.Type getType();
    
    /**
     * 执行发送
     * 
     * @param message 消息
     * @param requester RSocket 连接
     * @param route 路由路径
     * @return 响应消息
     */
    Mono<Message<?>> send(Message<?> message, RSocketRequester requester, String route);
}
