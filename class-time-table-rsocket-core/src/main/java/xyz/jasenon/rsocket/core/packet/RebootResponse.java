package xyz.jasenon.rsocket.core.packet;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;
import xyz.jasenon.rsocket.core.protocol.Message;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * 重启响应
 * 
 * 设备确认重启
 */
@Getter
@Setter
public class RebootResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否接受重启
     */
    private boolean accepted;

    /**
     * 预计重启时间
     */
    private Instant rebootTime;

    /**
     * 结果消息
     */
    private String message;

    public static RebootResponse accepted(Instant rebootTime) {
        RebootResponse response = new RebootResponse();
        response.setAccepted(true);
        response.setRebootTime(rebootTime);
        response.setMessage("设备将在指定时间重启");
        return response;
    }

    public static RebootResponse rejected(String reason) {
        RebootResponse response = new RebootResponse();
        response.setAccepted(false);
        response.setMessage("重启被拒绝: " + reason);
        return response;
    }

}
