package xyz.jasenon.rsocket.core.rsocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Status;
import xyz.jasenon.rsocket.core.rsocket.strategy.SendStrategyManager;

import java.time.Instant;
import java.util.Map;

/**
 * 服务端实现
 * 
 * 负责服务端向客户端发送消息
 * 使用策略模式处理不同类型的消息发送
 */
@Slf4j
@Component
public class ServerImpl implements Server {

    @Autowired
    private ConnectionManager connectionManager;
    
    @Autowired
    private SendStrategyManager strategyManager;

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

        // 使用策略管理器执行发送
        return strategyManager.send(message, requester, route);
    }

    /**
     * 向指定设备发送消息（通过设备ID）
     * 
     * @param deviceDbId 设备ID
     * @param message 消息
     * @return 响应消息
     */
    @Override
    public Mono<Message<?>> sendToDevice(Long deviceDbId, Message<?> message) {
        RSocketRequester requester = connectionManager.getRequester(deviceDbId);
        if (requester == null) {
            return Mono.error(new IllegalStateException("设备 " + deviceDbId + " 不在线"));
        }
        message.setTo(deviceDbId);
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
        return Flux.fromIterable(connectionManager.getAllConnections().entrySet())
                .flatMap(entry -> send(message, entry.getValue())
                        .map(resp -> 1)
                        .onErrorReturn(0))
                .reduce(Integer::sum)
                .doOnSuccess(count -> log.info("广播消息成功发送给 {} 个设备", count));
    }
}
