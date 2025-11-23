package xyz.jasenon.lab.common.entity.device.gateway;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.device.BaseSocketEntity;

@Getter
@Setter
public class SocketGateway extends BaseSocketEntity {

    /**
     * 网关名称
     */
    private String gatewayName;

}
