package xyz.jasenon.rsocket.core.packet;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;
import xyz.jasenon.rsocket.core.protocol.Message;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * 设备注册请求 - 简化设计
 */
@Getter
@Setter
public class RegisterRequest implements MessageAdaptor<RegisterRequest,RegisterResponse>,  Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 班牌唯一编号
     */
    private String uuid;

    /**
     * 关联的实验室id
     */
    private Long laboratoryId;

    @Override
    public Message<RegisterRequest> adaptor() {
        return Message.<RegisterRequest>builder()
                .route(Const.Route.DEVICE_REGISTER)
                .type(Message.Type.REQUEST_RESPONSE)
                .data(this)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public Class<RegisterResponse> getResponseType() {
        return RegisterResponse.class;
    }

}
