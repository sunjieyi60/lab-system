package xyz.jasenon.lab.class_time_table.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备实体
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Data
@TableName("smart_board_device")
public class Device {
    
    /**
     * 设备ID（唯一标识）
     */
    @TableId(type = IdType.INPUT)
    private String deviceId;
    
    /**
     * 设备名称
     */
    private String deviceName;
    
    /**
     * 设备MAC地址
     */
    private String macAddress;
    
    /**
     * 设备IP地址
     */
    private String ipAddress;
    
    /**
     * 设备类型（如：smartboard, gateway等）
     */
    private String deviceType;
    
    /**
     * 硬件信息（JSON字符串）
     */
    private String hardwareInfo;
    
    /**
     * 设备状态：ONLINE, OFFLINE
     */
    private String status;
    
    /**
     * 设备版本号
     */
    private String version;
    
    /**
     * 最后在线时间
     */
    private LocalDateTime lastOnlineTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 设备配置JSON（最新配置）
     */
    private String configJson;
}

