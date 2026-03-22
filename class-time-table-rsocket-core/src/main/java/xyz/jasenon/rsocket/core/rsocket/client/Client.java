package xyz.jasenon.rsocket.core.rsocket.client;

import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.ClientSend;
import xyz.jasenon.rsocket.core.protocol.Message;

/**
 * 客户端接口
 * 
 * 客户端向服务端发送消息
 * 使用 route 标识消息类型（client -> server）
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

    // ==================== 请求-响应（Request-Response） ====================

    /**
     * 发送消息到服务端并等待响应
     * 
     * @param message 消息（实现 ClientSend 接口，使用 route 标识）
     * @return 响应消息
     */
    Mono<Message> send(ClientSend message);

    // ==================== 单向发送（Fire-and-Forget） ====================

    /**
     * Fire-and-Forget 发送（不等待响应）
     * 
     * @param message 消息（实现 ClientSend 接口）
     * @return Mono<Void>
     */
    Mono<Void> sendAndForget(ClientSend message);
}
