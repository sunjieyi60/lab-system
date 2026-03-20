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
import xyz.jasenon.rsocket.classtimetablersocket.mapper.ClassTimeTableMapper;
import xyz.jasenon.rsocket.core.model.ClassTimeTable;
import xyz.jasenon.rsocket.core.model.Config;

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
        String cacheKey = Const.Key.CACHE_DEVICE_PREFIX + uuid;
        
        return Mono.fromCallable(() -> 
            cache.get(cacheKey, () -> deviceMapper.selectByUuid(uuid), Const.Default.CACHE_MINUTES)
        ).subscribeOn(Schedulers.boundedElastic())
         .doOnError(e -> log.error("查询设备 {} 失败: {}", uuid, e.getMessage()));
    }

    /**
     * 根据数据库ID查询（带缓存）
     */
    public Mono<ClassTimeTable> getById(Long id) {
        String cacheKey = Const.Key.CACHE_DEVICE_ID_PREFIX + id;
        
        return Mono.fromCallable(() ->
            cache.get(cacheKey, () -> deviceMapper.selectById(id), Const.Default.CACHE_MINUTES)
        ).subscribeOn(Schedulers.boundedElastic())
         .doOnError(e -> log.error("查询设备ID {} 失败: {}", id, e.getMessage()));
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
                log.info("新设备已创建: id={}", device.getId());
            } else {
                // 设备存在，更新实验室ID
                if (request.getLaboratoryId() != null && 
                    !request.getLaboratoryId().equals(device.getLaboratoryId())) {
                    device.setLaboratoryId(request.getLaboratoryId());
                    deviceMapper.updateById(device);
                    // 清除缓存
                    cache.delete(Const.Key.CACHE_DEVICE_PREFIX + device.getUuid());
                }
            }

            // 更新在线状态
            device.setStatus(Const.Status.ONLINE);
            deviceMapper.updateStatus(device.getId(), Const.Status.ONLINE);

            // 清除缓存
            cache.delete(Const.Key.CACHE_DEVICE_PREFIX + device.getUuid());

            // 返回注册响应
            RegisterResponse response = new RegisterResponse();
            response.setDeviceDbId(device.getId());
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
    public Mono<Void> updateStatus(Long deviceDbId, String status) {
        return Mono.fromRunnable(() -> {
            deviceMapper.updateStatus(deviceDbId, status);
            
            // 清除缓存
            ClassTimeTable device = deviceMapper.selectById(deviceDbId);
            if (device != null) {
                cache.delete(Const.Key.CACHE_DEVICE_PREFIX + device.getUuid());
            }
            
            log.debug("设备 {} 状态更新为 {}", deviceDbId, status);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * 更新设备配置
     */
    public Mono<Boolean> updateConfig(Long deviceDbId, Config config) {
        return Mono.fromCallable(() -> {
            ClassTimeTable device = deviceMapper.selectById(deviceDbId);
            if (device == null) {
                log.warn("更新配置失败，设备 {} 不存在", deviceDbId);
                return false;
            }
            
            device.setConfig(config);
            int rows = deviceMapper.updateById(device);
            
            if (rows > 0) {
                // 清除缓存
                cache.delete(Const.Key.CACHE_DEVICE_PREFIX + device.getUuid());
                log.info("设备 {} 配置已更新", deviceDbId);
                return true;
            }
            return false;
            
        }).subscribeOn(Schedulers.boundedElastic())
          .onErrorReturn(false);
    }

    /**
     * 获取实验室下的所有设备ID
     */
    public List<Long> getDeviceIdsByLaboratory(Long laboratoryId) {
        return deviceMapper.selectByLaboratoryId(laboratoryId)
                .stream()
                .map(ClassTimeTable::getId)
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
