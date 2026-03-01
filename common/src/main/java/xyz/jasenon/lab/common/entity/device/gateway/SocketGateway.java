package xyz.jasenon.lab.common.entity.device.gateway;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.device.BaseSocketEntity;

@Getter
@Setter
@TableName("socket_gateway")
@Accessors(chain = true)
public class SocketGateway extends BaseSocketEntity {

    /**
     * 网关名称
     */
    private String gatewayName;

}
