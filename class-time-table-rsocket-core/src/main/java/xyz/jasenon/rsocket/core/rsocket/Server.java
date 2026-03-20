package xyz.jasenon.rsocket.core.rsocket;

import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;

/**
 * 服务端接口
 * 
 * 服务端向客户端发送消息
 */
public interface Server {

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
     * @param deviceDbId 设备ID
     * @param message 消息
     * @return 响应消息
     */
    Mono<Message<?>> sendToDevice(Long deviceDbId, Message<?> message);

    /**
     * 广播消息给所有在线设备
     * 
     * @param message 消息
     * @return 发送成功的设备数量
     */
    Mono<Integer> broadcast(Message<?> message);
}
