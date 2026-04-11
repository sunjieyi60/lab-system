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
     *   <li>从请求中提取设备UUID和配置信息</li>
     *   <li>查询设备是否存在</li>
     *   <li>更新数据库中的配置信息</li>
     *   <li>清除设备缓存</li>
     *   <li>如果设备在线，通过 RSocket 推送配置到设备</li>
     * </ol>
     * </p>
     */
    @Override
    public Mono<UpdateConfigResponse> updateConfigAndPush(UpdateConfigRequest request) {
        String uuid = request.getTo();
        return Mono.fromCallable(() -> {
            ClassTimeTable device = lambdaQuery()
                    .eq(ClassTimeTable::getUuid, uuid)
                    .one();

            if (device == null) {
                log.warn("更新配置失败，设备 {} 不存在", uuid);
                return UpdateConfigResponse.fail(404, "设备不存在");
            }

            device.setConfig(request.getConfig());
            if (!updateById(device)) {
                log.error("更新设备 {} 配置到数据库失败", uuid);
                return UpdateConfigResponse.fail(500, "数据库更新失败");
            }

            cache.delete(Const.Key.CACHE_DEVICE_UUID_PREFIX + uuid);
            log.info("设备 {} 配置已更新到数据库", uuid);
            return device;
        }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(mixed -> {
                    if (mixed instanceof UpdateConfigResponse r) {
                        return Mono.just(r);
                    }
                    return afterDeviceRowSavedPushProfile(uuid, (ClassTimeTable) mixed);
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
    public Mono<UpdateConfigResponse> updateLaboratoryAndPush(String uuid, Long laboratoryId) {
        return Mono.fromCallable(() -> {
            ClassTimeTable device = lambdaQuery()
                    .eq(ClassTimeTable::getUuid, uuid)
                    .one();

            if (device == null) {
                log.warn("更新实验室失败，设备 {} 不存在", uuid);
                return UpdateConfigResponse.fail(404, "设备不存在");
            }

            device.setLaboratoryId(laboratoryId);
            if (!updateById(device)) {
                log.error("更新设备 {} 实验室到数据库失败", uuid);
                return UpdateConfigResponse.fail(500, "数据库更新失败");
            }

            cache.delete(Const.Key.CACHE_DEVICE_UUID_PREFIX + uuid);
            log.info("设备 {} 实验室已更新为 {}", uuid, laboratoryId);
            return device;
        }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(mixed -> {
                    if (mixed instanceof UpdateConfigResponse r) {
                        return Mono.just(r);
                    }
                    return afterDeviceRowSavedPushProfile(uuid, (ClassTimeTable) mixed);
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
     * @return 推送响应的异步 Mono
     */
    private Mono<UpdateConfigResponse> afterDeviceRowSavedPushProfile(
            String uuid, ClassTimeTable device) {
        if (!connectionManager.isOnline(uuid)) {
            log.info("设备 {} 当前离线，档案将在设备上线后同步", uuid);
            return Mono.just(UpdateConfigResponse.fail(503, "设备离线"));
        }

        UpdateConfigRequest request = buildProfilePushRequest(uuid, device);
        return connectionManager.sendTo(uuid, request, UpdateConfigResponse.class)
                .timeout(Duration.ofSeconds(5))
                .map(response -> {
                    if (response.isSuccess()) {
                        log.info("设备 {} 成功应用档案", uuid);
                    } else {
                        log.warn("设备 {} 拒绝应用档案", uuid);
                    }
                    return response;
                })
                .onErrorResume(throwable -> {
                    String errorMsg = throwable.getMessage();
                    if (errorMsg != null && errorMsg.contains("Timeout")) {
                        log.warn("向设备 {} 推送档案超时", uuid);
                        return Mono.just(UpdateConfigResponse.fail(408, "推送超时"));
                    }
                    log.error("向设备 {} 推送档案失败: {}", uuid, errorMsg);
                    return Mono.just(UpdateConfigResponse.fail(500, "推送失败"));
                });
    }

    /**
     * 构建设备档案推送请求
     * <p>
     * 创建设备配置更新请求，包含配置信息、实验室ID等完整档案数据。
     * 使用 UpdateConfigRequest.create() 工厂方法创建请求对象。
     * </p>
     *
     * @param uuid 设备UUID
     * @param device 设备实体
     * @return 配置更新请求对象
     */
    private static UpdateConfigRequest buildProfilePushRequest(String uuid, ClassTimeTable device) {
        UpdateConfigRequest request = UpdateConfigRequest.create(
                device.getConfig(),
                true,
                System.currentTimeMillis(),
                device.getLaboratoryId()
        );
        request.setTo(uuid);
        request.setFrom(Const.Role.SERVER);
        return request;
    }
}
