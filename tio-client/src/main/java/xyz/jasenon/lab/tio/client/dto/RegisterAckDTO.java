package xyz.jasenon.lab.tio.client.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备注册响应DTO（与服务端保持一致）
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterAckDTO {
    
    @JSONField(name = "success")
    private Boolean success;
    
    @JSONField(name = "token")
    private String token;
    
    @JSONField(name = "config")
    private DeviceConfigDTO config;
    
    @JSONField(name = "errorMessage")
    private String errorMessage;
    
    @JSONField(name = "errorCode")
    private String errorCode;
}

