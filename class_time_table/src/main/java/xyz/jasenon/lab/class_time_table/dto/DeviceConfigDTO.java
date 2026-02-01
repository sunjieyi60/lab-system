package xyz.jasenon.lab.class_time_table.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备配置DTO
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceConfigDTO {
    
    /**
     * 心跳间隔（毫秒）
     */
    @JSONField(name = "heartbeatInterval")
    private Integer heartbeatInterval;
    
    /**
     * 重连延迟（毫秒）
     */
    @JSONField(name = "reconnectDelay")
    private Integer reconnectDelay;
    
    /**
     * 分片大小（字节）
     */
    @JSONField(name = "chunkSize")
    private Integer chunkSize;
    
    /**
     * 功能特性配置
     */
    @JSONField(name = "features")
    private FeaturesConfig features;
    
    /**
     * 服务器配置
     */
    @JSONField(name = "server")
    private ServerConfig server;
    
    /**
     * 功能特性配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeaturesConfig {
        @JSONField(name = "faceRecognition")
        private Boolean faceRecognition;
        
        @JSONField(name = "accessControl")
        private Boolean accessControl;
        
        @JSONField(name = "timetableSync")
        private Boolean timetableSync;
        
        @JSONField(name = "otaUpdate")
        private Boolean otaUpdate;
    }
    
    /**
     * 服务器配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerConfig {
        @JSONField(name = "host")
        private String host;
        
        @JSONField(name = "port")
        private Integer port;
    }
}

