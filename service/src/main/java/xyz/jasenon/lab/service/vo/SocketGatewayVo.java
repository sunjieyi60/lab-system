package xyz.jasenon.lab.service.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Getter
@Setter
public class SocketGatewayVo {

    /**
     * 网关id
     */
    private Long gatewayId;

    /**
     * 网关名称
     */
    private String gatewayName;

    /**
     * 所属实验室id
     */
    private Long laboratoryId;

    /**
     * mac地址
     */
    private String mac;

    /**
     * 当前ip
     */
    private String ip;

}
