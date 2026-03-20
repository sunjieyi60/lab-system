package xyz.jasenon.rsocket.core.packet;

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
     * 结果码
     */
    private Integer code;

    /**
     * 设备当前课表版本
     */
    private Long currentVersion;

    /**
     * 更新时间
     */
    private Instant updateTime;

    public static UpdateScheduleResponse success(Long version) {
        UpdateScheduleResponse response = new UpdateScheduleResponse();
        response.setSuccess(true);
        response.setCode(0);
        response.setCurrentVersion(version);
        response.setUpdateTime(Instant.now());
        return response;
    }

    public static UpdateScheduleResponse fail(Integer code, String message) {
        UpdateScheduleResponse response = new UpdateScheduleResponse();
        response.setSuccess(false);
        response.setCode(code);
        return response;
    }
}
