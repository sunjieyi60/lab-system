package xyz.jasenon.lab.mqtt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.mqtt.mqtt.client.handler.*;

import java.util.Map;

/**
 * @author Jasenon_ce
 * @date 2025/11/25
 */
@Configuration
public class MessageHandlerConfig {

    @Bean(name = "handlers")
    public Map<DeviceType, MqttMessageHandler<?,?,?,?>> handlers(
            AirConditionMessageHandler airConditionMessageHandler,
            CircuitBreakMessageHandler circuitBreakMessageHandler,
            LightMessageHandler lightMessageHandler,
            SensorMessageHandler sensorMessageHandler,
            AccessMessageHandler accessMessageHandler
    ) {
        return Map.of(
                DeviceType.AirCondition, airConditionMessageHandler,
                DeviceType.CircuitBreak, circuitBreakMessageHandler,
                DeviceType.Light, lightMessageHandler,
                DeviceType.Sensor, sensorMessageHandler,
                DeviceType.Access, accessMessageHandler
        );
    }

}
