package xyz.jasenon.lab.mqtt.mqtt.client.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import cn.hutool.core.lang.Assert;
import jakarta.annotation.Resource;
import xyz.jasenon.lab.common.entity.device.DeviceType;


@Component
public class MqttMessageDispatcher {

    @Resource(name = "handlers")
    private Map<DeviceType, MqttMessageHandler<?, ?, ?, ?>> handlers;

    public void dispatch(byte[] payloads, Long rs485Id) {
        DeviceType deviceType = parsDeviceType(payloads[0] & 0xFF);
        MqttMessageHandler<?, ?, ?, ?> handler = handlers.get(deviceType);
        Assert.notNull(handler,"未知的设备类型,无法获取消息处理器");
        handler.handle(payloads,rs485Id,deviceType);
    }

    DeviceType parsDeviceType(Integer address){
       return DeviceType.parseAddress(address);
    }
}