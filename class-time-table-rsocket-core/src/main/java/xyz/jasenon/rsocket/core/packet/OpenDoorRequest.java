package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * 开门请求
 * 
 * 服务器向设备发送开门指令
 */
@Getter
@Setter
public class OpenDoorRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 开门方式：FACE/Password/REMOTE
     */
    private OpenType type;

    /**
     * 验证信息（人脸ID或密码）
     */
    private String verifyInfo;

    /**
     * 开门持续时间（秒）
     */
    private Integer duration;

    /**
     * 请求时间
     */
    private Instant requestTime;

    public enum OpenType {
        /**
         * 人脸识别
         */
        FACE,
        /**
         * 密码
         */
        PASSWORD,
        /**
         * 远程开门
         */
        REMOTE
    }
}
