package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.ServerSend;
import xyz.jasenon.rsocket.core.protocol.Status;

import java.time.Instant;

/**
 * 重启请求
 * 
 * 服务器向设备发送重启指令
 * 继承 Message，简化设计
 */
@Getter
@Setter
public class RebootRequest extends Message implements ServerSend {

    private static final long serialVersionUID = 1L;

    /**
     * 延迟重启时间（秒），0表示立即重启
     */
    private Integer delaySeconds;

    /**
     * 重启原因
     */
    private String reason;

    /**
     * 请求时间
     */
    private Instant requestTime;

    /**
     * 创建重启请求 Message
     */
    public static RebootRequest create(Integer delaySeconds, String reason) {
        RebootRequest request = new RebootRequest();
        // client -> server 使用 route
        request.setRoute(Const.Route.DEVICE_REBOOT);
        request.setStatus(Status.C10000);
        request.setDelaySeconds(delaySeconds);
        request.setReason(reason);
        request.setRequestTime(Instant.now());
        request.setTimestamp(Instant.now());
        return request;
    }

    @Override
    public Command command() {
        return Command.REBOOT;
    }
}
