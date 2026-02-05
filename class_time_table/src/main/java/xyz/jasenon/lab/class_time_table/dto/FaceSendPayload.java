package xyz.jasenon.lab.class_time_table.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

/**
 * FACE_SEND 包体（Server 发出 -> Client）
 * 下发一条人脸到班牌本地特征库，tag 即人脸名称（FaceFeature 无 name 字段）。
 *
 * @author Jasenon_ce
 * @date 2026/2/2
 */
@Data
public class FaceSendPayload {

    /** 请求 ID，Server 下发时生成，Client 回复 FACE_SEND_ACK 时带回，用于 Web 一次请求得知插入结果 */
    @JSONField(name = "request_id")
    private String requestId;

    /** 人脸 ID（唯一标识） */
    @JSONField(name = "face_id")
    private String faceId;

    /** 人脸名称（对应 FaceFeature 的 tag 字段） */
    @JSONField(name = "tag")
    private String tag;

    /** 分组（如：default_group） */
    @JSONField(name = "group")
    private String group;

    /** 人脸图片 Base64（JPEG/PNG） */
    @JSONField(name = "image_base64")
    private String imageBase64;
}
