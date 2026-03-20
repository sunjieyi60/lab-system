package xyz.jasenon.rsocket.core.rsocket.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Status;

import java.time.Instant;

/**
 * Fire-and-Forget 发送策略
 */
@Slf4j
@Component
public class FireAndForgetStrategy implements SendStrategy {

    @Override
    public Message.Type getType() {
        return Message.Type.FIRE_AND_FORGET;
    }

    @Override
    public Mono<Message<?>> send(Message<?> message, RSocketRequester requester, String route) {
        return requester.route(route)
                .data(message)
                .send()
                .then(Mono.just(createSuccessResponse()))
                .doOnSuccess(v -> log.debug("发送 Fire-and-Forget 消息成功: route={}", route))
                .onErrorResume(e -> {
                    log.error("发送 Fire-and-Forget 消息失败: route={}", route, e);
                    return Mono.just(createErrorResponse(e.getMessage()));
                });
    }
    
    private Message<?> createSuccessResponse() {
        Message<Object> response = new Message<>();
        response.setType(Message.Type.REQUEST_RESPONSE);
        response.setStatus(Status.C10000);
        response.setTimestamp(Instant.now());
        return response;
    }
    
    private Message<?> createErrorResponse(String errorMessage) {
        Message<Object> response = new Message<>();
        response.setType(Message.Type.REQUEST_RESPONSE);
        response.setStatus(new Status() {
            @Override
            public Integer getCode() { return -1; }
            @Override
            public String getMsg() { return errorMessage; }
        });
        response.setPayload(java.util.Map.of("error", errorMessage));
        response.setTimestamp(Instant.now());
        return response;
    }
}
