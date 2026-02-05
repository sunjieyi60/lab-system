package xyz.jasenon.lab.class_time_table.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

/**
 * 设备注册请求DTO
 * 使用混合加密：AES加密设备信息，RSA加密AES密钥
 * 包含timestamp和nonce防止重放攻击
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Data
public class RegisterRequestDTO {
    
    /**
     * 加密的设备信息（Base64编码）
     * 使用AES加密DeviceInfoDTO的JSON字符串
     */
    @JSONField(name = "encryptedDeviceInfo")
    private String encryptedDeviceInfo;
    
    /**
     * 加密的AES密钥（Base64编码）
     * 使用服务端RSA公钥加密AES密钥
     */
    @JSONField(name = "encryptedAesKey")
    private String encryptedAesKey;
    
    /**
     * AES加密的初始化向量IV（Base64编码）
     */
    @JSONField(name = "iv")
    private String iv;
    
    /**
     * 时间戳（毫秒），用于防重放攻击
     */
    @JSONField(name = "timestamp")
    private Long timestamp;
    
    /**
     * 随机数（nonce），用于防重放攻击
     */
    @JSONField(name = "nonce")
    private String nonce;
}
