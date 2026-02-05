package xyz.jasenon.lab.mqtt.mqtt.client;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import xyz.jasenon.lab.mqtt.setnx.MqttNx;

@Slf4j
public class MqttSendCallback implements MqttCallbackExtended {

    private final MqttSendClient mqttClient;
    private final MqttNx mqttNx;

    public MqttSendCallback(MqttSendClient sendClient, MqttNx mqttNx) {
        this.mqttClient = sendClient;
        this.mqttNx = mqttNx;
    }

    @Override
    @SneakyThrows
    public void connectComplete(boolean b, String s) {
        log.info("mqttClient:{},connect to topic:{} success", mqttClient.getClientId(), s);
        mqttClient.subscribe(this.mqttClient.topic);
    }

    @Override
    public void connectionLost(Throwable throwable) {
        log.warn("mqttClient:{},connect to topic:{} lost", mqttClient.getClientId(), mqttClient.topic);
        try {
            if (mqttNx.tryLock()) {
                this.mqttClient.connect();
                this.mqttClient.subscribe(this.mqttClient.topic);
            }
        } catch (MqttException e) {
            log.error("mqttClient:{},reconnect to topic:{} error:{}", mqttClient.getClientId(), mqttClient.topic, e.getMessage());
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            log.error("redisson lock error:{}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            mqttNx.unlock();
        }
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        iMqttDeliveryToken.setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken iMqttToken) {
                log.info("mqttClient:{},send message to topic:{} success", mqttClient.getClientId(), mqttClient.topic);
                try {
                    log.info("payload:{}", new String(iMqttDeliveryToken.getMessage().getPayload()));
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                mqttNx.unlock();
                log.warn("mqttClient:{},send message to topic:{} failed", mqttClient.getClientId(), mqttClient.topic);
            }
        });
    }
}
