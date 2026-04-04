package xyz.jasenon.rsocket.server.service.impl;

import com.github.yulichang.base.MPJBaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.cache.Cache;
import xyz.jasenon.rsocket.core.model.ClassTimeTable;
import xyz.jasenon.rsocket.core.model.Config;
import xyz.jasenon.rsocket.core.packet.Heartbeat;
import xyz.jasenon.rsocket.core.packet.RegisterRequest;
import xyz.jasenon.rsocket.core.packet.RegisterResponse;
import xyz.jasenon.rsocket.core.packet.UpdateConfigRequest;
import xyz.jasenon.rsocket.core.packet.UpdateConfigResponse;
import xyz.jasenon.rsocket.core.protocol.Status;
import xyz.jasenon.rsocket.server.diy.ClassicConnectManager;
import xyz.jasenon.rsocket.server.mapper.DeviceMapper;
import xyz.jasenon.rsocket.server.service.DeviceService;
import xyz.jasenon.rsocket.server.vo.ProfilePushTrigger;
import xyz.jasenon.rsocket.server.vo.PushConfigResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DeviceServiceImpl extends MPJBaseServiceImpl<DeviceMapper, ClassTimeTable> implements DeviceService,Const.Key{

    @Autowired
    private Cache cache;

    @Autowired
    private ClassicConnectManager connectionManager;

    @Override
    public Mono<RegisterResponse> register(RegisterRequest request) {
        return Mono.fromCallable(
                () ->{
                    log.info("Server 收到注册包 uuid:{}, laboratoryId:{}", request.getUuid(),request.getLaboratoryId());
                    var device = lambdaQuery().eq(ClassTimeTable::getUuid, request.getUuid()).one();

                    if (device == null){
                        var insert = new ClassTimeTable();
                        insert.setUuid(request.getUuid());
                        insert.setConfig(Config.Default());
                        insert.setLaboratoryId(request.getLaboratoryId());
                        save(insert);
                        device = insert;
                    }else{
                        if (device.getLaboratoryId() != null &&
                                !device.getLaboratoryId().equals(request.getLaboratoryId())){
                            device.setLaboratoryId(request.getLaboratoryId());
                            updateById(device);
                        }
                    }
                    // attention 这里的缓存清理有connect管理

                    // 包装响应类
                    var response = new RegisterResponse();
                    response.setConfig(device.getConfig() == null ? Config.Default() : device.getConfig());
                    response.setLaboratoryId(device.getLaboratoryId());
                    response.setUuid(request.getUuid());
                    response.setStatus(Status.C10000);
                    response.setFrom(Const.Role.SERVER);
                    response.setTo(request.getUuid());

                    return response;
                }
        ).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> heartbeat(Heartbeat heartbeat) {
        return null;
    }

    @Override
    public List<ClassTimeTable> listAll(String status) {
        if (status == null || status.isBlank()) {
            // 查询所有设备
            return lambdaQuery().list();
        }
        if (!List.of(Const.Status.ONLINE, Const.Status.OFFLINE).contains(status)){
            throw R.badRequest("不合法的参数").convert();
        }
        return lambdaQuery().eq(ClassTimeTable::getStatus, status).list();
    }

    @Override
    public Mono<PushConfigResult> updateConfigAndPush(String uuid, Config config) {
        return Mono.fromCallable(() -> {
            ClassTimeTable device = lambdaQuery()
                    .eq(ClassTimeTable::getUuid, uuid)
                    .one();

            if (device == null) {
                log.warn("更新配置失败，设备 {} 不存在", uuid);
                return meta(PushConfigResult.notFound(uuid), ProfilePushTrigger.CONFIG,
                        List.of(), false, false, List.of());
            }

            device.setConfig(config);
            if (!updateById(device)) {
                log.error("更新设备 {} 配置到数据库失败", uuid);
                return meta(PushConfigResult.error(uuid, "数据库更新失败"), ProfilePushTrigger.CONFIG,
                        List.of(), false, false, List.of());
            }

            cache.delete(Const.Key.CACHE_DEVICE_UUID_PREFIX + uuid);
            log.info("设备 {} 配置已更新到数据库", uuid);
            return device;
        }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(mixed -> {
                    if (mixed instanceof PushConfigResult r) {
                        return Mono.just(r);
                    }
                    return afterDeviceRowSavedPushProfile(uuid, (ClassTimeTable) mixed, ProfilePushTrigger.CONFIG);
                });
    }

    @Override
    public Mono<PushConfigResult> updateLaboratoryAndPush(String uuid, Long laboratoryId) {
        return Mono.fromCallable(() -> {
            ClassTimeTable device = lambdaQuery()
                    .eq(ClassTimeTable::getUuid, uuid)
                    .one();

            if (device == null) {
                log.warn("更新实验室失败，设备 {} 不存在", uuid);
                return meta(PushConfigResult.notFound(uuid), ProfilePushTrigger.LABORATORY,
                        List.of(), false, false, List.of());
            }

            device.setLaboratoryId(laboratoryId);
            if (!updateById(device)) {
                log.error("更新设备 {} 实验室到数据库失败", uuid);
                return meta(PushConfigResult.error(uuid, "数据库更新失败"), ProfilePushTrigger.LABORATORY,
                        List.of(), false, false, List.of());
            }

            cache.delete(Const.Key.CACHE_DEVICE_UUID_PREFIX + uuid);
            log.info("设备 {} 实验室已更新为 {}", uuid, laboratoryId);
            return device;
        }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(mixed -> {
                    if (mixed instanceof PushConfigResult r) {
                        return Mono.just(r);
                    }
                    return afterDeviceRowSavedPushProfile(uuid, (ClassTimeTable) mixed, ProfilePushTrigger.LABORATORY);
                });
    }

    private Mono<PushConfigResult> afterDeviceRowSavedPushProfile(
            String uuid, ClassTimeTable device, ProfilePushTrigger trigger) {
        List<String> persisted = trigger == ProfilePushTrigger.CONFIG
                ? List.of("config")
                : List.of("laboratoryId");
        List<String> downlink = PushConfigResult.profileDownlinkKeys();

        if (!connectionManager.isOnline(uuid)) {
            log.info("设备 {} 当前离线，档案将在设备上线后同步", uuid);
            PushConfigResult r = PushConfigResult.offline(uuid);
            return Mono.just(meta(r, trigger, persisted, false, false, downlink));
        }

        UpdateConfigRequest request = buildProfilePushRequest(uuid, device);
        return connectionManager.sendTo(uuid, request, UpdateConfigResponse.class)
                .timeout(Duration.ofSeconds(5))
                .map(response -> {
                    if (response.isSuccess()) {
                        log.info("设备 {} 成功应用档案", uuid);
                        return meta(PushConfigResult.success(uuid), trigger, persisted, true, true, downlink);
                    }
                    log.warn("设备 {} 拒绝应用档案: {}", uuid, response.getMsg());
                    return meta(PushConfigResult.rejected(uuid), trigger, persisted, true, true, downlink);
                })
                .onErrorResume(throwable -> {
                    String errorMsg = throwable.getMessage();
                    if (errorMsg != null && errorMsg.contains("Timeout")) {
                        log.warn("向设备 {} 推送档案超时", uuid);
                        return Mono.just(meta(PushConfigResult.timeout(uuid), trigger, persisted, true, true, downlink));
                    }
                    log.error("向设备 {} 推送档案失败: {}", uuid, errorMsg);
                    return Mono.just(meta(PushConfigResult.error(uuid, errorMsg), trigger, persisted, true, true, downlink));
                });
    }

    private static PushConfigResult meta(
            PushConfigResult r,
            ProfilePushTrigger trigger,
            List<String> persisted,
            boolean deviceOnline,
            boolean pushAttempted,
            List<String> payloadKeys) {
        r.setTrigger(trigger);
        r.setPersistedInDatabase(persisted == null ? new ArrayList<>() : new ArrayList<>(persisted));
        r.setDeviceOnline(deviceOnline);
        r.setPushAttempted(pushAttempted);
        r.setNotifiedDevicePayloadKeys(payloadKeys == null ? new ArrayList<>() : new ArrayList<>(payloadKeys));
        return r;
    }

    private static UpdateConfigRequest buildProfilePushRequest(String uuid, ClassTimeTable device) {
        Config cfg = device.getConfig() != null ? device.getConfig() : Config.Default();
        UpdateConfigRequest request = UpdateConfigRequest.create(
                cfg,
                true,
                System.currentTimeMillis(),
                device.getLaboratoryId()
        );
        request.setTo(uuid);
        request.setFrom(Const.Role.SERVER);
        return request;
    }
}
