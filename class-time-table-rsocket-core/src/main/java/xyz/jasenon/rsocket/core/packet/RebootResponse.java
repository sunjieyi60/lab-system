package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.ClientSend;
import xyz.jasenon.rsocket.core.protocol.Message;

import java.time.Instant;

/**
 * 重启响应
 * 
 * 设备确认重启
 * 继承 Message，简化设计
 */
@Getter
@Setter
public class RebootResponse extends Message implements ClientSend {

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
    private String messageText;

    public static RebootResponse accepted(Instant rebootTime) {
        RebootResponse response = new RebootResponse();
        response.setAccepted(true);
        response.setRebootTime(rebootTime);
        response.setMessageText("设备将在指定时间重启");
        response.setTimestamp(Instant.now());
        return response;
    }

    public static RebootResponse rejected(String reason) {
        RebootResponse response = new RebootResponse();
        response.setAccepted(false);
        response.setMessageText("重启被拒绝: " + reason);
        response.setTimestamp(Instant.now());
        return response;
    }

    @Override
    public String route() {
        return Const.Route.DEVICE_REBOOT;
    }
}
