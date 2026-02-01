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
    @JSONField(name = "deviceId")
    private String deviceId;
    
    /**
     * 设备名称
     */
    @JSONField(name = "deviceName")
    private String deviceName;
    
    /**
     * MAC地址
     */
    @JSONField(name = "macAddress")
    private String macAddress;
    
    /**
     * 设备版本号
     */
    @JSONField(name = "version")
    private String version;
    
    /**
     * 设备类型（如：smartboard, gateway等）
     */
    @JSONField(name = "deviceType")
    private String deviceType;
    
    /**
     * 硬件信息（JSON字符串，可选）
     */
    @JSONField(name = "hardwareInfo")
    private String hardwareInfo;
}

