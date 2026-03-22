package xyz.jasenon.rsocket.core.rsocket.client;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Status;
import xyz.jasenon.rsocket.core.rsocket.client.handler.Handler;
import xyz.jasenon.rsocket.core.rsocket.client.handler.HandlerManager;
import xyz.jasenon.rsocket.core.utils.Convert;

/**
 * 客户端响应处理器 - 自有协议
 * 
 * 处理服务端主动发送的消息（服务端调用客户端）
 * 根据 Command 路由到对应的 Handler 处理
 * 使用自有协议的 Status 包装响应
 * 
 * 注意：server -> client 的消息使用 command 标识
 */
@Slf4j
public class ClientResponder implements RSocket {

    @Override
    public Mono<Void> fireAndForget(@NonNull Payload payload) {
        return Mono.fromCallable(() -> Convert.castMessage(payload))
                .doOnNext(msg -> payload.release())
                .flatMap(this::handleFireAndForget)
                .onErrorResume(e -> {
                    log.error("处理 FireAndForget 消息失败", e);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Payload> requestResponse(@NonNull Payload payload) {
        return Mono.fromCallable(() -> Convert.castMessage(payload))
                .doOnNext(msg -> payload.release())
                .flatMap(this::handleRequestResponse)
                .map(Convert::castPayload)
                .onErrorResume(e -> {
                    log.error("处理 RequestResponse 消息失败", e);
                    // server -> client 错误，使用 command
                    return Mono.just(Convert.castPayload(
                            Message.error(Command.REGISTER, Status.C10001, "处理失败", e.getMessage())));
                });
    }

    @Override
    public Flux<Payload> requestStream(@NonNull Payload payload) {
        return RSocket.super.requestStream(payload);
    }

    @Override
    public Flux<Payload> requestChannel(@NonNull Publisher<Payload> payloads) {
        return RSocket.super.requestChannel(payloads);
    }

    /**
     * 处理 FireAndForget 消息（server -> client）
     */
    private Mono<Void> handleFireAndForget(Message message) {
        Command command = message.getCommand();
        if (command == null) {
            log.warn("收到 FireAndForget 消息，但缺少 Command 信息");
            return Mono.empty();
        }

        log.debug("处理 FireAndForget 消息: command={}", command);

        Handler handler = HandlerManager.get(command);
        if (handler == null) {
            log.warn("未找到 Command={} 对应的 Handler", command);
            return Mono.empty();
        }

        return handler.handler(message)
                .doOnSuccess(resp -> log.debug("FireAndForget 消息处理完成: command={}, code={}", 
                        command, resp.getCode()))
                .doOnError(e -> log.error("FireAndForget 消息处理失败: command={}", command, e))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    /**
     * 处理 RequestResponse 消息（server -> client）
     */
    private Mono<Message> handleRequestResponse(Message message) {
        Command command = message.getCommand();
        if (command == null) {
            log.warn("收到 RequestResponse 消息，但缺少 Command 信息");
            // server -> client 错误，使用 command
            return Mono.just(Message.error(Command.REGISTER, Status.C10001,
                    "消息缺少 Command 信息", null));
        }

        log.debug("处理 RequestResponse 消息: command={}", command);

        Handler handler = HandlerManager.get(command);
        if (handler == null) {
            log.warn("未找到 Command={} 对应的 Handler", command);
            // server -> client 错误，使用 command
            return Mono.just(Message.error(command, Status.C10001,
                    "不支持的 Command: " + command, null));
        }

        return handler.handler(message)
                .doOnSuccess(resp -> log.debug("RequestResponse 消息处理完成: command={}, code={}, msg={}", 
                        command, resp.getCode(), resp.getMsg()))
                .doOnError(e -> log.error("RequestResponse 消息处理失败: command={}", command, e))
                .onErrorResume(e -> Mono.just(Message.error(command, Status.C10001,
                        "处理失败", e.getMessage())));
    }
}
