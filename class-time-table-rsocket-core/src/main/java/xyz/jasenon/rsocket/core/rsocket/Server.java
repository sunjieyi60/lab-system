package xyz.jasenon.rsocket.core.rsocket;

import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.ServerSend;

/**
 * 服务端接口
 * 
 * 服务端向客户端发送消息
 * 使用 command 标识消息类型（server -> client）
 */
public interface Server {

    // ==================== 请求-响应（Request-Response） ====================

    /**
     * 向指定设备发送消息并等待响应
     * 
     * @param message 消息（实现 ServerSend 接口，使用 command 标识）
     * @param requester RSocket 连接
     * @return 响应消息
     */
    Mono<Message> send(ServerSend message, RSocketRequester requester);

    /**
     * 向指定设备发送消息（通过设备ID）
     * 
     * @param deviceId 设备ID
     * @param message 消息（实现 ServerSend 接口）
     * @return 响应消息
     */
    Mono<Message> sendTo(String deviceId, ServerSend message);

    /**
     * 广播消息给所有在线设备
     * 
     * @param message 消息（实现 ServerSend 接口）
     * @return 发送成功的设备数量
     */
    Mono<Integer> broadcast(ServerSend message);

    // ==================== 单向发送（Fire-and-Forget） ====================

    /**
     * Fire-and-Forget 发送（不等待响应）
     * 
     * @param message 消息（实现 ServerSend 接口）
     * @param requester RSocket 连接
     * @return Mono<Void>
     */
    Mono<Void> sendAndForget(ServerSend message, RSocketRequester requester);

    /**
     * Fire-and-Forget 发送到指定设备
     * 
     * @param deviceId 设备ID
     * @param message 消息（实现 ServerSend 接口）
     * @return Mono<Void>
     */
    Mono<Void> sendAndForgetTo(String deviceId, ServerSend message);

    /**
     * Fire-and-Forget 广播
     * 
     * @param message 消息（实现 ServerSend 接口）
     * @return 发送成功的设备数量
     */
    Mono<Integer> broadcastAndForget(ServerSend message);
}
