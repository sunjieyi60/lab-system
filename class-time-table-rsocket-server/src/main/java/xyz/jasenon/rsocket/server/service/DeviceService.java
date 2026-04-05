package xyz.jasenon.rsocket.server.service;

import com.github.yulichang.base.MPJBaseService;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.model.ClassTimeTable;
import xyz.jasenon.rsocket.core.model.Config;
import xyz.jasenon.rsocket.core.packet.Heartbeat;
import xyz.jasenon.rsocket.core.packet.RegisterRequest;
import xyz.jasenon.rsocket.core.packet.RegisterResponse;
import xyz.jasenon.rsocket.server.vo.PushConfigResult;

import java.util.List;

public interface DeviceService extends MPJBaseService<ClassTimeTable> {

    Mono<RegisterResponse> register(RegisterRequest request);

    Mono<Void> heartbeat(Heartbeat heartbeat);

    List<ClassTimeTable> listAll(String status);

    /**
     * 更新设备配置并推送到设备
     */
    Mono<PushConfigResult> updateConfigAndPush(String uuid, Config config);

    /**
     * 更新设备关联实验室并推送档案（配置 + 实验室）到在线设备
     */
    Mono<PushConfigResult> updateLaboratoryAndPush(String uuid, Long laboratoryId);

}
