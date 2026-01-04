package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.device.gateway.RS485Gateway;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.gateway.CreateRS485Gateway;
import xyz.jasenon.lab.service.dto.gateway.DeleteRS485Gateway;
import xyz.jasenon.lab.service.mapper.record.RS485GatewayMapper;
import xyz.jasenon.lab.service.service.IRS485GatewayService;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Service
public class  RS485GatewayServiceImpl extends ServiceImpl<RS485GatewayMapper, RS485Gateway> implements IRS485GatewayService {
    @Override
    public R createRS485Gateway(CreateRS485Gateway createRS485Gateway) {
        RS485Gateway rs485Gateway = new RS485Gateway()
                .setSendTopic(createRS485Gateway.getSendTopic())
                .setAcceptTopic(createRS485Gateway.getAcceptTopic())
                .setGatewayName(createRS485Gateway.getGatewayName())
                .setBelongToLaboratoryId(createRS485Gateway.getBelongToLaboratoryId());
        this.save(rs485Gateway);
        return R.success("rs485网关创建成功");
    }

    @Override
    public R deleteRS485Gateway(DeleteRS485Gateway deleteRS485Gateway) {
        this.removeById(deleteRS485Gateway.getRs485GatewayId());
        return R.success("rs485网关删除成功");
    }
}
