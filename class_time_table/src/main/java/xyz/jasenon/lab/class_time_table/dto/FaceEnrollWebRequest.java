package xyz.jasenon.lab.class_time_table.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

/**
 * Web 人脸下发请求体（JSON）
 *
 * @author Jasenon_ce
 * @date 2026/2/2
 */
@Data
public class FaceEnrollWebRequest {

    @JSONField(name = "device_id")
    private String deviceId;

    @JSONField(name = "face_id")
    private String faceId;

    /** 人脸名称（对应 FaceFeature 的 tag 字段） */
    @JSONField(name = "tag")
    private String tag;

    @JSONField(name = "group")
    private String group;

    @JSONField(name = "image_base64")
    private String imageBase64;
}
