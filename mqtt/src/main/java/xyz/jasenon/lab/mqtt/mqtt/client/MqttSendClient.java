package xyz.jasenon.lab.mqtt.mqtt.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import xyz.jasenon.lab.mqtt.mqtt.MqttTask;
import xyz.jasenon.lab.mqtt.setnx.MqttNx;

@Slf4j
public class MqttSendClient extends MqttBaseClient{

    private final Long rs485Id;

    public MqttSendClient(MqttClientProperties mqttProperties, 
        MqttNx mqttNx, String topic, Long rs485Id) throws MqttException {
            super(mqttProperties, mqttNx, topic);
            this.rs485Id = rs485Id;
            customConnect();
    }

    private void customConnect(){
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(mqttClientProperties.getUsername());
        options.setPassword(mqttClientProperties.getPassword().toCharArray());
        options.setCleanSession(mqttClientProperties.getCleanSession());
        options.setConnectionTimeout(mqttClientProperties.getTimeout());
        options.setKeepAliveInterval(mqttClientProperties.getKeepAlive());
        options.setAutomaticReconnect(true);

        try {
            this.setCallback(new MqttSendCallback(this,mqttNx));
            this.connect(options);
        } catch (Exception e) {
            log.error("connect error", e);
        }
    }

    public void submitMqttTask(MqttTask mqttTask){
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(mqttClientProperties.getQos());
        mqttMessage.setPayload(mqttTask.getPayload());
        try {
           this.publish(topic, mqttMessage);
        }catch (Exception e){
            log.error("submitMqttTask error", e);
        }
    }

}
