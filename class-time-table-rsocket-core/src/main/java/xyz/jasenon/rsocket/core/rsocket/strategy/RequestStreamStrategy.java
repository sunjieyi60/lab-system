package xyz.jasenon.rsocket.core.rsocket.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Status;

import java.time.Instant;
import java.util.List;

/**
 * Request-Stream 发送策略
 */
@Slf4j
@Component
public class RequestStreamStrategy implements SendStrategy {

    @Override
    public Message.Type getType() {
        return Message.Type.REQUEST_STREAM;
    }

    @Override
    public Mono<Message<?>> send(Message<?> message, RSocketRequester requester, String route) {
        return requester.route(route)
                .data(message)
                .retrieveFlux(new org.springframework.core.ParameterizedTypeReference<Message<?>>() {})
                .collectList()
                .map(list -> {
                    Message<Object> response = new Message<>();
                    response.setType(Message.Type.REQUEST_STREAM);
                    response.setPayload(list);
                    response.setStatus(Status.C10000);
                    response.setTimestamp(Instant.now());
                    return response;
                })
                .doOnSuccess(resp -> log.debug("收到流响应: route={}, size={}", route, 
                        ((List<?>) resp.getPayload()).size()))
                .onErrorResume(e -> {
                    log.error("发送流请求失败: route={}", route, e);
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
        response.setPayload(java.util.Map.of("error", errorMessage));
        response.setTimestamp(Instant.now());
        return response;
    }
}
