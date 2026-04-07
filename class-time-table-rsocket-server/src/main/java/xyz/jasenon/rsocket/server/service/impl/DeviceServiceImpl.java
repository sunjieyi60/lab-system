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

/**
 * 班牌设备服务实现类
 * <p>
 * 提供班牌设备的核心业务逻辑实现，包括：
 * <ul>
 *   <li>设备注册与身份验证</li>
 *   <li>设备状态管理</li>
 *   <li>配置更新与推送</li>
 *   <li>实验室关联管理</li>
 * </ul>
 * 继承 MyBatis Plus 的 {@link MPJBaseServiceImpl} 提供基础 CRUD 能力。
 * </p>
 *
 * @author Jasenon_ce
 * @see DeviceService
 * @see ClassicConnectManager
 * @since 1.0.0
 */
@Slf4j
@Service
public class DeviceServiceImpl extends MPJBaseServiceImpl<DeviceMapper, ClassTimeTable> implements DeviceService, Const.Key {

    /** 缓存服务，用于设备数据缓存 */
    @Autowired
    private Cache cache;

    /** 连接管理器，用于管理设备 RSocket 连接 */
    @Autowired
    private ClassicConnectManager connectionManager;

    /**
     * {@inheritDoc}
     * <p>
     * 设备注册逻辑：
     * <ul>
     *   <li>根据 UUID 查询设备</li>
     *   <li>设备不存在则自动创建，使用默认配置</li>
     *   <li>设备存在则更新实验室ID（如有变化）</li>
     *   <li>返回包含配置信息的注册响应</li>
     * </ul>
     * </p>
     */
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

    /**
     * {@inheritDoc}
     * <p>
     * <strong>已弃用</strong>：此方法暂未实现，RSocket 协议级心跳已足够。
     * </p>
     */
    @Override
    public Mono<Void> heartbeat(Heartbeat heartbeat) {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 根据状态查询设备列表：
     * <ul>
     *   <li>status 为 null 或空字符串时查询所有设备</li>
     *   <li>status 必须为 ONLINE 或 OFFLINE，否则抛出异常</li>
     * </ul>
     * </p>
     *
     * @throws xyz.jasenon.lab.common.exception.BusinessException 当状态参数不合法时抛出
     */
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

    /**
     * {@inheritDoc}
     * <p>
     * 更新配置并推送到设备的完整流程：
     * <ol>
     *   <li>查询设备是否存在</li>
     *   <li>更新数据库中的配置信息</li>
     *   <li>清除设备缓存</li>
     *   <li>如果设备在线，通过 RSocket 推送配置到设备</li>
     * </ol>
     * </p>
     */
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

    /**
     * {@inheritDoc}
     * <p>
     * 更新实验室并推送到设备的完整流程：
     * <ol>
     *   <li>查询设备是否存在</li>
     *   <li>更新数据库中的实验室ID</li>
     *   <li>清除设备缓存</li>
     *   <li>如果设备在线，推送完整的设备档案（配置+实验室）到设备</li>
     * </ol>
     * </p>
     */
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

    /**
     * 设备数据保存后的档案推送处理
     * <p>
     * 检查设备在线状态，如在线则通过 RSocket 推送设备档案。
     * 推送超时时间为5秒。
     * </p>
     *
     * @param uuid 设备UUID
     * @param device 设备实体
     * @param trigger 推送触发类型（配置更新或实验室变更）
     * @return 推送结果的异步 Mono
     */
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

    /**
     * 构建推送结果元数据
     * <p>
     * 为推送结果设置详细的元数据信息，包括触发类型、持久化字段、在线状态等。
     * </p>
     *
     * @param r 基础推送结果
     * @param trigger 推送触发类型
     * @param persisted 已持久化到数据库的字段列表
     * @param deviceOnline 设备是否在线
     * @param pushAttempted 是否尝试推送
     * @param payloadKeys 推送到设备的字段列表
     * @return 包含完整元数据的推送结果
     */
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

    /**
     * 构建设备档案推送请求
     * <p>
     * 创建设备配置更新请求，包含配置信息、实验室ID等完整档案数据。
     * </p>
     *
     * @param uuid 设备UUID
     * @param device 设备实体
     * @return 配置更新请求对象
     */
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
