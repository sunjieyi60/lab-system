package xyz.jasenon.rsocket.core.rsocket.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Status;

import java.time.Instant;
import java.util.Map;

/**
 * Request-Response 发送策略
 */
@Slf4j
@Component
public class RequestResponseStrategy implements SendStrategy {

    @Override
    public Message.Type getType() {
        return Message.Type.REQUEST_RESPONSE;
    }

    @Override
    public Mono<Message<?>> send(Message<?> message, RSocketRequester requester, String route) {
        return requester.route(route)
                .data(message)
                .retrieveMono(new org.springframework.core.ParameterizedTypeReference<Message<?>>() {})
                .doOnSuccess(resp -> log.debug("收到响应: route={}", route))
                .onErrorResume(e -> {
                    log.error("发送请求失败: route={}", route, e);
                    return Mono.just(createErrorResponse(e.getMessage()));
                });
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
        response.setPayload(Map.of("error", errorMessage));
        response.setTimestamp(Instant.now());
        return response;
    }
}
