package xyz.jasenon.lab.mqtt.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.mqtt.mqtt.client.handler.AccessMessageHandler;
import xyz.jasenon.lab.mqtt.mqtt.client.handler.AirConditionMessageHandler;
import xyz.jasenon.lab.mqtt.mqtt.client.handler.CircuitBreakMessageHandler;
import xyz.jasenon.lab.mqtt.mqtt.client.handler.LightMessageHandler;
import xyz.jasenon.lab.mqtt.mqtt.client.handler.MqttMessageHandler;
import xyz.jasenon.lab.mqtt.mqtt.client.handler.SensorMessageHandler;

@Configuration
public class MessageDispatcherConfig {

    @Bean("handlers")
    Map<DeviceType,MqttMessageHandler<?,?,?,?>> handlers(
        AccessMessageHandler accessMessageHandler,
        CircuitBreakMessageHandler circuitBreakMessageHandler,
        AirConditionMessageHandler airConditionMessageHandler,
        LightMessageHandler lightMessageHandler,
        SensorMessageHandler sensorMessageHandler
    ){
        Map<DeviceType,MqttMessageHandler<?,?,?,?>> handlers = new HashMap<>();
        handlers.put(DeviceType.Access, accessMessageHandler);
        handlers.put(DeviceType.CircuitBreak, circuitBreakMessageHandler);
        handlers.put(DeviceType.AirCondition, airConditionMessageHandler);
        handlers.put(DeviceType.Light, lightMessageHandler);
        handlers.put(DeviceType.Sensor, sensorMessageHandler);
        return handlers;
    }

}
