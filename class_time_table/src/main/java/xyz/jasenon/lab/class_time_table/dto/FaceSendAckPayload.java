package xyz.jasenon.lab.class_time_table.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FACE_SEND_ACK 包体（Client 回复 -> Server）
 * 班牌本地特征库插入结果，用于完成 Web 请求的 future，供 Web 一次请求内得知插入结果。
 *
 * @author Jasenon_ce
 * @date 2026/2/2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceSendAckPayload {

    @JSONField(name = "request_id")
    private String requestId;

    @JSONField(name = "success")
    private Boolean success;

    @JSONField(name = "face_id")
    private String faceId;

    @JSONField(name = "error_message")
    private String errorMessage;
}
