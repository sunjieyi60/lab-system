package xyz.jasenon.lab.class_time_table.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备信息DTO（解密后的设备信息）
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfoDTO {
    
    /**
     * 设备ID（唯一标识）
     */
    @JSONField(name = "device_id")
    private String deviceId;
    
    /**
     * 设备名称
     */
    @JSONField(name = "device_name")
    private String deviceName;
    
    /**
     * IP地址（客户端上报，服务端可优先使用 Channel 的 IP）
     */
    @JSONField(name = "ip_address")
    private String ipAddress;

    /**
     * MAC地址
     */
    @JSONField(name = "mac_address")
    private String macAddress;
    
}

