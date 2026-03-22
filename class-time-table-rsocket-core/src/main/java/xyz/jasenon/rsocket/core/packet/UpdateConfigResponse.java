package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.ClientSend;
import xyz.jasenon.rsocket.core.protocol.Message;

import java.time.Instant;

/**
 * 更新配置响应
 * 
 * 设备返回配置更新结果
 * 继承 Message，简化设计
 */
@Getter
@Setter
public class UpdateConfigResponse extends Message implements ClientSend {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 更新时间
     */
    private Instant updateTime;

    public static UpdateConfigResponse success(Long version) {
        UpdateConfigResponse response = new UpdateConfigResponse();
        response.setSuccess(true);
        response.setUpdateTime(Instant.now());
        response.setTimestamp(Instant.now());
        return response;
    }

    public static UpdateConfigResponse fail(Integer code, String message) {
        UpdateConfigResponse response = new UpdateConfigResponse();
        response.setSuccess(false);
        response.setTimestamp(Instant.now());
        // 错误信息可以通过其他字段传递
        return response;
    }

    @Override
    public String route() {
        return Const.Route.DEVICE_CONFIG_UPDATE;
    }
}
