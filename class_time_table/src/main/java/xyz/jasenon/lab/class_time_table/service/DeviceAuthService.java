package xyz.jasenon.lab.class_time_table.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 设备身份验证服务
 * 使用RSA公私钥对进行设备身份验证
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Slf4j
@Service
public class DeviceAuthService {
    
    private static final String ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    
    /**
     * 验证设备签名
     * 
     * @param deviceId 设备ID
     * @param data 原始数据
     * @param signature 签名（Base64编码）
     * @param publicKeyStr 公钥（Base64编码）
     * @return 验证是否通过
     */
    public boolean verifySignature(String deviceId, byte[] data, String signature, String publicKeyStr) {
        try {
            // 解码公钥
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyStr);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            
            // 解码签名
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            
            // 验证签名
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(data);
            
            boolean verified = sig.verify(signatureBytes);
            log.debug("设备{}签名验证结果: {}", deviceId, verified);
            return verified;
        } catch (Exception e) {
            log.error("设备{}签名验证失败", deviceId, e);
            return false;
        }
    }
    
    // 注意：此服务已迁移到DeviceEncryptionService，保留此类用于其他签名验证场景
}

