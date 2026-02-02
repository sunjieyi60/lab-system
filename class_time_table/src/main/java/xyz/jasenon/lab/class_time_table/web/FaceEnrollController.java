package xyz.jasenon.lab.class_time_table.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.class_time_table.dto.FaceEnrollWebDTO;
import xyz.jasenon.lab.class_time_table.dto.FaceSendAckPayload;
import xyz.jasenon.lab.class_time_table.service.FaceEnrollService;

/**
 * Web 人脸下发 API：Web 调用 -> Server 发 FACE_SEND 到设备 -> 设备插入本地特征库后回复 FACE_SEND_ACK -> 一次请求内返回插入结果
 *
 * @author Jasenon_ce
 * @date 2026/2/2
 */
@RestController
@RequestMapping("/api/face")
@RequiredArgsConstructor
public class FaceEnrollController {

    private final FaceEnrollService faceEnrollService;

    /**
     * 下发一条人脸到指定班牌本地特征库，阻塞等待班牌回复 FACE_SEND_ACK 后返回插入结果
     * 请求体 JSON：deviceId、faceId、tag（人脸名称）、group、imageBase64
     */
    @PostMapping("/enroll")
    public ResponseEntity<FaceSendAckPayload> enroll(@RequestBody FaceEnrollWebDTO body) {
        if (body == null || body.getDeviceId() == null || body.getFaceId() == null) {
            return ResponseEntity.badRequest().body(FaceSendAckPayload.builder()
                    .success(false)
                    .errorMessage("deviceId、faceId 必填")
                    .build());
        }
        FaceSendAckPayload result = faceEnrollService.pushToDeviceAndWait(
                body.getDeviceId(),
                body.getFaceId(),
                body.getTag(),
                body.getGroup(),
                body.getImageBase64());
        return ResponseEntity.ok(result);
    }
}
