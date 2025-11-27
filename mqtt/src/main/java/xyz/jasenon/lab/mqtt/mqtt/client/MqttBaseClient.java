package xyz.jasenon.lab.mqtt.mqtt.client;

import cn.hutool.crypto.digest.MD5;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import xyz.jasenon.lab.mqtt.setnx.MqttNx;

import java.util.UUID;

public class MqttBaseClient extends MqttClient {

    public final MqttClientProperties mqttClientProperties;
    public final MqttNx mqttNx;
    public final String topic;

    public MqttBaseClient(MqttClientProperties mqttProperties,
            MqttNx mqttNx, String topic) throws MqttException {

        super(mqttProperties.getHostUrl(),
                MD5.create().digestHex(UUID.randomUUID().toString()),
                new MemoryPersistence());

        this.mqttClientProperties = mqttProperties;
        this.mqttNx = mqttNx;
        this.topic = topic;
    }

}
