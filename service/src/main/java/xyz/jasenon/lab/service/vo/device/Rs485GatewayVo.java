package xyz.jasenon.lab.service.vo.device;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Getter
@Setter
public class Rs485GatewayVo {

    /**
     * 网关id
     */
    private Long gatewayId;
    /**
     * 所属实验室id
     */
    private Long laboratoryId;
    /**
     * 网关名称
     */
    private String gatewayName;
    /**
     * 接收主题
     */
    private String acceptTopic;
    /**
     * 发送主题
     */
    private String sendTopic;

}
