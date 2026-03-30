package xyz.jasenon.rsocket.core;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.model.ClassTimeTable;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.ServerSend;
import xyz.jasenon.rsocket.core.rsocket.AbstractConnectionManager;
import xyz.jasenon.rsocket.core.rsocket.Server;

/**
 * API 门面类
 * 
 * 提供统一的消息发送接口：
 * - Server -> Client: 使用 command 标识
 * - Client -> Server: 使用 route 标识
 */
@Component
@RequiredArgsConstructor
public class Api {

    private final AbstractConnectionManager connectionManager;
    private final Server server;

    /**
     * 绑定班牌设备到连接
     */
    public void bindClassTimeTable(ClassTimeTable classTimeTable, RSocketRequester requester) {
        connectionManager.register(classTimeTable.unique(), requester);
    }

    /**
     * 获取设备的 Requester
     */
    public RSocketRequester getRequesterByUnique(ClassTimeTable classTimeTable) {
        return connectionManager.getRequester(classTimeTable.unique());
    }

    // ==================== Server -> Client（使用 command）====================

    /**
     * 向指定设备发送消息并等待响应
     * 
     * @param message 消息（实现 ServerSend 接口，使用 command）
     * @param requester RSocket 连接
     */
    public Mono<Message> sendToClient(ServerSend message, RSocketRequester requester) {
        return server.send(message, requester);
    }

    /**
     * 向指定设备发送消息（通过设备ID）
     */
    public Mono<Message> sendToClient(String deviceId, ServerSend message) {
        return server.sendTo(deviceId, message);
    }

    /**
     * Fire-and-Forget 发送到客户端
     */
    public Mono<Void> sendAndForgetToClient(ServerSend message, RSocketRequester requester) {
        return server.sendAndForget(message, requester);
    }

    /**
     * Fire-and-Forget 发送到指定设备
     */
    public Mono<Void> sendAndForgetToClient(String deviceId, ServerSend message) {
        return server.sendAndForgetTo(deviceId, message);
    }

    /**
     * 广播消息给所有在线设备
     */
    public Mono<Integer> broadcastToClients(ServerSend message) {
        return server.broadcast(message);
    }

    // ==================== Client -> Server（使用 route）====================

    // 客户端发送功能通过 Client 接口实现
}
