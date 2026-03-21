package xyz.jasenon.rsocket.core.rsocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.rsocket.strategy.SendStrategyManager;

import java.time.Instant;

/**
 * 服务端实现
 * 
 * 负责服务端向客户端发送消息
 * 使用策略模式处理不同类型的消息发送
 * 支持类型安全的 MessageAdaptor 调用
 */
@Slf4j
@Component
public class ServerImpl implements Server {

    @Autowired
    private ConnectionManager connectionManager;
    
    @Autowired
    private SendStrategyManager strategyManager;

    // ==================== 基础发送方法 ====================

    /**
     * 向指定设备发送消息
     * 
     * @param message 消息（包含 route、payload 等）
     * @param requester RSocket 连接
     * @return 响应消息
     */
    @Override
    public Mono<Message<?>> send(Message<?> message, RSocketRequester requester) {
        if (requester == null) {
            return Mono.error(new IllegalStateException("RSocket 连接为空"));
        }

        String route = message.getRoute();
        if (route == null || route.isEmpty()) {
            return Mono.error(new IllegalArgumentException("消息 route 不能为空"));
        }

        // 设置时间戳
        message.setTimestamp(Instant.now());

        log.debug("向设备发送消息: route={}", route);
        
        // 使用策略管理器执行发送
        return strategyManager.send(message, requester, route)
                .doOnSuccess(resp -> log.debug("收到设备响应: route={}, status={}", route, resp.getStatus()))
                .onErrorResume(e -> {
                    log.error("向设备发送消息失败: route={}", route, e);
                    return Mono.error(e);
                });
    }

    /**
     * 向指定设备发送消息（通过设备ID）
     * 
     * @param deviceId 设备ID
     * @param message 消息
     * @return 响应消息
     */
    @Override
    public Mono<Message<?>> sendTo(String deviceId, Message<?> message) {
        RSocketRequester requester = connectionManager.getRequester(deviceId);
        if (requester == null) {
            log.warn("设备 {} 不在线，无法发送消息", deviceId);
            return Mono.error(new IllegalStateException("设备 " + deviceId + " 不在线"));
        }
        message.setTo(deviceId);
        return send(message, requester);
    }

    /**
     * 广播消息给所有在线设备
     * 
     * @param message 消息
     * @return 发送成功的设备数量
     */
    @Override
    public Mono<Integer> broadcast(Message<?> message) {
        message.setTimestamp(Instant.now());
        
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

    // ==================== Fire-and-Forget 方法 ====================

    /**
     * Fire-and-Forget 发送（不等待响应）
     */
    @Override
    public Mono<Void> sendAndForget(Message<?> message, RSocketRequester requester) {
        if (requester == null) {
            return Mono.error(new IllegalStateException("RSocket 连接为空"));
        }

        String route = message.getRoute();
        if (route == null || route.isEmpty()) {
            return Mono.error(new IllegalArgumentException("消息 route 不能为空"));
        }

        message.setTimestamp(Instant.now());
        message.setType(Message.Type.FIRE_AND_FORGET);

        log.debug("FireAndForget 发送消息: route={}", route);
        
        return strategyManager.sendAndForget(message, requester, route)
                .doOnSuccess(v -> log.debug("FireAndForget 消息发送成功: route={}", route))
                .onErrorResume(e -> {
                    log.error("FireAndForget 消息发送失败: route={}", route, e);
                    return Mono.empty(); // FireAndForget 不传播错误
                });
    }

    /**
     * Fire-and-Forget 发送到指定设备
     */
    @Override
    public Mono<Void> sendAndForgetTo(String deviceId, Message<?> message) {
        RSocketRequester requester = connectionManager.getRequester(deviceId);
        if (requester == null) {
            log.warn("设备 {} 不在线，FireAndForget 消息丢弃", deviceId);
            return Mono.empty(); // FireAndForget 不报错
        }
        message.setTo(deviceId);
        return sendAndForget(message, requester);
    }

    /**
     * Fire-and-Forget 广播
     */
    @Override
    public Mono<Integer> broadcastAndForget(Message<?> message) {
        message.setTimestamp(Instant.now());
        message.setType(Message.Type.FIRE_AND_FORGET);
        
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
}
