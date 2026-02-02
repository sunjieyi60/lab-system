package xyz.jasenon.lab.class_time_table.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import xyz.jasenon.lab.class_time_table.dto.DeviceInfoDTO;
import xyz.jasenon.lab.class_time_table.dto.RegisterRequestDTO;
import xyz.jasenon.lab.class_time_table.entity.Device;

import javax.crypto.SecretKey;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 设备注册服务
 * 处理设备注册逻辑：解密设备信息 -> 写入Redis -> 绑定ChannelContext
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceRegisterService {
    
    private final DeviceEncryptionService deviceEncryptionService;
    private final RedissonClient redissonClient;
    
    private static final String DEVICE_REGISTER_KEY_PREFIX = "smartboard:device:register:";
    private static final long REGISTER_EXPIRE_SECONDS = 86400; // 24小时过期
    private final String groupId = "smartboard";
    
    /**
     * 注册设备（解密并写入Redis，绑定ChannelContext，保存AES密钥）
     * 
     * @param registerRequest 注册请求DTO
     * @param channelContext 通道上下文
     * @return 注册结果，包含设备信息和AES密钥
     */
    public RegisterResult registerDevice(RegisterRequestDTO registerRequest, ChannelContext channelContext) {
        try {
            // 1. 解密注册请求（混合加密：RSA解密AES密钥，AES解密设备信息）
            DeviceEncryptionService.DecryptResult decryptResult = 
                deviceEncryptionService.decryptRegisterRequest(registerRequest);
            
            if (!decryptResult.isSuccess()) {
                return RegisterResult.failed("DECRYPT_FAILED", decryptResult.getErrorMessage());
            }
            
            DeviceInfoDTO deviceInfo = decryptResult.getDeviceInfo();
            SecretKey aesKey = decryptResult.getAesKey();
            
            // 客户端未绑定设备时 deviceId 为空，由服务端分配
            String deviceId = deviceInfo.getDeviceId();
            if (deviceId == null || deviceId.isBlank()) {
                deviceId = UUID.randomUUID().toString();
                deviceInfo.setDeviceId(deviceId);
                log.info("设备未携带 deviceId，服务端分配: {}", deviceId);
            }
            
            log.info("设备{}解密成功，开始注册", deviceId);
            
            // 2. 构建设备实体（用于Redis和MySQL）
            Device device = new Device();
            device.setDeviceId(deviceId);
            device.setDeviceName(deviceInfo.getDeviceName());
            device.setMacAddress(deviceInfo.getMacAddress());
            device.setIpAddress(channelContext.getClientNode().getIp());
            device.setStatus("ONLINE");
            device.setLastOnlineTime(LocalDateTime.now());
            device.setCreateTime(LocalDateTime.now());
            device.setUpdateTime(LocalDateTime.now());
            
            // 3. 写入Redis（等待定时任务刷盘到MySQL）
            String registerKey = DEVICE_REGISTER_KEY_PREFIX + deviceId;
            RMap<String, Object> deviceMap = redissonClient.getMap(registerKey);
            deviceMap.put("deviceId", device.getDeviceId());
            deviceMap.put("deviceName", device.getDeviceName());
            deviceMap.put("macAddress", device.getMacAddress());
            deviceMap.put("version", device.getVersion());
            deviceMap.put("ipAddress", device.getIpAddress());
            deviceMap.put("deviceType", device.getDeviceType());
            deviceMap.put("hardwareInfo", device.getHardwareInfo());
            deviceMap.put("status", device.getStatus());
            deviceMap.put("lastOnlineTime", device.getLastOnlineTime().toString());
            deviceMap.put("createTime", device.getCreateTime().toString());
            deviceMap.put("updateTime", device.getUpdateTime().toString());
            deviceMap.expire(java.time.Duration.ofSeconds(REGISTER_EXPIRE_SECONDS));
            
            // 4. 绑定ChannelContext的bindId（关键：后续通过bindId验证设备合法性）
            Tio.bindBsId(channelContext, deviceId);
            Tio.bindGroup(channelContext, groupId);
            
            // 5. 保存设备的AES密钥（用于后续通信）
            deviceEncryptionService.saveDeviceAesKey(deviceId, aesKey);
            
            log.info("设备{}注册成功，已写入Redis、绑定ChannelContext并保存AES密钥，bindId={}", deviceId, deviceId);
            
            return RegisterResult.success(deviceInfo, device, aesKey);
            
        } catch (Exception e) {
            log.error("设备注册失败", e);
            return RegisterResult.failed("INTERNAL_ERROR", "服务器内部错误: " + e.getMessage());
        }
    }
    
    /**
     * 验证设备合法性（通过ChannelContext的bindId）
     * 
     * @param channelContext 通道上下文
     * @return 设备ID，如果设备不合法返回null
     */
    public String verifyDevice(ChannelContext channelContext) {
        String bindId = channelContext.getBsId();
        if (bindId == null || bindId.isEmpty()) {
            log.warn("ChannelContext未绑定设备ID，来自: {}", channelContext.getClientNode());
            return null;
        }
        
        // 检查Redis中是否存在该设备
        String registerKey = DEVICE_REGISTER_KEY_PREFIX + bindId;
        RMap<String, Object> deviceMap = redissonClient.getMap(registerKey);
        if (!deviceMap.isExists()) {
            log.warn("设备{}不在Redis中，视为非法用户", bindId);
            return null;
        }
        
        return bindId;
    }
    
    /**
     * 注册结果
     */
    public static class RegisterResult {
        private final boolean success;
        private final DeviceInfoDTO deviceInfo;
        private final Device device;
        private final javax.crypto.SecretKey aesKey;
        private final String errorCode;
        private final String errorMessage;
        
        private RegisterResult(boolean success, DeviceInfoDTO deviceInfo, Device device, 
                              javax.crypto.SecretKey aesKey, String errorCode, String errorMessage) {
            this.success = success;
            this.deviceInfo = deviceInfo;
            this.device = device;
            this.aesKey = aesKey;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
        
        public static RegisterResult success(DeviceInfoDTO deviceInfo, Device device, javax.crypto.SecretKey aesKey) {
            return new RegisterResult(true, deviceInfo, device, aesKey, null, null);
        }
        
        public static RegisterResult failed(String errorCode, String errorMessage) {
            return new RegisterResult(false, null, null, null, errorCode, errorMessage);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public DeviceInfoDTO getDeviceInfo() { return deviceInfo; }
        public Device getDevice() { return device; }
        public javax.crypto.SecretKey getAesKey() { return aesKey; }
        public String getErrorCode() { return errorCode; }
        public String getErrorMessage() { return errorMessage; }
    }
}
