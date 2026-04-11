package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.device.gateway.SocketGateway;
import xyz.jasenon.lab.service.dto.gateway.CreateSocketGateway;
import xyz.jasenon.lab.service.dto.gateway.DeleteSocketGateway;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
public interface ISocketGatewayService extends IService<SocketGateway> {

    SocketGateway createSocketGateway(CreateSocketGateway createSocketGateway);

    void deleteSocketGateway(DeleteSocketGateway deleteSocketGateway);

}
