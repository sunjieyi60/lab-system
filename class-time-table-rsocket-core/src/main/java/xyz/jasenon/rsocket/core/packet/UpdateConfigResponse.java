package xyz.jasenon.rsocket.core.packet;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;
import xyz.jasenon.rsocket.core.protocol.Message;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 更新配置响应
 * 
 * 设备返回配置更新结果
 */
@Getter
@Setter
public class UpdateConfigResponse implements Serializable {

    @Serial
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
        return response;
    }

    public static UpdateConfigResponse fail(Integer code, String message) {
        UpdateConfigResponse response = new UpdateConfigResponse();
        response.setSuccess(false);
        return response;
    }

}
