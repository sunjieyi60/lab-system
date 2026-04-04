package xyz.jasenon.rsocket.core.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.BaseEntity;

import java.util.List;

/**
 * 人脸图片库实体 - 用于web渲染人脸库
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName(value = "face_image_library", autoResultMap = true)
public class FaceImageEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 关联的班牌UUID */
    private String uuid;

    /** 人脸特征名称 */
    private String faceFeatureName;

    /** 人脸图片URL列表 - 用于web渲染，存储上传后的图片URL */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> imageUrls;

    /** 人脸特征数据（由client提取后回传，可选） */
    private byte[] faceFeature;

    /** 图片数量 */
    private Integer imageCount;

    /** 状态: UPLOADING/COMPLETED/FAILED */
    private String status;

    /** 关联的任务ID */
    private String taskId;
}
