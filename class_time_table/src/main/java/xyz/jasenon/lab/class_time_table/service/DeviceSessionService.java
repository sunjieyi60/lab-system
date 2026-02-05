package xyz.jasenon.lab.class_time_table.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.tio.core.ChannelContext;
import xyz.jasenon.lab.class_time_table.t_io.protocol.SmartBoardPacket;


/**
 * 设备会话管理服务
 * 维护DeviceId与Channel的映射关系，管理设备在线状态
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceSessionService {
    
    private final RedissonClient redissonClient;
    
    private static final String DEVICE_CHANNEL_KEY_PREFIX = "smartboard:device:channel:";
    private static final String CHANNEL_DEVICE_KEY_PREFIX = "smartboard:channel:device:";
    private static final String DEVICE_ONLINE_KEY_PREFIX = "smartboard:device:online:";
    
    /** 设备会话过期时间（秒），心跳会续期 */
    private static final long SESSION_EXPIRE_SECONDS = 120;
    
    /**
     * 绑定设备与通道
     * 
     * @param deviceId 设备ID
     * @param channelContext 通道上下文
     */
    public void bindDeviceChannel(String deviceId, ChannelContext channelContext) {
        String channelId = channelContext.getId();
        
        // 设备ID -> Channel映射
        String deviceChannelKey = DEVICE_CHANNEL_KEY_PREFIX + deviceId;
        RMap<String, String> deviceChannelMap = redissonClient.getMap(deviceChannelKey);
        deviceChannelMap.put("channelId", channelId);
        deviceChannelMap.put("ip", channelContext.getClientNode().getIp());
        deviceChannelMap.put("port", String.valueOf(channelContext.getClientNode().getPort()));
        deviceChannelMap.expire(java.time.Duration.ofSeconds(SESSION_EXPIRE_SECONDS));
        
        // Channel -> 设备ID映射（反向查找）
        String channelDeviceKey = CHANNEL_DEVICE_KEY_PREFIX + channelId;
        RMap<String, String> channelDeviceMap = redissonClient.getMap(channelDeviceKey);
        channelDeviceMap.put("deviceId", deviceId);
        channelDeviceMap.expire(java.time.Duration.ofSeconds(SESSION_EXPIRE_SECONDS));
        
        // 设备在线状态
        String onlineKey = DEVICE_ONLINE_KEY_PREFIX + deviceId;
        RBucket<String> onlineBucket = redissonClient.getBucket(onlineKey);
        onlineBucket.set("ONLINE");
        onlineBucket.expire(java.time.Duration.ofSeconds(SESSION_EXPIRE_SECONDS));
        
        log.info("设备{}已绑定通道: {}", deviceId, channelId);
    }
    
    /**
     * 获取设备对应的通道ID
     * 
     * @param deviceId 设备ID
     * @return 通道ID，如果设备不在线则返回null
     */
    public String getChannelIdByDeviceId(String deviceId) {
        String deviceChannelKey = DEVICE_CHANNEL_KEY_PREFIX + deviceId;
        RMap<String, String> deviceChannelMap = redissonClient.getMap(deviceChannelKey);
        return deviceChannelMap.get("channelId");
    }
    
    /**
     * 根据通道ID获取设备ID
     * 
     * @param channelContext 通道上下文
     * @return 设备ID，如果未绑定则返回null
     */
    public String getDeviceIdByChannel(ChannelContext channelContext) {
        String channelId = channelContext.getId();
        String channelDeviceKey = CHANNEL_DEVICE_KEY_PREFIX + channelId;
        RMap<String, String> channelDeviceMap = redissonClient.getMap(channelDeviceKey);
        return channelDeviceMap.get("deviceId");
    }
    
    /**
     * 检查设备是否在线
     * 
     * @param deviceId 设备ID
     * @return 是否在线
     */
    public boolean isDeviceOnline(String deviceId) {
        String onlineKey = DEVICE_ONLINE_KEY_PREFIX + deviceId;
        return redissonClient.getBucket(onlineKey).isExists();
    }
    
    /**
     * 续期设备会话（心跳时调用）
     * 
     * @param deviceId 设备ID
     */
    public void renewDeviceSession(String deviceId) {
        if (deviceId == null) {
            return;
        }
        
        String deviceChannelKey = DEVICE_CHANNEL_KEY_PREFIX + deviceId;
        String channelDeviceKey = CHANNEL_DEVICE_KEY_PREFIX + getChannelIdByDeviceId(deviceId);
        String onlineKey = DEVICE_ONLINE_KEY_PREFIX + deviceId;
        
        // 续期所有相关key
        RMap<String, String> deviceChannelMap = redissonClient.getMap(deviceChannelKey);
        if (deviceChannelMap.isExists()) {
            deviceChannelMap.expire(java.time.Duration.ofSeconds(SESSION_EXPIRE_SECONDS));
        }
        if (channelDeviceKey != null) {
            RMap<String, String> channelDeviceMap = redissonClient.getMap(channelDeviceKey);
            if (channelDeviceMap.isExists()) {
                channelDeviceMap.expire(java.time.Duration.ofSeconds(SESSION_EXPIRE_SECONDS));
            }
        }
        RBucket<String> onlineBucket = redissonClient.getBucket(onlineKey);
        if (onlineBucket.isExists()) {
            onlineBucket.expire(java.time.Duration.ofSeconds(SESSION_EXPIRE_SECONDS));
        }
    }
    
    /**
     * 解绑设备与通道（设备断开连接时调用）
     * 
     * @param channelContext 通道上下文
     */
    public void unbindDeviceChannel(ChannelContext channelContext) {
        String channelId = channelContext.getId();
        String deviceId = getDeviceIdByChannel(channelContext);
        
        if (deviceId != null) {
            // 删除设备 -> Channel映射
            String deviceChannelKey = DEVICE_CHANNEL_KEY_PREFIX + deviceId;
            redissonClient.getMap(deviceChannelKey).delete();
            
            // 删除设备在线状态
            String onlineKey = DEVICE_ONLINE_KEY_PREFIX + deviceId;
            redissonClient.getBucket(onlineKey).delete();
            
            log.info("设备{}已解绑通道: {}", deviceId, channelId);
        }
        
        // 删除Channel -> 设备ID映射
        String channelDeviceKey = CHANNEL_DEVICE_KEY_PREFIX + channelId;
        redissonClient.getMap(channelDeviceKey).delete();
    }
    
    /**
     * 向指定设备推送消息
     * 
     * @param deviceId 设备ID
     * @param packet 数据包
     * @return 是否推送成功（设备在线且推送成功返回true）
     */
    public boolean pushToDevice(String deviceId, SmartBoardPacket packet) {
        String channelId = getChannelIdByDeviceId(deviceId);
        
        if (channelId == null) {
            log.warn("设备{}不在线，无法推送消息", deviceId);
            return false;
        }
        
        // 通过channelId获取ChannelContext（需要从TioServer获取）
        // 这里简化处理，实际需要通过TioServer的GroupContext获取
        // 暂时返回false，后续在MessageDispatcher中实现
        return false;
    }
    
    /**
     * 获取设备在线状态信息
     * 
     * @param deviceId 设备ID
     * @return 在线状态信息
     */
    public DeviceOnlineInfo getDeviceOnlineInfo(String deviceId) {
        String deviceChannelKey = DEVICE_CHANNEL_KEY_PREFIX + deviceId;
        RMap<String, String> deviceChannelMap = redissonClient.getMap(deviceChannelKey);
        
        if (!deviceChannelMap.isExists()) {
            return DeviceOnlineInfo.offline(deviceId);
        }
        
        String channelId = deviceChannelMap.get("channelId");
        String ip = deviceChannelMap.get("ip");
        String port = deviceChannelMap.get("port");
        
        return DeviceOnlineInfo.online(deviceId, channelId, ip, port);
    }
    
    /**
     * 设备在线信息
     */
    public static class DeviceOnlineInfo {
        private final String deviceId;
        private final boolean online;
        private final String channelId;
        private final String ip;
        private final String port;
        
        private DeviceOnlineInfo(String deviceId, boolean online, String channelId, String ip, String port) {
            this.deviceId = deviceId;
            this.online = online;
            this.channelId = channelId;
            this.ip = ip;
            this.port = port;
        }
        
        public static DeviceOnlineInfo online(String deviceId, String channelId, String ip, String port) {
            return new DeviceOnlineInfo(deviceId, true, channelId, ip, port);
        }
        
        public static DeviceOnlineInfo offline(String deviceId) {
            return new DeviceOnlineInfo(deviceId, false, null, null, null);
        }
        
        // Getters
        public String getDeviceId() { return deviceId; }
        public boolean isOnline() { return online; }
        public String getChannelId() { return channelId; }
        public String getIp() { return ip; }
        public String getPort() { return port; }
    }
}

