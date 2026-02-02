package xyz.jasenon.lab.class_time_table.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

/**
 * Web 人脸下发接口请求体（POST /api/face/enroll）
 * 指定设备与一条人脸信息，Server 通过 FACE_SEND 下发给班牌，班牌插入本地特征库后回复 FACE_SEND_ACK，Web 一次请求内得到插入结果。
 *
 * @author Jasenon_ce
 * @date 2026/2/2
 */
@Data
public class FaceEnrollWebDTO {

    @JSONField(name = "deviceId")
    private String deviceId;

    @JSONField(name = "faceId")
    private String faceId;

    /** 人脸名称（对应 FaceFeature 的 tag 字段） */
    @JSONField(name = "tag")
    private String tag;

    @JSONField(name = "group")
    private String group;

    @JSONField(name = "imageBase64")
    private String imageBase64;
}
