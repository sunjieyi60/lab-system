package xyz.jasenon.lab.mqtt.mqtt.client.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import xyz.jasenon.lab.common.entity.device.DeviceType;

import java.util.Map;


@Component
public class MqttMessageDispatcher {

    @Autowired
    private Map<DeviceType, MqttMessageHandler<?,?,?,?>> handlers;

    public void dispatch(byte[] payloads, Long rs485Id) {
        DeviceType deviceType = parsDeviceType(payloads[0] & 0xff);
        MqttMessageHandler<?,?,?,?> handler = handlers.get(deviceType);
        if (handler == null) {
            throw new IllegalArgumentException("未知的设备类型");
        }
        handler.handler(payloads, rs485Id, deviceType);
    }

    DeviceType parsDeviceType(Integer address){
       return DeviceType.parseAddress(address);
    }
}