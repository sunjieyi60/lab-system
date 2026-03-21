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
 * 设备注册响应 - 简化设计
 */
@Getter
@Setter
public class RegisterResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 班牌唯一编号
     */
    private String uuid;

    /**
     * 班牌配置
     */
    private Config config;

}
