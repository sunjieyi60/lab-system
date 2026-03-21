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
import xyz.jasenon.rsocket.core.model.ClassTimeTable;
import xyz.jasenon.rsocket.core.model.Config;
import xyz.jasenon.rsocket.server.mapper.ClassTimeTableMapper;

import java.util.List;

/**
 * 班牌设备服务
 *
 * 使用 MyBatis Plus + Redis 缓存
 * 借助 Reactor 的 subscribeOn 实现异步
 */
@Slf4j
@Service
public class ClassTimeTableService {

    @Autowired
    private ClassTimeTableMapper deviceMapper;

    @Autowired
    private Cache cache;

    /**
     * 根据 uuid 查询（带缓存）
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

            return response;

        }).subscribeOn(Schedulers.boundedElastic())
          .onErrorResume(e -> {
              log.error("设备注册失败: {}", e.getMessage(), e);
              return Mono.error(e);
          });
    }

    /**
     * 更新设备在线状态
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
     */
    public Mono<Boolean> updateConfig(String uuid, Config config) {
        return Mono.fromCallable(() -> {
            ClassTimeTable device = deviceMapper.selectByUuid(uuid);
            if (device == null) {
                log.warn("更新配置失败，设备 {} 不存在", uuid);
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
     */
    public List<String> getDeviceUuidsByLaboratory(Long laboratoryId) {
        return deviceMapper.selectByLaboratoryId(laboratoryId)
                .stream()
                .map(ClassTimeTable::getUuid)
                .toList();
    }

    /**
     * 查询所有在线设备
     */
    public Mono<List<ClassTimeTable>> getOnlineDevices() {
        return Mono.fromCallable(() -> deviceMapper.selectOnlineDevices())
                .subscribeOn(Schedulers.boundedElastic());
    }
}
