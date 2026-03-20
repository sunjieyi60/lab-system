package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;

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
     * 结果消息
     */
    private String message;

    /**
     * 设备当前配置版本
     */
    private Long currentVersion;

    /**
     * 更新时间
     */
    private Instant updateTime;

    public static UpdateConfigResponse success(Long version) {
        UpdateConfigResponse response = new UpdateConfigResponse();
        response.setSuccess(true);
        response.setCode(0);
        response.setMessage("配置更新成功");
        response.setCurrentVersion(version);
        response.setUpdateTime(Instant.now());
        return response;
    }

    public static UpdateConfigResponse fail(Integer code, String message) {
        UpdateConfigResponse response = new UpdateConfigResponse();
        response.setSuccess(false);
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}
