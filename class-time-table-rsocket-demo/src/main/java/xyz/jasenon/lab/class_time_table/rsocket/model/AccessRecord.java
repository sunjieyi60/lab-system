package xyz.jasenon.lab.class_time_table.rsocket.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 门禁访问记录
 */
@Data
@Builder
public class AccessRecord implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 记录 ID
     */
    private String recordId;
    
    /**
     * 班牌设备 UUID
     */
    private String deviceUuid;
    
    /**
     * 识别到的人脸 ID
     */
    private String faceId;
    
    /**
     * 人员姓名
     */
    private String personName;
    
    /**
     * 用户类型
     */
    private FaceData.UserType userType;
    
    /**
     * 访问时间
     */
    private Instant accessTime;
    
    /**
     * 识别结果
     */
    private Result result;
    
    /**
     * 开门方式（人脸、刷卡、密码等）
     */
    private AccessMethod method;
    
    /**
     * 匹配分数（0-100）
     */
    private Integer matchScore;
    
    /**
     * 抓拍图片 URL
     */
    private String snapshotUrl;
    
    /**
     * 备注
     */
    private String remark;
    
    public enum Result {
        ALLOWED,    // 允许通过
        DENIED,     // 拒绝
        EXPIRED,    // 权限过期
        UNKNOWN     // 未知人员
    }
    
    public enum AccessMethod {
        FACE,       // 人脸识别
        CARD,       // 刷卡
        PASSWORD,   // 密码
        QR_CODE,    // 二维码
        REMOTE      // 远程开门
    }
}
