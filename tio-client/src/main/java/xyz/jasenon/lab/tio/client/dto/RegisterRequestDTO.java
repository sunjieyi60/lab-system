package xyz.jasenon.lab.tio.client.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

/**
 * 设备注册请求DTO（与服务端保持一致）
 * 序列化为JSON字符串后，使用服务端RSA公钥加密，作为packet的payload
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Data
public class RegisterRequestDTO {
    
    /**
     * 设备信息
     */
    @JSONField(name = "deviceInfo")
    private DeviceInfoDTO deviceInfo;
}
