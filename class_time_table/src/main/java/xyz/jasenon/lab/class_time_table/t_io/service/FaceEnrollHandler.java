package xyz.jasenon.lab.class_time_table.t_io.service;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.tio.core.ChannelContext;
import xyz.jasenon.lab.class_time_table.dto.FaceSendAckPayload;
import xyz.jasenon.lab.class_time_table.service.FaceEnrollService;
import xyz.jasenon.lab.class_time_table.t_io.protocol.SmartBoardPacket;

import java.nio.charset.StandardCharsets;

/**
 * 人脸下发处理器（仅处理 Client 回复）
 *
 * <p>协议约定：send = Server 发出，send_ack = Client 回复。
 * <ul>
 *   <li><b>FACE_SEND (0x20)</b>：Server 发出，payload = {@link xyz.jasenon.lab.class_time_table.dto.FaceSendPayload}，
 *       向班牌下发一条人脸，班牌插入本地特征库。</li>
 *   <li><b>FACE_SEND_ACK (0x21)</b>：Client 回复，payload = {@link FaceSendAckPayload}，
 *       班牌插入成功/失败后回传 requestId、success、faceId、errorMessage，用于完成 Web 请求的 future，
 *       使 Web 端一次请求内得知插入结果。</li>
 * </ul>
 *
 * <p>本类只处理 <b>FACE_SEND_ACK</b>（Client 回复）。Server 发出 FACE_SEND 由 {@link FaceEnrollService} 构造并下发。
 */
@Slf4j
@Component
public class FaceEnrollHandler {

    private final FaceEnrollService faceEnrollService;

    /** 使用 @Lazy 打破与 FaceEnrollService 的循环依赖：TioServerConfig -> SmartBoardTioHandler -> FaceEnrollHandler -> FaceEnrollService -> TioServer */
    public FaceEnrollHandler(@Lazy FaceEnrollService faceEnrollService) {
        this.faceEnrollService = faceEnrollService;
    }

    /**
     * 处理班牌回复的 FACE_SEND_ACK，完成对应 Web 请求的 future
     *
     * <p>当班牌收到 FACE_SEND 后，在本地特征库插入一条新特征（成功或失败），
     * 都会回复 FACE_SEND_ACK，payload 为 JSON：requestId、success、faceId、errorMessage。
     * 本方法解析 payload 为 {@link FaceSendAckPayload}，根据 requestId 完成 {@link FaceEnrollService}
     * 中等待的 future，从而 Web 端一次请求即可得到插入结果。
     *
     * @param packet         FACE_SEND_ACK 包，body 为 FaceSendAckPayload 的 JSON
     * @param channelContext 班牌对应的 Channel 上下文
     */
    public void handleFaceSendAck(SmartBoardPacket packet, ChannelContext channelContext) {
        byte[] payload = packet.getPayload();
        if (payload == null || payload.length == 0) {
            return;
        }
        try {
            String json = new String(payload, StandardCharsets.UTF_8);
            FaceSendAckPayload result = JSON.parseObject(json, FaceSendAckPayload.class);
            if (result != null && result.getRequestId() != null) {
                faceEnrollService.completeResult(result.getRequestId(), result);
                log.info("班牌录入结果已回传(FACE_SEND_ACK), requestId={}, success={}, faceId={}",
                        result.getRequestId(), result.getSuccess(), result.getFaceId());
            }
        } catch (Exception e) {
            log.warn("解析 FACE_SEND_ACK 失败", e);
        }
    }
}
