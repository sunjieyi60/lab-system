package xyz.jasenon.lab.mqtt.mqtt;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.redisson.api.RedissonClient;

public class TaskProcessor {

    private final String sendTopic;
    private final String acceptTopic;
    private final MqttClient mqttSendClient;
    private final MqttClient mqttAcceptClient;
    
    private final PriorityBlockingQueue<MqttTask> taskQueue = new PriorityBlockingQueue<>();
    private final ReentrantLock queueLock = new ReentrantLock();
    private final Condition queueNotEmpty = queueLock.newCondition();

    public TaskProcessor(String sendTopic, String acceptTopic, 
                        MqttClient mqttSendClient, MqttClient mqttAcceptClient,
                        RedissonClient redissonClient) {
        this.sendTopic = sendTopic;
        this.acceptTopic = acceptTopic;
        this.mqttSendClient = mqttSendClient;
        this.mqttAcceptClient = mqttAcceptClient;
    }

}
