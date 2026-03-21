package xyz.jasenon.rsocket.core.rsocket.client.handler;

import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;

public interface Handler {

    Command command();

    <T,R> Mono<Message<R>> handler(Message<?> message);

}
