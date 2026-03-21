package xyz.jasenon.rsocket.core.rsocket.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;

/**
 * Fire-and-Forget 发送策略
 * 
 * 单向发送，不等待响应，返回 Mono<Void>
 */
@Slf4j
public class FireAndForgetStrategy implements SendStrategy {

    @Override
    public Message.Type getType() {
        return Message.Type.FIRE_AND_FORGET;
    }

    @Override
    public Mono<Message<?>> send(Message<?> message, RSocketRequester requester, String route) {
        // FireAndForget 不返回实际消息，调用 sendAndForget 后返回 empty
        return sendAndForget(message, requester, route)
                .then(Mono.empty());
    }

    @Override
    public Mono<Void> sendAndForget(Message<?> message, RSocketRequester requester, String route) {
        return requester.route(route)
                .data(message)
                .send()
                .doOnSuccess(v -> log.debug("发送 Fire-and-Forget 消息成功: route={}", route))
                .onErrorResume(e -> {
                    log.error("发送 Fire-and-Forget 消息失败: route={}", route, e);
                    return Mono.empty();
                });
    }
}
