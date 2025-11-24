package xyz.jasenon.lab.common.service;

import xyz.jasenon.lab.common.entity.device.gateway.RS485Gateway;

public interface IRS485GatewayService$ {

    RS485Gateway getRS485GatewayByGatewayId(Long gatewayId);
    
}
