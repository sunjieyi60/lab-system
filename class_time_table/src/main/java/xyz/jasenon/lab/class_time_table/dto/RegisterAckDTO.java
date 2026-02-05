package xyz.jasenon.lab.class_time_table.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备注册响应DTO
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterAckDTO {
    
    /**
     * 是否成功
     */
    @JSONField(name = "success")
    private Boolean success;
    
    /**
     * 设备Token（Base64编码），注册成功后返回
     */
    @JSONField(name = "token")
    private String token;
    
    /**
     * 设备配置信息（成功时返回）
     */
    @JSONField(name = "config")
    private DeviceConfigDTO config;
    
    /**
     * 错误信息（失败时返回）
     */
    @JSONField(name = "errorMessage")
    private String errorMessage;
    
    /**
     * 错误代码（失败时返回）
     */
    @JSONField(name = "errorCode")
    private String errorCode;
    
    /**
     * 创建成功响应
     * 注意：token可以为null，因为使用ChannelContext的bindId进行身份验证
     */
    public static RegisterAckDTO success(String token, DeviceConfigDTO config) {
        return RegisterAckDTO.builder()
                .success(true)
                .token(token) // 可以为null
                .config(config)
                .build();
    }
    
    /**
     * 创建失败响应
     */
    public static RegisterAckDTO failed(String errorCode, String errorMessage) {
        return RegisterAckDTO.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}

