package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 设备注册请求 - 简化设计
 */
@Getter
@Setter
public class RegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 班牌唯一编号
     */
    private String uuid;

    /**
     * 关联的实验室id
     */
    private Long laboratoryId;
}
