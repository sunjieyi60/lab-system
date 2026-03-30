package xyz.jasenon.rsocket.core.rsocket;

import com.alibaba.fastjson2.JSON;
import io.rsocket.util.ByteBufPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.ServerSend;
import xyz.jasenon.rsocket.core.protocol.Status;

import java.nio.ByteBuffer;


/**
 * 服务端实现
 * 
 * 负责服务端向客户端发送消息
 * 使用 command 标识消息类型（server -> client）
 * 直接通过底层 RSocket 发送，绕过 Spring 的 @MessageMapping 路由
 */
@Slf4j
@Component
public class ServerImpl implements Server {

    @Autowired
    private AbstractConnectionManager connectionManager;

    // ==================== 请求-响应 ====================

    @Override
    public Mono<Message> send(ServerSend message, RSocketRequester requester) {
        if (requester == null) {
            return Mono.error(new IllegalStateException("RSocket 连接为空"));
        }

        Command command = message.command();
        if (command == null) {
            return Mono.error(new IllegalArgumentException("消息 command 不能为空"));
        }

        // 设置时间戳和 command
        if (message instanceof Message) {
            ((Message) message).setTimestamp(System.currentTimeMillis());
            ((Message) message).setCommand(command);
        }

        log.debug("向设备发送请求-响应消息: command={}", command);
        
        // 直接通过底层 RSocket 发送 JSON 数据
        return requester.rsocket()
                .requestResponse(ByteBufPayload.create(JSON.toJSONBytes(message)))
                .map(payload -> {
                    try {
                        ByteBuffer buffer = payload.getData();
                        byte[] bytes = byteBufferToBytes(buffer);
                        payload.release();
                        return JSON.parseObject(bytes, Message.class);
                    } catch (Exception e) {
                        payload.release();
                        throw new RuntimeException("解析响应失败", e);
                    }
                })
                .doOnSuccess(resp -> log.debug("收到设备响应: command={}, status={}", 
                        command, resp.getStatus()))
                .onErrorResume(e -> {
                    log.error("向设备发送消息失败: command={}", command, e);
                    return Mono.just(createErrorResponse(command, e.getMessage()));
                });
    }

    @Override
    public Mono<Message> sendTo(String deviceId, ServerSend message) {
        RSocketRequester requester = connectionManager.getRequester(deviceId);
        if (requester == null) {
            log.warn("设备 {} 不在线，无法发送消息", deviceId);
            return Mono.error(new IllegalStateException("设备 " + deviceId + " 不在线"));
        }
        if (message instanceof Message) {
            ((Message) message).setTo(deviceId);
        }
        return send(message, requester);
    }

    @Override
    public Mono<Integer> broadcast(ServerSend message) {
        if (message instanceof Message) {
            ((Message) message).setTimestamp(System.currentTimeMillis());
        }
        
        var connections = connectionManager.getAllConnections();
        if (connections.isEmpty()) {
            log.warn("没有在线设备，广播消息取消");
            return Mono.just(0);
        }

        log.info("开始广播消息给 {} 个设备", connections.size());
        
        return Flux.fromIterable(connections.entrySet())
                .flatMap(entry -> send(message, entry.getValue())
                        .map(resp -> 1)
                        .onErrorResume(e -> {
                            log.error("向设备 {} 发送消息失败", entry.getKey(), e);
                            return Mono.just(0);
                        }))
                .reduce(Integer::sum)
                .doOnSuccess(count -> log.info("广播消息成功发送给 {} 个设备", count));
    }

    // ==================== Fire-and-Forget ====================

    @Override
    public Mono<Void> sendAndForget(ServerSend message, RSocketRequester requester) {
        if (requester == null) {
            return Mono.error(new IllegalStateException("RSocket 连接为空"));
        }

        Command command = message.command();
        if (command == null) {
            return Mono.error(new IllegalArgumentException("消息 command 不能为空"));
        }

        if (message instanceof Message) {
            ((Message) message).setTimestamp(System.currentTimeMillis());
            ((Message) message).setCommand(command);
        }

        log.debug("FireAndForget 发送消息: command={}", command);
        
        // 直接通过底层 RSocket 发送
        return requester.rsocket()
                .fireAndForget(ByteBufPayload.create(JSON.toJSONBytes(message)))
                .doOnSuccess(v -> log.debug("FireAndForget 消息发送成功: command={}", command))
                .onErrorResume(e -> {
                    log.error("FireAndForget 消息发送失败: command={}", command, e);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> sendAndForgetTo(String deviceId, ServerSend message) {
        RSocketRequester requester = connectionManager.getRequester(deviceId);
        if (requester == null) {
            log.warn("设备 {} 不在线，FireAndForget 消息丢弃", deviceId);
            return Mono.empty();
        }
        if (message instanceof Message) {
            ((Message) message).setTo(deviceId);
        }
        return sendAndForget(message, requester);
    }

    @Override
    public Mono<Integer> broadcastAndForget(ServerSend message) {
        if (message instanceof Message) {
            ((Message) message).setTimestamp(System.currentTimeMillis());
        }
        
        var connections = connectionManager.getAllConnections();
        if (connections.isEmpty()) {
            return Mono.just(0);
        }

        return Flux.fromIterable(connections.entrySet())
                .flatMap(entry -> sendAndForget(message, entry.getValue())
                        .then(Mono.just(1))
                        .onErrorResume(e -> Mono.just(0)))
                .reduce(Integer::sum)
                .doOnSuccess(count -> log.info("FireAndForget 广播成功发送给 {} 个设备", count));
    }

    // ==================== 工具方法 ====================

    /**
     * 将 ByteBuffer 转换为 byte[]（支持 Direct ByteBuffer）
     */
    private byte[] byteBufferToBytes(ByteBuffer buffer) {
        if (buffer.hasArray()) {
            return buffer.array();
        } else {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return bytes;
        }
    }

    /**
     * 创建错误响应
     */
    private Message createErrorResponse(Command command, String errorMessage) {
        Message response = new Message();
        response.setCommand(command);
        response.setStatus(Status.C10001);
        response.setError(errorMessage);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
}
