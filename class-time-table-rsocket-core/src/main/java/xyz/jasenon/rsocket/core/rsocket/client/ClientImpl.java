package xyz.jasenon.rsocket.core.rsocket.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.ClientSend;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Status;


/**
 * 客户端实现
 * 
 * 负责客户端向服务端发送消息
 * 直接使用 RSocketRequester，根据方法选择交互模式
 */
@Slf4j
@Component
public class ClientImpl implements Client {

    private RSocketRequester requester;

    // ==================== 连接管理 ====================

    @Override
    public Mono<Void> connect() {
        // 连接逻辑由外部管理，这里只做占位
        return Mono.empty();
    }

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

    // ==================== 请求-响应 ====================

    /**
     * 发送消息到服务端（Request-Response 模式）
     * 
     * @param message 实现 ClientSend 接口的消息（使用 route）
     */
    @Override
    public Mono<Message> send(ClientSend message) {
        if (requester == null) {
            return Mono.error(new IllegalStateException("RSocket 连接未建立"));
        }

        String route = message.route();
        if (route == null || route.isEmpty()) {
            return Mono.error(new IllegalArgumentException("消息 route 不能为空"));
        }

        // 设置时间戳
        if (message instanceof Message) {
            ((Message) message).setTimestamp(System.currentTimeMillis());
        }

        log.debug("客户端发送请求: route={}", route);
        
        return requester.route(route)
                .data(message)
                .retrieveMono(Message.class)
                .doOnSuccess(resp -> log.debug("收到服务端响应: route={}, status={}", route, resp.getStatus()))
                .onErrorResume(e -> {
                    log.error("向服务端发送消息失败: route={}", route, e);
                    return Mono.just(createErrorResponse(e.getMessage()));
                });
    }

    // ==================== Fire-and-Forget ====================

    /**
     * Fire-and-Forget 发送（不等待响应）
     * 
     * @param message 实现 ClientSend 接口的消息（使用 route）
     */
    @Override
    public Mono<Void> sendAndForget(ClientSend message) {
        if (requester == null) {
            log.warn("RSocket 连接未建立，FireAndForget 消息丢弃");
            return Mono.empty();
        }

        String route = message.route();
        if (route == null || route.isEmpty()) {
            log.warn("消息 route 为空，FireAndForget 消息丢弃");
            return Mono.empty();
        }

        // 设置时间戳
        if (message instanceof Message) {
            ((Message) message).setTimestamp(System.currentTimeMillis());
        }

        log.debug("客户端 FireAndForget 发送: route={}", route);
        
        return requester.route(route)
                .data(message)
                .send()
                .doOnSuccess(v -> log.debug("客户端 FireAndForget 发送成功: route={}", route))
                .onErrorResume(e -> {
                    log.error("客户端 FireAndForget 发送失败: route={}", route, e);
                    return Mono.empty();
                });
    }

    /**
     * 创建错误响应
     */
    private Message createErrorResponse(String errorMessage) {
        Message response = new Message();
        response.setStatus(Status.C10001);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
}
