package xyz.jasenon.rsocket.core.packet;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;
import xyz.jasenon.rsocket.core.protocol.Message;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.model.Config;

import java.io.Serializable;
import java.time.Instant;

/**
 * 更新配置请求
 * 
 * 服务器向设备推送新配置
 */
@Getter
@Setter
public class UpdateConfigRequest implements MessageAdaptor<UpdateConfigRequest, UpdateConfigResponse>,  Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 新配置
     */
    private Config config;

    /**
     * 是否立即生效
     */
    private Boolean immediate;

    /**
     * 配置版本号（用于冲突检测）
     */
    private Long version;

    /**
     * 请求时间
     */
    private Instant requestTime;

    @Override
    public Message<UpdateConfigRequest> adaptor() {
        return Message.<UpdateConfigRequest>builder()
                .route(Const.Route.DEVICE_CONFIG_UPDATE)
                .type(Message.Type.REQUEST_RESPONSE)
                .data(this)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public Class<UpdateConfigResponse> getResponseType() {
        return UpdateConfigResponse.class;
    }

}
