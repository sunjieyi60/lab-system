package xyz.jasenon.lab.mqtt.mqtt.client;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import xyz.jasenon.lab.mqtt.mqtt.client.handler.MqttMessageDispatcher;
import xyz.jasenon.lab.mqtt.setnx.MqttNx;

@Slf4j
@RequiredArgsConstructor
public class MqttAcceptCallback implements MqttCallbackExtended {

    private final MqttNx mqttNx;
    private final MqttAcceptClient mqttClient;
    private final ThreadPoolTaskExecutor executor;
    private final MqttMessageDispatcher dispatcher;

    @Override
    public void connectionLost(Throwable arg0) {
        log.info("connectionLost");
        try {
            if (mqttNx.tryLock()) {
                mqttClient.connect();
                mqttClient.subscribe(this.mqttClient.topic);
            }
        } catch (Exception e) {
            log.error("reconnect error:{},mqttClient:{},mqttNx:{}", e, mqttClient.getClientId(), mqttNx.getKey());
        } finally {
            mqttNx.unlock();
            ;
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken arg0) {
        try {
            log.info("deliveryComplete, message:{}", arg0.getMessage().getPayload());
        } catch (MqttException e) {
            log.error("deliveryComplete error:{}", e);
        }
    }

    @Override
    public void messageArrived(String arg0, MqttMessage arg1) {
        byte[] payloads = arg1.getPayload();
        String topic = arg0;
        log.info("messageArrived, topic:{},payloads:{}", topic, payloads);
        executor.execute(() -> {
            try {
                dispatcher.dispatch(payloads, this.mqttClient.getRs485Id());
            } catch (Exception e) {
                log.error("messageHandle error: {}", e);
            }
        });
        mqttNx.unlock();
    }

    @Override
    @SneakyThrows
    public void connectComplete(boolean arg0, String arg1) {
        log.info("mqttClient:{},connect to topic:{} success", mqttClient.getClientId(), arg1);
        mqttClient.subscribe(this.mqttClient.topic);
    }

}
