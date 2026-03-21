package xyz.jasenon.rsocket.core.packet;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;
import xyz.jasenon.rsocket.core.protocol.Message;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.model.Config;

import java.io.Serializable;
import java.time.Instant;

/**
 * 心跳请求/响应负载 - 简化设计
 */
@Getter
@Setter
public class Heartbeat implements  Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 班牌唯一编号
     */
    private String uuid;

    /**
     * 心跳间隔（秒）
     */
    private Integer interval;

    /**
     * 是否有配置更新
     */
    private Boolean configUpdated;

    /**
     * 新配置（如果有更新）
     */
    private Config newConfig;

}
