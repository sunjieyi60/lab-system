package xyz.jasenon.lab.mqtt.mqtt.client;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import xyz.jasenon.lab.mqtt.mqtt.client.handler.MqttMessageDispatcher;
import xyz.jasenon.lab.mqtt.setnx.MqttNx;

@Slf4j
public class MqttAcceptClient extends MqttBaseClient {

    @Getter
    private final Long rs485Id;
    private final ThreadPoolTaskExecutor executor;
    private final MqttMessageDispatcher mqttMessageDispatcher;

    public MqttAcceptClient(MqttClientProperties mqttProperties,
                            MqttNx mqttNx, String topic, Long rs485Id,
                            ThreadPoolTaskExecutor executor,
                            MqttMessageDispatcher mqttMessageDispatcher) throws MqttException {
        super(mqttProperties, mqttNx, topic);
        this.rs485Id = rs485Id;
        this.executor = executor;
        this.mqttMessageDispatcher = mqttMessageDispatcher;
        customConnect();
    }

    protected void customConnect(){
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(mqttClientProperties.getUsername());
        options.setPassword(mqttClientProperties.getPassword().toCharArray());
        options.setCleanSession(mqttClientProperties.getCleanSession());
        options.setConnectionTimeout(mqttClientProperties.getTimeout());
        options.setKeepAliveInterval(mqttClientProperties.getKeepAlive());
        options.setAutomaticReconnect(true);

        try {
            this.connect(options);
            this.setCallback(new MqttAcceptCallback(mqttNx, this, executor, mqttMessageDispatcher));
        } catch (Exception e) {
            log.error("connect error", e);
        }
    }

}
