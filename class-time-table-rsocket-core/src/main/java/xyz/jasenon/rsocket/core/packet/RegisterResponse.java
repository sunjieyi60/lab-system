package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.model.Config;

import java.io.Serializable;

/**
 * 设备注册响应 - 简化设计
 */
@Getter
@Setter
public class RegisterResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备数据库ID
     */
    private Long deviceDbId;

    /**
     * 班牌配置
     */
    private Config config;
}
