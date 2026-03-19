package xyz.jasenon.lab.class_time_table.rsocket.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 人脸识别数据
 */
@Data
@Builder
public class FaceData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 人脸 ID（注册时生成）
     */
    private String faceId;
    
    /**
     * 人脸名称（学生/教师姓名）
     */
    private String faceName;
    
    /**
     * 人脸特征数据（Base64 编码）
     */
    private String featureData;
    
    /**
     * 人脸图片 URL（可选，用于云端比对）
     */
    private String imageUrl;
    
    /**
     * 关联用户 ID
     */
    private String userId;
    
    /**
     * 用户类型（学生、教师、访客）
     */
    private UserType userType;
    
    /**
     * 有效期至（null 表示永久）
     */
    private Instant validUntil;
    
    public enum UserType {
        STUDENT,    // 学生
        TEACHER,    // 教师
        VISITOR,    // 访客
        ADMIN       // 管理员
    }
}
