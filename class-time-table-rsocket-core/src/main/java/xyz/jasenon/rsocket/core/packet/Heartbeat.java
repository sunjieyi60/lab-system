package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.model.Config;

import java.io.Serializable;

/**
 * 心跳请求/响应负载 - 简化设计
 */
@Getter
@Setter
public class Heartbeat implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备数据库ID
     */
    private Long deviceDbId;

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
