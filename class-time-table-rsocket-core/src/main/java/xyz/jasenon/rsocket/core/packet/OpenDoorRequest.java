package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.ServerSend;
import xyz.jasenon.rsocket.core.protocol.Status;

import java.io.Serial;
import java.io.Serializable;


/**
 * 开门请求
 * 
 * 服务器向设备发送开门指令
 * 继承 Message，简化设计
 */
@Getter
@Setter
public class OpenDoorRequest extends Message implements ServerSend, Serializable {

    @Serial
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
    private Long requestTime;

    @Override
    public Command command() {
        return super.getCommand();
    }

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

    /**
     * 创建开门请求 Message
     */
    public static OpenDoorRequest create(OpenType type, String verifyInfo, Integer duration) {
        OpenDoorRequest request = new OpenDoorRequest();
        // client -> server 使用 route
        request.setRoute(Const.Route.DEVICE_DOOR_OPEN);
        request.setStatus(Status.C10000);
        request.setType_(type);
        request.setVerifyInfo(verifyInfo);
        request.setDuration(duration);
        request.setRequestTime(System.currentTimeMillis());
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    // 兼容旧代码的 getter/setter
    public OpenType getType_() {
        return type;
    }

    public void setType_(OpenType type) {
        this.type = type;
    }
}
