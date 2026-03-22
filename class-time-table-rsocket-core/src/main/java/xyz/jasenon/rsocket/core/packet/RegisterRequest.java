package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.ClientSend;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Status;

import java.time.Instant;

/**
 * 设备注册请求 - 继承 Message
 * 
 * 客户端向服务器发送注册请求
 */
@Getter
@Setter
public class RegisterRequest extends Message implements ClientSend {

    private static final long serialVersionUID = 1L;

    /**
     * 班牌唯一编号
     */
    private String uuid;

    /**
     * 关联的实验室id
     */
    private Long laboratoryId;

    /**
     * 创建注册请求 Message
     */
    public static RegisterRequest create(String uuid, Long laboratoryId) {
        RegisterRequest request = new RegisterRequest();
        // client -> server 使用 route
        request.setRoute(Const.Route.DEVICE_REGISTER);
        request.setStatus(Status.C10000);
        request.setUuid(uuid);
        request.setLaboratoryId(laboratoryId);
        request.setTimestamp(Instant.now());
        return request;
    }

    @Override
    public String route() {
        return Const.Route.DEVICE_REGISTER;
    }
}
