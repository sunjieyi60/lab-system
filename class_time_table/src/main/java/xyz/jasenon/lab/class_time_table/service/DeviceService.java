package xyz.jasenon.lab.class_time_table.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.class_time_table.entity.Device;
import xyz.jasenon.lab.class_time_table.mapper.DeviceMapper;

import java.time.LocalDateTime;

/**
 * 设备服务
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService extends ServiceImpl<DeviceMapper, Device> {
    
    /**
     * 根据设备ID获取设备信息
     * 
     * @param deviceId 设备ID
     * @return 设备信息
     */
    public Device getDeviceById(String deviceId) {
        return getOne(new LambdaQueryWrapper<Device>().eq(Device::getDeviceId, deviceId));
    }
    
    // 注意：新方案不再需要设备公钥，设备信息通过加密传输自动注册
    
    /**
     * 更新设备在线状态
     * 
     * @param deviceId 设备ID
     * @param ipAddress IP地址
     * @param version 设备版本
     */
    public void updateDeviceOnlineStatus(String deviceId, String ipAddress, String version) {
        Device device = getDeviceById(deviceId);
        if (device == null) {
            log.warn("设备{}不存在，无法更新在线状态", deviceId);
            return;
        }
        
        device.setStatus("ONLINE");
        device.setIpAddress(ipAddress);
        device.setLastOnlineTime(LocalDateTime.now());
        if (version != null) {
            device.setVersion(version);
        }
        device.setUpdateTime(LocalDateTime.now());
        
        updateById(device);
        log.debug("设备{}在线状态已更新", deviceId);
    }
    
    /**
     * 更新设备离线状态
     * 
     * @param deviceId 设备ID
     */
    public void updateDeviceOfflineStatus(String deviceId) {
        Device device = getDeviceById(deviceId);
        if (device == null) {
            return;
        }
        
        device.setStatus("OFFLINE");
        device.setUpdateTime(LocalDateTime.now());
        updateById(device);
        log.debug("设备{}已更新为离线状态", deviceId);
    }
    
    /**
     * 获取设备配置DTO
     * 
     * @param deviceId 设备ID
     * @return 设备配置DTO
     */
    public xyz.jasenon.lab.class_time_table.dto.DeviceConfigDTO getDeviceConfig(String deviceId) {
        Device device = getDeviceById(deviceId);
        if (device == null) {
            log.warn("设备{}不存在，返回默认配置", deviceId);
            return getDefaultConfig();
        }
        
        // 如果设备有配置JSON，解析并返回；否则返回默认配置
        if (device.getConfigJson() != null && !device.getConfigJson().isEmpty()) {
            try {
                return com.alibaba.fastjson2.JSON.parseObject(
                    device.getConfigJson(), 
                    xyz.jasenon.lab.class_time_table.dto.DeviceConfigDTO.class
                );
            } catch (Exception e) {
                log.warn("设备{}配置JSON解析失败，使用默认配置", deviceId, e);
                return getDefaultConfig();
            }
        }
        
        return getDefaultConfig();
    }
    
    /**
     * 获取默认设备配置
     * 
     * @return 默认配置DTO
     */
    private xyz.jasenon.lab.class_time_table.dto.DeviceConfigDTO getDefaultConfig() {
        return xyz.jasenon.lab.class_time_table.dto.DeviceConfigDTO.builder()
                .heartbeatInterval(5000)
                .reconnectDelay(5000)
                .chunkSize(524288)
                .features(xyz.jasenon.lab.class_time_table.dto.DeviceConfigDTO.FeaturesConfig.builder()
                        .faceRecognition(true)
                        .accessControl(true)
                        .timetableSync(true)
                        .otaUpdate(true)
                        .build())
                .server(xyz.jasenon.lab.class_time_table.dto.DeviceConfigDTO.ServerConfig.builder()
                        .host("192.168.10.100")
                        .port(9999)
                        .build())
                .build();
    }
}

