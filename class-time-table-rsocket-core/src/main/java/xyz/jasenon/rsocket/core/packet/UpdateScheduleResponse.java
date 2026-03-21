package xyz.jasenon.rsocket.core.packet;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;
import xyz.jasenon.rsocket.core.protocol.Message;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * 更新课表响应
 * 
 * 设备返回课表更新结果
 */
@Getter
@Setter
public class UpdateScheduleResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 更新时间
     */
    private Instant updateTime;

    public static UpdateScheduleResponse success(Long version) {
        UpdateScheduleResponse response = new UpdateScheduleResponse();
        response.setSuccess(true);
        response.setUpdateTime(Instant.now());
        return response;
    }

    public static UpdateScheduleResponse fail(Integer code, String message) {
        UpdateScheduleResponse response = new UpdateScheduleResponse();
        response.setSuccess(false);
        return response;
    }

}
