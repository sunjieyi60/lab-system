package xyz.jasenon.rsocket.core.rsocket;

import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;

/**
 * 客户端接口
 * 
 * 客户端向服务端发送消息
 */
public interface Client {

    /**
     * 设置 RSocket 连接
     * 
     * @param requester RSocket 连接
     */
    void setRequester(RSocketRequester requester);

    /**
     * 发送消息到服务端
     * 
     * @param message 消息（包含 route、payload 等）
     * @return 响应消息
     */
    Mono<Message<?>> send(Message<?> message);

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
}
