package xyz.jasenon.lab.class_time_table.service;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tio.core.Tio;
import org.tio.server.TioServer;
import xyz.jasenon.lab.class_time_table.dto.FaceSendAckPayload;
import xyz.jasenon.lab.class_time_table.dto.FaceSendPayload;
import xyz.jasenon.lab.class_time_table.t_io.protocol.CommandType;
import xyz.jasenon.lab.class_time_table.t_io.protocol.PacketBuilder;
import xyz.jasenon.lab.class_time_table.t_io.protocol.QosLevel;
import xyz.jasenon.lab.class_time_table.t_io.protocol.SmartBoardPacket;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 人脸录入服务：Web 下发 -> Server 推送到设备 -> 设备回传结果 -> Server 反馈 Web
 *
 * @author Jasenon_ce
 * @date 2026/2/2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FaceEnrollService {

    private final TioServer tioServer;
    private final PacketBuilder packetBuilder = new PacketBuilder();

    /** requestId -> CompletableFuture，Client 回复 FACE_SEND_ACK 时完成 */
    private final ConcurrentHashMap<String, CompletableFuture<FaceSendAckPayload>> pending = new ConcurrentHashMap<>();

    private static final int PUSH_TIMEOUT_SECONDS = 30;

    /**
     * Web 下发人脸到指定设备，阻塞等待设备录入结果后返回（供 Web 反馈）
     *
     * @param deviceId  设备 ID（bsId）
     * @param faceId    人脸 ID
     * @param tag       人脸名称（对应 FaceFeature 的 tag 字段）
     * @param group     分组
     * @param imageBase64 图片 Base64
     * @return 设备录入结果，超时或设备不在线时返回失败
     */
    public FaceSendAckPayload pushToDeviceAndWait(String deviceId, String faceId, String tag, String group, String imageBase64) {
        String requestId = UUID.randomUUID().toString();
        CompletableFuture<FaceSendAckPayload> future = new CompletableFuture<>();
        pending.put(requestId, future);

        FaceSendPayload payload = new FaceSendPayload();
        payload.setRequestId(requestId);
        payload.setFaceId(faceId);
        payload.setTag(tag != null ? tag : "");
        payload.setGroup(group != null ? group : "default_group");
        payload.setImageBase64(imageBase64);

        String json = JSON.toJSONString(payload);
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        SmartBoardPacket packet = packetBuilder.build(CommandType.FACE_SEND, body, QosLevel.AT_LEAST_ONCE);

        try {
            // deviceId 即注册时 Tio.bindBsId 绑定的 bsId；若设备不在线则无 channel 绑定，sendToBsId 不发送，后续 future.get() 会超时
            Tio.sendToBsId(tioServer.getTioServerConfig(), deviceId, packet);
            log.info("人脸已推送到设备, requestId={}, deviceId={}, faceId={}", requestId, deviceId, faceId);
            return future.get(PUSH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("等待设备录入结果超时, requestId={}, deviceId={}", requestId, deviceId);
            pending.remove(requestId);
            return FaceSendAckPayload.builder()
                    .requestId(requestId)
                    .success(false)
                    .faceId(faceId)
                    .errorMessage("设备响应超时")
                    .build();
        } catch (Exception e) {
            log.error("推送人脸到设备失败, deviceId={}, faceId={}", deviceId, faceId, e);
            pending.remove(requestId);
            return FaceSendAckPayload.builder()
                    .requestId(requestId)
                    .success(false)
                    .faceId(faceId)
                    .errorMessage("推送失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Client 回复 FACE_SEND_ACK 时调用，完成对应 Web 请求的 future
     */
    public void completeResult(String requestId, FaceSendAckPayload result) {
        CompletableFuture<FaceSendAckPayload> future = pending.remove(requestId);
        if (future != null) {
            future.complete(result);
        } else {
            log.warn("收到未知 requestId 的录入结果, requestId={}", requestId);
        }
    }
}
