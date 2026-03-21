package xyz.jasenon.rsocket.core;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.model.ClassTimeTable;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;
import xyz.jasenon.rsocket.core.rsocket.ConnectionManager;
import xyz.jasenon.rsocket.core.rsocket.Server;

@Component
@RequiredArgsConstructor
public class Api {

    private final ConnectionManager connectionManager;
    private final Server server;

    public void bindClassTimeTable(ClassTimeTable classTimeTable, RSocketRequester requester){
        connectionManager.register(classTimeTable.unique(), requester);
    };

    public RSocketRequester getRequesterByUnique(ClassTimeTable classTimeTable){
        return connectionManager.getRequester(classTimeTable.unique());
    };

    public Mono<Message<?>> send(Message<?> message, RSocketRequester requester){
        return server.send(message, requester);
    }

    public <T,R> Mono<Message<R>> send(MessageAdaptor<T, R> adaptor, RSocketRequester requester){
        return server.send(adaptor, requester);
    }

    public Mono<Message<?>> sendTo(String deviceId, Message<?> message){
        return server.sendTo(deviceId, message);
    }

    public <T,R> Mono<Message<R>> sendTo(String deviceId, MessageAdaptor<T,R> adaptor){
        return server.sendTo(deviceId, adaptor);
    }

    public Mono<Integer> broadcast(Message<?> message){
        return server.broadcast(message);
    }

}
