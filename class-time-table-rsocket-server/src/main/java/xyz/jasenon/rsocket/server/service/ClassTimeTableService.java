package xyz.jasenon.rsocket.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.cache.Cache;
import xyz.jasenon.rsocket.core.packet.RegisterRequest;
import xyz.jasenon.rsocket.core.packet.RegisterResponse;
import xyz.jasenon.rsocket.core.packet.UpdateConfigRequest;
import xyz.jasenon.rsocket.core.model.ClassTimeTable;
import xyz.jasenon.rsocket.core.model.Config;
import xyz.jasenon.rsocket.server.mapper.ClassTimeTableMapper;

import java.util.List;

/**
 * 班牌设备服务实现类（旧版）
 * <p>
 * 提供班牌设备的基础数据操作，包括设备查询、注册、状态更新、配置管理等功能。
 * 使用 MyBatis Plus 进行数据库操作，结合 Redis 缓存提升性能。
 * 借助 Reactor 的 subscribeOn 实现异步非阻塞操作。
 * </p>
 * <p>
 * <strong>注意：</strong>此服务类为旧版实现，新功能请使用 {@link DeviceService} 接口。
 * </p>
 *
 * @author Jasenon_ce
 * @see ClassTimeTableMapper
 * @see Cache
 * @since 1.0.0
 * @deprecated 建议使用 {@link DeviceService}
 */
@Slf4j
@Service
@Deprecated
public class ClassTimeTableService {

    /** 班牌设备数据访问层 */
    @Autowired
    private ClassTimeTableMapper deviceMapper;

    /** 缓存服务 */
    @Autowired
    private Cache cache;

    /**
     * 根据 UUID 查询设备（带缓存）
     * <p>
     * 先从缓存中查询，缓存未命中则从数据库查询并将结果写入缓存。
     * 缓存有效期由 {@link Const.Default#CACHE_MINUTES} 指定。
     * </p>
     *
     * @param uuid 设备UUID
     * @return 设备信息的异步 Mono
     */
    public Mono<ClassTimeTable> getByUuid(String uuid) {
        String cacheKey = Const.Key.CACHE_DEVICE_UUID_PREFIX + uuid;

        return Mono.fromCallable(() ->
            cache.get(cacheKey, () -> deviceMapper.selectByUuid(uuid), Const.Default.CACHE_MINUTES)
        ).subscribeOn(Schedulers.boundedElastic())
         .doOnError(e -> log.error("查询设备 {} 失败: {}", uuid, e.getMessage()));
    }

    /**
     * 设备注册
     * <p>
     * 处理班牌设备的注册逻辑：
     * <ul>
     *   <li>如果设备不存在，自动创建设备记录</li>
     *   <li>如果设备已存在，更新实验室ID（如有变化）</li>
     *   <li>设置设备状态为在线</li>
     *   <li>清除相关缓存</li>
     * </ul>
     * </p>
     *
     * @param request 设备注册请求，包含UUID和实验室ID
     * @return 注册响应的异步 Mono，包含设备配置信息
     */
    public Mono<RegisterResponse> register(RegisterRequest request) {
        return Mono.fromCallable(() -> {
            log.info("处理设备注册: uuid={}, laboratoryId={}",
                    request.getUuid(), request.getLaboratoryId());

            // 查询设备是否存在
            ClassTimeTable device = deviceMapper.selectByUuid(request.getUuid());

            if (device == null) {
                // 设备不存在，自动创建
                log.info("设备 {} 不存在，自动创建", request.getUuid());

                device = new ClassTimeTable();
                device.setUuid(request.getUuid());
                device.setLaboratoryId(request.getLaboratoryId());
                device.setStatus(Const.Status.OFFLINE);
                device.setConfig(Config.Default());

                deviceMapper.insert(device);
                log.info("新设备已创建: id={}, uuid={}", device.getId(), device.getUuid());
            } else {
                // 设备存在，更新实验室ID
                if (request.getLaboratoryId() != null &&
                    !request.getLaboratoryId().equals(device.getLaboratoryId())) {
                    device.setLaboratoryId(request.getLaboratoryId());
                    deviceMapper.updateById(device);
                    // 清除缓存
                    cache.delete(Const.Key.CACHE_DEVICE_UUID_PREFIX + device.getUuid());
                }
            }

            // 更新在线状态
            device.setStatus(Const.Status.ONLINE);
            deviceMapper.updateStatus(device.getId(), Const.Status.ONLINE);

            // 清除缓存
            cache.delete(Const.Key.CACHE_DEVICE_UUID_PREFIX + device.getUuid());

            // 返回注册响应（使用 uuid 作为唯一标识）
            RegisterResponse response = new RegisterResponse();
            response.setUuid(device.getUuid());
            response.setConfig(device.getConfig() != null ? device.getConfig() : Config.Default());
            response.setLaboratoryId(device.getLaboratoryId());

            return response;

        }).subscribeOn(Schedulers.boundedElastic())
          .onErrorResume(e -> {
              log.error("设备注册失败: {}", e.getMessage(), e);
              return Mono.error(e);
          });
    }

    /**
     * 更新设备在线状态
     * <p>
     * 更新数据库中的设备状态，并清除相关缓存。
     * </p>
     *
     * @param uuid 设备UUID
     * @param status 目标状态，可选值为 {@link Const.Status#ONLINE} 或 {@link Const.Status#OFFLINE}
     * @return 空响应的异步 Mono
     */
    public Mono<Void> updateStatus(String uuid, String status) {
        return Mono.fromRunnable(() -> {
            ClassTimeTable device = deviceMapper.selectByUuid(uuid);
            if (device != null) {
                deviceMapper.updateStatus(device.getId(), status);
                // 清除缓存
                cache.delete(Const.Key.CACHE_DEVICE_UUID_PREFIX + uuid);
                log.debug("设备 {} 状态更新为 {}", uuid, status);
            } else {
                log.warn("更新状态失败，设备 {} 不存在", uuid);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * 更新设备配置
     * <p>
     * 更新数据库中的设备配置信息，并清除相关缓存。
     * 使用 UpdateConfigRequest 作为参数，包含配置信息和版本控制。
     * </p>
     *
     * @param request 配置更新请求，包含设备UUID、配置信息、是否立即生效等
     * @return 是否更新成功的异步 Mono
     */
    public Mono<Boolean> updateConfig(UpdateConfigRequest request) {
        return Mono.fromCallable(() -> {
            String uuid = request.getTo();
            if (uuid == null) {
                log.warn("更新配置失败，请求中未指定设备UUID");
                return false;
            }
            
            ClassTimeTable device = deviceMapper.selectByUuid(uuid);
            if (device == null) {
                log.warn("更新配置失败，设备 {} 不存在", uuid);
                return false;
            }

            Config config = request.getConfig();
            if (config == null) {
                log.warn("更新配置失败，请求中未包含配置信息");
                return false;
            }
            
            device.setConfig(config);
            int rows = deviceMapper.updateById(device);

            if (rows > 0) {
                // 清除缓存
                cache.delete(Const.Key.CACHE_DEVICE_UUID_PREFIX + uuid);
                log.info("设备 {} 配置已更新", uuid);
                return true;
            }
            return false;

        }).subscribeOn(Schedulers.boundedElastic())
          .onErrorReturn(false);
    }

    /**
     * 获取实验室下的所有设备UUID
     * <p>
     * 查询指定实验室关联的所有班牌设备UUID列表。
     * </p>
     *
     * @param laboratoryId 实验室ID
     * @return 设备UUID列表
     */
    public List<String> getDeviceUuidsByLaboratory(Long laboratoryId) {
        return deviceMapper.selectByLaboratoryId(laboratoryId)
                .stream()
                .map(ClassTimeTable::getUuid)
                .toList();
    }

    /**
     * 查询所有在线设备
     * <p>
     * 查询数据库中状态为在线的所有班牌设备。
     * </p>
     *
     * @return 在线设备列表的异步 Mono
     */
    public Mono<List<ClassTimeTable>> getOnlineDevices() {
        return Mono.fromCallable(() -> deviceMapper.selectOnlineDevices())
                .subscribeOn(Schedulers.boundedElastic());
    }
}
