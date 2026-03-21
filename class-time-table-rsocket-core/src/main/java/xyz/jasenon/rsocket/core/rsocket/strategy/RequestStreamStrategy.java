package xyz.jasenon.rsocket.core.rsocket.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Request-Stream 发送策略
 * 
 * 请求流模式，将多个响应聚合为一条消息
 */
@Slf4j
public class RequestStreamStrategy implements SendStrategy {

    @Override
    public Message.Type getType() {
        return Message.Type.REQUEST_STREAM;
    }

    @Override
    public Mono<Message<?>> send(Message<?> message, RSocketRequester requester, String route) {
        return requester.route(route)
                .data(message)
                .retrieveFlux(new ParameterizedTypeReference<Message<?>>() {})
                .collectList()
                .<Message<?>>flatMap(list -> {  // 显式指定 flatMap 返回类型
                    Message<List<Message<?>>> response = new Message<>();
                    response.setType(Message.Type.REQUEST_STREAM);
                    response.setData(list);
                    response.setStatus(Status.C10000);
                    response.setTimestamp(Instant.now());
                    return Mono.just(response);
                })
                .doOnSuccess(resp -> log.debug("收到流响应: route={}, size={}", route,
                        ((List<?>) resp.getData()).size()))
                .onErrorResume(e -> {
                    log.error("RSocket 流请求失败: route={}", route, e);
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
        response.setData(Map.of("error", errorMessage));
        response.setTimestamp(Instant.now());
        return response;
    }
}
