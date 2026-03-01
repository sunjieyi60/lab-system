package xyz.jasenon.lab.common.entity.device.gateway;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.BaseEntity;

@Getter
@Setter
@TableName("rs485_gateway")
@Accessors(chain = true)
public class RS485Gateway extends BaseEntity {

    /**
     * 网关名称
     */
    private String gatewayName;

    /**
     * 网关订阅消息的主题
     */
    private String sendTopic;

    /**
     * 网关发送消息的主题
     */
    private String acceptTopic;

    /**
     * 所属实验室ID
     */
    private Long belongToLaboratoryId;
}
