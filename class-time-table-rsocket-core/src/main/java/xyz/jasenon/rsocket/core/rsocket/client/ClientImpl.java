package xyz.jasenon.rsocket.core.rsocket.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.rsocket.strategy.SendStrategyManager;

import java.time.Instant;

/**
 * 客户端实现
 * 
 * 负责客户端向服务端发送消息
 * 使用策略模式处理不同类型的消息发送
 * 支持类型安全的 MessageAdaptor 调用
 */
@Slf4j
@Component
public class ClientImpl implements Client {

    private RSocketRequester requester;
    
    @Autowired
    private SendStrategyManager strategyManager;

    // ==================== 连接管理 ====================

    /**
     * 设置 RSocket 连接
     */
    @Override
    public void setRequester(RSocketRequester requester) {
        this.requester = requester;
        log.info("客户端 RSocket 连接已设置");
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

    // ==================== 发送方法 ====================

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

        log.debug("客户端发送消息: route={}", route);
        
        // 使用策略管理器执行发送
        return strategyManager.send(message, requester, route)
                .doOnSuccess(resp -> log.debug("收到服务端响应: route={}, status={}", route, resp.getStatus()))
                .onErrorResume(e -> {
                    log.error("向服务端发送消息失败: route={}", route, e);
                    return Mono.error(e);
                });
    }

    // ==================== Fire-and-Forget 方法 ====================

    /**
     * Fire-and-Forget 发送（不等待响应）
     */
    @Override
    public Mono<Void> sendAndForget(Message<?> message) {
        if (requester == null) {
            log.warn("RSocket 连接未建立，FireAndForget 消息丢弃");
            return Mono.empty(); // FireAndForget 不报错
        }

        String route = message.getRoute();
        if (route == null || route.isEmpty()) {
            log.warn("消息 route 为空，FireAndForget 消息丢弃");
            return Mono.empty();
        }

        message.setTimestamp(Instant.now());
        message.setType(Message.Type.FIRE_AND_FORGET);

        log.debug("客户端 FireAndForget 发送: route={}", route);
        
        return strategyManager.sendAndForget(message, requester, route)
                .doOnSuccess(v -> log.debug("客户端 FireAndForget 发送成功: route={}", route))
                .onErrorResume(e -> {
                    log.error("客户端 FireAndForget 发送失败: route={}", route, e);
                    return Mono.empty(); // FireAndForget 不传播错误
                });
    }
}
