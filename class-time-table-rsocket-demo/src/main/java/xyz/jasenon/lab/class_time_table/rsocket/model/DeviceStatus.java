package xyz.jasenon.lab.class_time_table.rsocket.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 班牌设备状态
 */
@Data
@Builder
public class DeviceStatus implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 设备 UUID
     */
    private String uuid;
    
    /**
     * 设备名称
     */
    private String deviceName;
    
    /**
     * 在线状态
     */
    private Status status;
    
    /**
     * 终端类型（android、linux 等）
     */
    private String terminal;
    
    /**
     * IP 地址
     */
    private String ipAddress;
    
    /**
     * 当前课表 ID
     */
    private String currentCourseId;
    
    /**
     * 当前教室/实验室
     */
    private String roomId;
    
    /**
     * 上报时间
     */
    private Instant reportTime;
    
    /**
     * 附加信息（电量、信号强度等）
     */
    private String extraInfo;
    
    public enum Status {
        ONLINE,      // 在线
        OFFLINE,     // 离线
        BUSY,        // 忙碌（处理任务中）
        ERROR        // 错误状态
    }
}
