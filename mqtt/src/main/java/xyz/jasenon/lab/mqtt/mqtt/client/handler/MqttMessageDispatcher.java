package xyz.jasenon.lab.mqtt.mqtt.client.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import xyz.jasenon.lab.common.entity.device.DeviceType;


@Component
public class MqttMessageDispatcher {

    @Autowired
    private AccessMessageHandler accessMessageHandler;

    public void dispatch(byte[] payloads, Long rs485Id) {

    }

    DeviceType parsDeviceType(Integer address){
       return DeviceType.parseAddress(address);
    }
}