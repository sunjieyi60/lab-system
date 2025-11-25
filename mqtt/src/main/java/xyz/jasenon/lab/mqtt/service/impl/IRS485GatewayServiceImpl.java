package xyz.jasenon.lab.mqtt.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import xyz.jasenon.lab.common.entity.device.gateway.RS485Gateway;
import xyz.jasenon.lab.mqtt.mapper.RS485Mapper;
import xyz.jasenon.lab.mqtt.service.IRS485GatewayService;

import java.util.List;

@Service
public class IRS485GatewayServiceImpl extends ServiceImpl<RS485Mapper,RS485Gateway> implements IRS485GatewayService {

    @Autowired
    private RS485Mapper rs485GatewayMapper;

    @Override
    public RS485Gateway getRS485GatewayByGatewayId(Long gatewayId) {
        return rs485GatewayMapper.selectById(gatewayId);
    }

    @Override
    public List<RS485Gateway> listAllRS485Gateway() {
        return list();
    }
}
