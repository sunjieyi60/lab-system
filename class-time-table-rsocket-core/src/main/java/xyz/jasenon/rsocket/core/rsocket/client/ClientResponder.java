package xyz.jasenon.rsocket.core.rsocket.client;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import lombok.NonNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.rsocket.client.handler.HandlerManager;
import xyz.jasenon.rsocket.core.utils.Convert;

public class ClientResponder implements RSocket {

    @Override
    public Mono<Void> fireAndForget(@NonNull Payload payload) {
        try {
            Message<?> message = Convert.castMessage(payload);
            payload.release();

            Command command = message.getCommand();
        }
    }

    @Override
    public Mono<Payload> requestResponse(@NonNull Payload payload) {
        return RSocket.super.requestResponse(payload);
    }

    @Override
    public Flux<Payload> requestStream(@NonNull Payload payload) {
        return RSocket.super.requestStream(payload);
    }

    @Override
    public Flux<Payload> requestChannel(@NonNull Publisher<Payload> payloads) {
        return RSocket.super.requestChannel(payloads);
    }
}
