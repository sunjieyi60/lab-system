package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.device.gateway.SocketGateway;
import xyz.jasenon.lab.service.dto.gateway.CreateSocketGateway;
import xyz.jasenon.lab.service.dto.gateway.DeleteSocketGateway;
import xyz.jasenon.lab.service.mapper.record.SocketGatewayMapper;
import xyz.jasenon.lab.service.service.ISocketGatewayService;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Service
public class SocketGatewayServiceImpl extends ServiceImpl<SocketGatewayMapper, SocketGateway> implements ISocketGatewayService {
    @Override
    public SocketGateway createSocketGateway(CreateSocketGateway createSocketGateway) {
        SocketGateway socketGateway = (SocketGateway) new SocketGateway()
                .setGatewayName(createSocketGateway.getGatewayName())
                .setMac(createSocketGateway.getMac())
                .setBelongToLaboratoryId(createSocketGateway.getBelongToLaboratoryId());
        this.save(socketGateway);
        return socketGateway;
    }

    @Override
    public void deleteSocketGateway(DeleteSocketGateway deleteSocketGateway) {
        this.removeById(deleteSocketGateway.getSocketGatewayId());
    }
}
