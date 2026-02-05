package xyz.jasenon.lab.class_time_table.service;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.class_time_table.dto.DeviceInfoDTO;
import xyz.jasenon.lab.class_time_table.dto.RegisterRequestDTO;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 设备加密服务
 * 注册阶段：RSA加密AES密钥交换，AES加密设备信息
 * 后续通信：使用交换的AES密钥加密payload
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Slf4j
@Service
public class DeviceEncryptionService {
    
    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    
    /** 服务端RSA私钥（用于解密AES密钥） */
    private PrivateKey serverPrivateKey;
    
    /** 存储设备ID到AES密钥的映射（注册成功后） */
    private final ConcurrentMap<String, SecretKey> deviceAesKeys = new ConcurrentHashMap<>();
    
    /** 存储已使用的nonce（防重放攻击，5分钟过期） */
    private final ConcurrentMap<String, Long> usedNonces = new ConcurrentHashMap<>();
    
    /**
     * 构造函数，加载服务端私钥
     */
    public DeviceEncryptionService(
            @Value("${device.auth.server.private-key:}") String privateKeyBase64,
            @Value("${device.auth.server.private-key-file:}") Resource privateKeyResource) {
        try {
            if (privateKeyBase64 != null && !privateKeyBase64.isEmpty()) {
                loadPrivateKeyFromBase64(privateKeyBase64);
            } else if (privateKeyResource != null && privateKeyResource.exists()) {
                loadPrivateKeyFromFile(privateKeyResource);
            } else {
                log.warn("服务端私钥未配置：private-key-file 指向的资源不存在或不可读，path={}，设备加密验证功能将不可用", privateKeyResource);
            }
        } catch (Exception e) {
            log.error("加载服务端私钥失败", e);
        }
    }
    
    /**
     * 从Base64字符串加载私钥
     */
    private void loadPrivateKeyFromBase64(String privateKeyBase64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        serverPrivateKey = keyFactory.generatePrivate(keySpec);
        log.info("服务端私钥加载成功（从配置）");
    }
    
    /**
     * 从文件加载私钥
     */
    private void loadPrivateKeyFromFile(Resource resource) throws Exception {
        String keyContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        keyContent = keyContent.replace("-----BEGIN PRIVATE KEY-----", "")
                               .replace("-----END PRIVATE KEY-----", "")
                               .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        serverPrivateKey = keyFactory.generatePrivate(keySpec);
        log.info("服务端私钥加载成功（从文件）");
    }
    
    /**
     * 解密注册请求（混合加密）
     * 1. 使用RSA私钥解密AES密钥
     * 2. 使用AES密钥解密设备信息
     * 3. 验证timestamp和nonce防重放攻击
     * 
     * @param registerRequest 注册请求DTO
     * @return 解密后的设备信息DTO，如果解密失败返回null
     */
    public DecryptResult decryptRegisterRequest(RegisterRequestDTO registerRequest) {
        try {
            if (serverPrivateKey == null) {
                log.error("服务端私钥未配置，无法解密设备信息");
                return DecryptResult.failed("服务端私钥未配置");
            }
            
            if (registerRequest == null) {
                return DecryptResult.failed("注册请求为空");
            }
            
            // 1. 验证timestamp（防重放攻击，允许5分钟误差）
            Long timestamp = registerRequest.getTimestamp();
            if (timestamp == null) {
                return DecryptResult.failed("时间戳为空");
            }
            
            long currentTime = System.currentTimeMillis();
            long timeDiff = Math.abs(currentTime - timestamp);
            if (timeDiff > 5 * 60 * 1000) { // 5分钟
                log.warn("注册请求时间戳过期，时间差: {}ms", timeDiff);
                return DecryptResult.failed("时间戳过期");
            }
            
            // 2. 验证nonce（防重放攻击）
            String nonce = registerRequest.getNonce();
            if (nonce == null || nonce.isEmpty()) {
                return DecryptResult.failed("nonce为空");
            }
            
            // 检查nonce是否已使用
            Long usedTime = usedNonces.get(nonce);
            if (usedTime != null) {
                log.warn("检测到重放攻击，nonce已使用: {}", nonce);
                return DecryptResult.failed("检测到重放攻击");
            }
            
            // 3. 使用RSA私钥解密AES密钥
            byte[] encryptedAesKeyBytes = Base64.getDecoder().decode(registerRequest.getEncryptedAesKey());
            Cipher rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION);
            rsaCipher.init(Cipher.DECRYPT_MODE, serverPrivateKey);
            byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKeyBytes);
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);
            
            // 4. 使用AES密钥解密设备信息
            byte[] encryptedDeviceInfoBytes = Base64.getDecoder().decode(registerRequest.getEncryptedDeviceInfo());
            byte[] ivBytes = Base64.getDecoder().decode(registerRequest.getIv());
            
            Cipher aesCipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
            byte[] decryptedBytes = aesCipher.doFinal(encryptedDeviceInfoBytes);
            
            // 5. 解析设备信息JSON
            String deviceInfoJson = new String(decryptedBytes, StandardCharsets.UTF_8);
            DeviceInfoDTO deviceInfo = JSON.parseObject(deviceInfoJson, DeviceInfoDTO.class);
            
            if (deviceInfo == null) {
                return DecryptResult.failed("设备信息解析失败");
            }
            // deviceId 可为空，由服务端在注册时分配
            
            // 6. 记录已使用的nonce（5分钟后自动清理）
            usedNonces.put(nonce, currentTime);
            // 异步清理过期nonce（简化处理，实际可以用定时任务）
            cleanupExpiredNonces();
            
            log.debug("设备信息解密成功: deviceId={}", deviceInfo.getDeviceId());
            return DecryptResult.success(deviceInfo, aesKey);
            
        } catch (Exception e) {
            log.error("解密注册请求失败", e);
            return DecryptResult.failed("解密失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存设备的AES密钥（注册成功后）
     */
    public void saveDeviceAesKey(String deviceId, SecretKey aesKey) {
        deviceAesKeys.put(deviceId, aesKey);
        log.debug("设备{}的AES密钥已保存", deviceId);
    }
    
    /**
     * 获取设备的AES密钥（后续通信使用）
     */
    public SecretKey getDeviceAesKey(String deviceId) {
        return deviceAesKeys.get(deviceId);
    }
    
    /**
     * 使用AES密钥解密payload（后续通信）
     */
    public byte[] decryptPayload(byte[] encryptedPayload, byte[] iv, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
        return cipher.doFinal(encryptedPayload);
    }
    
    /**
     * 使用AES密钥加密payload（后续通信）
     */
    public EncryptResult encryptPayload(byte[] payload, SecretKey aesKey) throws Exception {
        // 生成随机IV
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[GCM_IV_LENGTH];
        random.nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        byte[] encrypted = cipher.doFinal(payload);
        
        return new EncryptResult(encrypted, iv);
    }
    
    /**
     * 使用私钥签名数据
     */
    public byte[] signData(byte[] data) {
        try {
            if (serverPrivateKey == null) {
                log.error("服务端私钥未配置，无法签名");
                return null;
            }
            
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(serverPrivateKey);
            signature.update(data);
            return signature.sign();
        } catch (Exception e) {
            log.error("签名数据失败", e);
            return null;
        }
    }
    
    /**
     * 清理过期的nonce（5分钟前的）
     */
    private void cleanupExpiredNonces() {
        long currentTime = System.currentTimeMillis();
        long expireTime = 5 * 60 * 1000; // 5分钟
        
        usedNonces.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > expireTime
        );
    }
    
    /**
     * 解密结果
     */
    public static class DecryptResult {
        private final boolean success;
        private final DeviceInfoDTO deviceInfo;
        private final SecretKey aesKey;
        private final String errorMessage;
        
        private DecryptResult(boolean success, DeviceInfoDTO deviceInfo, SecretKey aesKey, String errorMessage) {
            this.success = success;
            this.deviceInfo = deviceInfo;
            this.aesKey = aesKey;
            this.errorMessage = errorMessage;
        }
        
        public static DecryptResult success(DeviceInfoDTO deviceInfo, SecretKey aesKey) {
            return new DecryptResult(true, deviceInfo, aesKey, null);
        }
        
        public static DecryptResult failed(String errorMessage) {
            return new DecryptResult(false, null, null, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public DeviceInfoDTO getDeviceInfo() { return deviceInfo; }
        public SecretKey getAesKey() { return aesKey; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * 加密结果
     */
    public static class EncryptResult {
        private final byte[] encrypted;
        private final byte[] iv;
        
        public EncryptResult(byte[] encrypted, byte[] iv) {
            this.encrypted = encrypted;
            this.iv = iv;
        }
        
        public byte[] getEncrypted() { return encrypted; }
        public byte[] getIv() { return iv; }
    }
}
