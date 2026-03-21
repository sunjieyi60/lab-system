package xyz.jasenon.rsocket.core.packet;

import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;
import xyz.jasenon.rsocket.core.protocol.Message;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * 开门请求
 * 
 * 服务器向设备发送开门指令
 * 实现 MessageAdaptor<OpenDoorRequest, OpenDoorResponse> 实现类型安全
 */
@Getter
@Setter
public class OpenDoorRequest implements MessageAdaptor<OpenDoorRequest, OpenDoorResponse>, Serializable {

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

    /**
     * 将请求转换为 Message 对象
     */
    @Override
    public Message<OpenDoorRequest> adaptor() {
        return Message.<OpenDoorRequest>builder()
                .route(Const.Route.DEVICE_DOOR_OPEN)
                .type(Message.Type.REQUEST_RESPONSE)
                .data(this)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 定义响应类型，用于类型安全
     */
    @Override
    public Class<OpenDoorResponse> getResponseType() {
        return OpenDoorResponse.class;
    }
}
