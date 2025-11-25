package xyz.jasenon.lab.mqtt.service;

import xyz.jasenon.lab.common.entity.device.gateway.RS485Gateway;
import xyz.jasenon.lab.common.service.IRS485GatewayService$;

import java.util.List;

public interface IRS485GatewayService extends IRS485GatewayService$ {

    List<RS485Gateway> listAllRS485Gateway();

}
