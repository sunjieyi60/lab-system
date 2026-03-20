package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * 重启请求
 * 
 * 服务器向设备发送重启指令
 */
@Getter
@Setter
public class RebootRequest implements Serializable {

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
}
