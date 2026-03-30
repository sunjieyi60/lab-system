package xyz.jasenon.rsocket.server.controller.rsocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.packet.Heartbeat;
import xyz.jasenon.rsocket.core.packet.RegisterRequest;
import xyz.jasenon.rsocket.core.packet.RegisterResponse;
import xyz.jasenon.rsocket.core.rsocket.AbstractConnectionManager;
import xyz.jasenon.rsocket.server.service.DeviceService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ConnectController {

    private final DeviceService deviceService;
    private final AbstractConnectionManager manager;

    @MessageMapping(Const.Route.DEVICE_REGISTER)
    public Mono<RegisterResponse> registerResponse(
            @Payload RegisterRequest request,
            RSocketRequester requester
            ){

        log.info("收到设备注册请求: uuid={}, laboratoryId={}",
                request.getUuid(), request.getLaboratoryId());

        return deviceService.register(request)
                .map(resp -> {
                    manager.register(resp.getUuid(), requester);
                    return resp;
                });
    }

    @MessageMapping(Const.Route.DEVICE_HEARTBEAT)
    public Mono<Void> heartbeat(
            @Payload Heartbeat heartbeat
            ){
        log.debug("接收到传来的心跳包{}",heartbeat.getUuid());
        return Mono.empty();
    }

}
