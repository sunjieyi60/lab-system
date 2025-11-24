package xyz.jasenon.lab.mqtt.mqtt.client;

import org.eclipse.paho.client.mqttv3.MqttException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import xyz.jasenon.lab.mqtt.setnx.MqttNx;

@Slf4j
public class MqttAcceptClient extends MqttBaseClient {

    @Getter
    private final Long rs485Id;

    public MqttAcceptClient(MqttClientProperties mqttProperties, 
        MqttNx mqttNx, String topic, Long rs485Id) throws MqttException {
        super(mqttProperties, mqttNx, topic);
        this.rs485Id = rs485Id;
    }

}
