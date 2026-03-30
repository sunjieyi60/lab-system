package xyz.jasenon.rsocket.server.service;

import com.github.yulichang.base.MPJBaseService;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.model.ClassTimeTable;
import xyz.jasenon.rsocket.core.packet.Heartbeat;
import xyz.jasenon.rsocket.core.packet.RegisterRequest;
import xyz.jasenon.rsocket.core.packet.RegisterResponse;

import java.util.List;

public interface DeviceService extends MPJBaseService<ClassTimeTable> {

    Mono<RegisterResponse> register(RegisterRequest request);

    Mono<Void> heartbeat(Heartbeat heartbeat);

    List<ClassTimeTable> listAll(String status);

}
