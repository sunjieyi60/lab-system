package xyz.jasenon.lab.class_time_table.t_io.service;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import xyz.jasenon.lab.class_time_table.dto.DeviceConfigDTO;
import xyz.jasenon.lab.class_time_table.dto.RegisterAckDTO;
import xyz.jasenon.lab.class_time_table.dto.RegisterRequestDTO;
import xyz.jasenon.lab.class_time_table.service.DeviceEncryptionService;
import xyz.jasenon.lab.class_time_table.service.DeviceRegisterService;
import xyz.jasenon.lab.class_time_table.service.DeviceService;
import xyz.jasenon.lab.class_time_table.t_io.protocol.CommandType;
import xyz.jasenon.lab.class_time_table.t_io.protocol.PacketBuilder;
import xyz.jasenon.lab.class_time_table.t_io.protocol.QosLevel;
import xyz.jasenon.lab.class_time_table.t_io.protocol.SmartBoardPacket;

import java.nio.charset.StandardCharsets;

/**
 * 设备注册处理器
 * 处理REGISTER指令，验证设备身份并回复REGISTER_ACK
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterHandler {
    
    private final DeviceRegisterService deviceRegisterService;
    private final DeviceService deviceService;
    private final DeviceEncryptionService deviceEncryptionService;
    private final PacketBuilder packetBuilder = new PacketBuilder();
    
    /**
     * 处理设备注册请求
     * 
     * @param packet 注册数据包
     * @param channelContext 通道上下文
     */
    public void handleRegister(SmartBoardPacket packet, ChannelContext channelContext) {
        try {
            byte[] payload = packet.getPayload();
            if (payload == null || payload.length == 0) {
                log.warn("收到空的注册请求，来自channel: {}", channelContext.getClientNode());
                sendRegisterAckError(channelContext, "INVALID_FORMAT", "注册请求为空");
                closeChannel(channelContext);
                return;
            }
            
            // 解析RegisterRequestDTO（JSON格式）
            String payloadStr = new String(payload, StandardCharsets.UTF_8);
            RegisterRequestDTO registerRequest;
            try {
                registerRequest = JSON.parseObject(payloadStr, RegisterRequestDTO.class);
            } catch (Exception e) {
                log.warn("注册请求JSON解析失败，来自channel: {}", channelContext.getClientNode(), e);
                sendRegisterAckError(channelContext, "INVALID_FORMAT", "注册请求格式错误");
                closeChannel(channelContext);
                return;
            }
            
            if (registerRequest == null) {
                log.warn("注册请求为空，来自channel: {}", channelContext.getClientNode());
                sendRegisterAckError(channelContext, "INVALID_FORMAT", "注册请求为空");
                closeChannel(channelContext);
                return;
            }
            
            log.info("收到设备注册请求，来自channel: {}", channelContext.getClientNode());
            
            // 解密并注册设备（混合加密：RSA解密AES密钥，AES解密设备信息）
            // 验证timestamp和nonce防重放攻击
            DeviceRegisterService.RegisterResult registerResult = deviceRegisterService.registerDevice(
                registerRequest,
                channelContext
            );
            
            if (!registerResult.isSuccess()) {
                log.warn("设备注册失败: {} - {}，中断连接", registerResult.getErrorCode(), registerResult.getErrorMessage());
                sendRegisterAckError(channelContext, registerResult.getErrorCode(), registerResult.getErrorMessage());
                closeChannel(channelContext);
                return;
            }
            
            String deviceId = registerResult.getDeviceInfo().getDeviceId();
            log.info("设备{}注册成功，已绑定ChannelContext并保存AES密钥", deviceId);
            
            // 获取设备配置（根据设备类型或默认配置）
            DeviceConfigDTO config = deviceService.getDeviceConfig(deviceId);
            
            // 构建注册成功响应（不需要token，使用ChannelContext的bindId）
            // 注意：AES密钥已在注册时交换，这里不需要再返回
            RegisterAckDTO ackDTO = RegisterAckDTO.success(null, config); // token设为null
            String responseJson = JSON.toJSONString(ackDTO);
            byte[] responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);
            
            // 使用私钥签名payload（保证信息完整性和身份验证）
            byte[] signature = deviceEncryptionService.signData(responseBytes);
            if (signature == null) {
                log.error("签名响应失败，设备: {}", deviceId);
                sendRegisterAckError(channelContext, "INTERNAL_ERROR", "签名失败");
                closeChannel(channelContext);
                return;
            }
            
            // 将签名附加到payload后面（格式：payload + 分隔符 + signature的Base64）
            String signatureBase64 = java.util.Base64.getEncoder().encodeToString(signature);
            String signedPayload = responseJson + "|" + signatureBase64;
            byte[] signedBytes = signedPayload.getBytes(StandardCharsets.UTF_8);
            
            // 创建REGISTER_ACK包
            SmartBoardPacket ackPacket = packetBuilder.build(
                CommandType.REGISTER_ACK, 
                signedBytes, 
                QosLevel.AT_LEAST_ONCE
            );
            
            // 发送响应
            Tio.send(channelContext, ackPacket);
            
            log.info("设备{}注册成功，已发送REGISTER_ACK，双方已切换为AES加密通信", deviceId);
            
        } catch (Exception e) {
            log.error("处理设备注册请求失败", e);
            sendRegisterAckError(channelContext, "INTERNAL_ERROR", "服务器内部错误: " + e.getMessage());
            closeChannel(channelContext);
        }
    }
    
    /**
     * 关闭通道（设备不合法时）
     */
    private void closeChannel(ChannelContext channelContext) {
        try {
            Tio.close(channelContext, "设备注册失败，连接已关闭");
        } catch (Exception e) {
            log.error("关闭通道失败", e);
        }
    }
    
    /**
     * 发送注册失败响应
     * 
     * @param channelContext 通道上下文
     * @param errorCode 错误代码
     * @param errorMessage 错误信息
     */
    private void sendRegisterAckError(ChannelContext channelContext, String errorCode, String errorMessage) {
        try {
            // 构建错误响应JSON
            RegisterAckDTO errorAck = RegisterAckDTO.failed(errorCode, errorMessage);
            String errorJson = JSON.toJSONString(errorAck);
            byte[] errorBytes = errorJson.getBytes(StandardCharsets.UTF_8);
            
            SmartBoardPacket errorPacket = packetBuilder.build(
                CommandType.REGISTER_ACK,
                errorBytes,
                QosLevel.AT_LEAST_ONCE
            );
            
            Tio.send(channelContext, errorPacket);
            log.debug("已发送注册失败响应: {} - {}", errorCode, errorMessage);
        } catch (Exception e) {
            log.error("发送注册失败响应异常", e);
        }
    }
    
    // 注意：新方案不再需要token，使用ChannelContext的bindId进行身份验证
}

