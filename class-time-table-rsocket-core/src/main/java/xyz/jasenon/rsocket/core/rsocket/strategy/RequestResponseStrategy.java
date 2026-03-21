package xyz.jasenon.rsocket.core.rsocket.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Status;

import java.time.Instant;

/**
 * Request-Response 发送策略
 * 
 * 支持类型安全的请求-响应模式
 */
@Slf4j
public class RequestResponseStrategy implements SendStrategy {

    @Override
    public Message.Type getType() {
        return Message.Type.REQUEST_RESPONSE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Message<?>> send(Message<?> message, RSocketRequester requester, String route) {
        return requester.route(route)
                .data(message)
                .retrieveMono(new ParameterizedTypeReference<Message<?>>() {})
                .doOnSuccess(resp -> log.debug("收到响应: route={}, status={}", route, resp.getStatus()))
                .onErrorResume(e -> {
                    log.error("发送请求失败: route={}", route, e);
                    return Mono.just(createErrorResponse(e.getMessage()));
                });
    }
    
    /**
     * 创建错误响应
     */
    private Message<?> createErrorResponse(String errorMessage) {
        Message<Object> response = new Message<>();
        response.setType(Message.Type.REQUEST_RESPONSE);
        response.setStatus(Status.C10001);
        response.setData(java.util.Map.of("error", errorMessage));
        response.setTimestamp(Instant.now());
        return response;
    }
}
