package xyz.jasenon.lab.tio.client.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备配置DTO（与服务端保持一致）
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceConfigDTO {
    
    @JSONField(name = "heartbeatInterval")
    private Integer heartbeatInterval;
    
    @JSONField(name = "reconnectDelay")
    private Integer reconnectDelay;
    
    @JSONField(name = "chunkSize")
    private Integer chunkSize;
    
    @JSONField(name = "features")
    private FeaturesConfig features;
    
    @JSONField(name = "server")
    private ServerConfig server;
    
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

