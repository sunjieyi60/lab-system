package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * 开门响应
 * 
 * 设备返回开门执行结果
 */
@Getter
@Setter
public class OpenDoorResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 结果码：0成功，其他失败
     */
    private Integer code;

    /**
     * 结果消息
     */
    private String message;

    /**
     * 实际开门时间
     */
    private Instant openTime;

    public static OpenDoorResponse success() {
        OpenDoorResponse response = new OpenDoorResponse();
        response.setSuccess(true);
        response.setCode(0);
        response.setMessage("开门成功");
        response.setOpenTime(Instant.now());
        return response;
    }

    public static OpenDoorResponse fail(Integer code, String message) {
        OpenDoorResponse response = new OpenDoorResponse();
        response.setSuccess(false);
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}
