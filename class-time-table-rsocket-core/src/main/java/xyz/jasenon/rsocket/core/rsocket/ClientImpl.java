package xyz.jasenon.rsocket.core.rsocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Status;
import xyz.jasenon.rsocket.core.rsocket.strategy.SendStrategyManager;

import java.time.Instant;
import java.util.Map;

/**
 * 客户端实现
 * 
 * 负责客户端向服务端发送消息
 * 使用策略模式处理不同类型的消息发送
 */
@Slf4j
@Component
public class ClientImpl implements Client {

    private RSocketRequester requester;
    
    @Autowired
    private SendStrategyManager strategyManager;

    /**
     * 设置 RSocket 连接
     */
    @Override
    public void setRequester(RSocketRequester requester) {
        this.requester = requester;
    }

    /**
     * 发送消息到服务端
     * 
     * @param message 消息（包含 route、payload 等）
     * @return 响应消息
     */
    @Override
    public Mono<Message<?>> send(Message<?> message) {
        if (requester == null) {
            return Mono.error(new IllegalStateException("RSocket 连接未建立"));
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
     * 检查连接是否建立
     */
    @Override
    public boolean isConnected() {
        return requester != null && requester.rsocket() != null && !requester.rsocket().isDisposed();
    }

    /**
     * 关闭连接
     */
    @Override
    public void disconnect() {
        if (requester != null) {
            requester.rsocket().dispose();
            requester = null;
            log.info("客户端连接已关闭");
        }
    }
}
