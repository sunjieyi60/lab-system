package xyz.jasenon.lab.tio.client.util;

import com.alibaba.fastjson2.JSON;
import xyz.jasenon.lab.tio.client.dto.DeviceInfoDTO;
import xyz.jasenon.lab.tio.client.dto.RegisterRequestDTO;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 设备加密工具类（客户端使用）
 * 使用服务端RSA公钥加密RegisterRequestDTO序列化后的JSON字符串
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
public class DeviceEncryptionUtil {
    
    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    
    /**
     * 加密注册请求
     * RegisterRequestDTO序列化为JSON字符串，然后用服务端RSA公钥加密
     * 
     * @param deviceInfo 设备信息DTO
     * @param serverPublicKeyBase64 服务端公钥（Base64编码）
     * @return 加密后的payload（byte[]），直接作为packet的payload
     */
    public static byte[] encryptRegisterRequest(DeviceInfoDTO deviceInfo, String serverPublicKeyBase64) throws Exception {
        // 1. 构建RegisterRequestDTO
        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setDeviceInfo(deviceInfo);
        
        // 2. 序列化为JSON字符串
        String registerRequestJson = JSON.toJSONString(registerRequest);
        byte[] registerRequestBytes = registerRequestJson.getBytes(StandardCharsets.UTF_8);
        
        // 3. 加载服务端公钥
        byte[] publicKeyBytes = Base64.getDecoder().decode(serverPublicKeyBase64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        PublicKey serverPublicKey = keyFactory.generatePublic(keySpec);
        
        // 4. 使用RSA公钥加密（注意：RSA有长度限制，如果payload较大需要分片）
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
        
        // RSA加密（2048位密钥最多加密245字节）
        // 如果payload较大，需要分片加密
        byte[] encryptedBytes;
        if (registerRequestBytes.length <= 245) {
            // 单次加密
            encryptedBytes = cipher.doFinal(registerRequestBytes);
        } else {
            // 分片加密（每245字节一片）
            int blockSize = 245;
            int offset = 0;
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            while (offset < registerRequestBytes.length) {
                int length = Math.min(blockSize, registerRequestBytes.length - offset);
                byte[] block = new byte[length];
                System.arraycopy(registerRequestBytes, offset, block, 0, length);
                byte[] encryptedBlock = cipher.doFinal(block);
                outputStream.write(encryptedBlock);
                offset += length;
            }
            encryptedBytes = outputStream.toByteArray();
        }
        
        // 5. 返回加密后的byte[]，直接作为packet的payload
        return encryptedBytes;
    }
    
    /**
     * 验证服务端签名
     * 服务端回复时用私钥签名payload，客户端用公钥验证
     * 
     * @param payload 原始payload（RegisterAckDTO的JSON字符串）
     * @param signatureBase64 签名（Base64编码）
     * @param serverPublicKeyBase64 服务端公钥（Base64编码）
     * @return 验证是否通过
     */
    public static boolean verifySignature(String payload, String signatureBase64, String serverPublicKeyBase64) {
        try {
            // 1. 加载服务端公钥
            byte[] publicKeyBytes = Base64.getDecoder().decode(serverPublicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PublicKey serverPublicKey = keyFactory.generatePublic(keySpec);
            
            // 2. 解码签名
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            
            // 3. 验证签名
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(serverPublicKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }
}
