package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.device.gateway.SocketGateway;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.gateway.CreateSocketGateway;
import xyz.jasenon.lab.service.dto.gateway.DeleteSocketGateway;
import xyz.jasenon.lab.service.mapper.SocketGatewayMapper;
import xyz.jasenon.lab.service.service.ISocketGatewayService;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Service
public class SocketGatewayServiceImpl extends ServiceImpl<SocketGatewayMapper, SocketGateway> implements ISocketGatewayService {
    @Override
    public R createSocketGateway(CreateSocketGateway createSocketGateway) {
        SocketGateway socketGateway = (SocketGateway) new SocketGateway()
                .setGatewayName(createSocketGateway.gatewayName())
                .setMac(createSocketGateway.mac())
                .setBelongToLaboratoryId(createSocketGateway.belongToLaboratoryId());
        this.save(socketGateway);
        return R.success("Socket网关创建成功");
    }

    @Override
    public R deleteSocketGateway(DeleteSocketGateway deleteSocketGateway) {
        this.removeById(deleteSocketGateway.socketGatewayId());
        return R.success("Socket网关删除成功");
    }
}
