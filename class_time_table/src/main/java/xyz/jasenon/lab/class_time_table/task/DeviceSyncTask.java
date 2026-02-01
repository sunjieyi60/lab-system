package xyz.jasenon.lab.class_time_table.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.class_time_table.entity.Device;
import xyz.jasenon.lab.class_time_table.mapper.DeviceMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 设备同步任务
 * 定时将Redis中的设备信息刷盘到MySQL
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceSyncTask {
    
    private final RedissonClient redissonClient;
    private final DeviceMapper deviceMapper;
    
    private static final String DEVICE_REGISTER_KEY_PREFIX = "smartboard:device:register:";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    /**
     * 定时同步任务：每5分钟执行一次
     * 将Redis中的设备注册信息刷盘到MySQL
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void syncDevicesToDatabase() {
        try {
            log.debug("开始执行设备信息刷盘任务");
            
            // 获取所有设备注册key
            // 注意：getKeysByPattern已deprecated，但为了兼容性暂时使用
            // 生产环境建议使用Redis SCAN命令或Redisson的scan方法
            Iterable<String> deviceKeysIter = redissonClient.getKeys().getKeysByPattern(DEVICE_REGISTER_KEY_PREFIX + "*");
            java.util.Set<String> deviceKeys = new java.util.HashSet<>();
            for (String key : deviceKeysIter) {
                deviceKeys.add(key);
            }
            
            int successCount = 0;
            int failCount = 0;
            
            for (String key : deviceKeys) {
                try {
                    RMap<String, Object> deviceMap = redissonClient.getMap(key);
                    if (!deviceMap.isExists() || deviceMap.isEmpty()) {
                        continue;
                    }
                    
                    // 从Redis读取设备信息
                    Device device = mapToDevice(deviceMap);
                    if (device == null) {
                        log.warn("设备信息映射失败，key: {}", key);
                        failCount++;
                        continue;
                    }
                    
                    // 检查MySQL中是否已存在
                    Device existingDevice = deviceMapper.selectOne(
                        new LambdaQueryWrapper<Device>().eq(Device::getDeviceId, device.getDeviceId())
                    );
                    
                    if (existingDevice == null) {
                        // 新设备，插入
                        deviceMapper.insert(device);
                        log.info("新设备已刷盘到MySQL: deviceId={}", device.getDeviceId());
                    } else {
                        // 已存在，更新
                        device.setCreateTime(existingDevice.getCreateTime()); // 保留创建时间
                        deviceMapper.updateById(device);
                        log.debug("设备信息已更新到MySQL: deviceId={}", device.getDeviceId());
                    }
                    
                    successCount++;
                    
                } catch (Exception e) {
                    log.error("刷盘设备信息失败，key: {}", key, e);
                    failCount++;
                }
            }
            
            log.info("设备信息刷盘任务完成，成功: {}, 失败: {}", successCount, failCount);
            
        } catch (Exception e) {
            log.error("设备信息刷盘任务执行失败", e);
        }
    }
    
    /**
     * 将Redis Map转换为Device实体
     */
    private Device mapToDevice(RMap<String, Object> deviceMap) {
        try {
            Device device = new Device();
            device.setDeviceId((String) deviceMap.get("deviceId"));
            device.setDeviceName((String) deviceMap.get("deviceName"));
            device.setMacAddress((String) deviceMap.get("macAddress"));
            device.setVersion((String) deviceMap.get("version"));
            device.setIpAddress((String) deviceMap.get("ipAddress"));
            device.setDeviceType((String) deviceMap.get("deviceType"));
            device.setHardwareInfo((String) deviceMap.get("hardwareInfo"));
            device.setStatus((String) deviceMap.get("status"));
            
            // 解析时间字段
            String lastOnlineTimeStr = (String) deviceMap.get("lastOnlineTime");
            if (lastOnlineTimeStr != null) {
                device.setLastOnlineTime(LocalDateTime.parse(lastOnlineTimeStr, DATE_TIME_FORMATTER));
            }
            
            String createTimeStr = (String) deviceMap.get("createTime");
            if (createTimeStr != null) {
                device.setCreateTime(LocalDateTime.parse(createTimeStr, DATE_TIME_FORMATTER));
            }
            
            String updateTimeStr = (String) deviceMap.get("updateTime");
            if (updateTimeStr != null) {
                device.setUpdateTime(LocalDateTime.parse(updateTimeStr, DATE_TIME_FORMATTER));
            }
            
            return device;
        } catch (Exception e) {
            log.error("转换设备信息失败", e);
            return null;
        }
    }
}

