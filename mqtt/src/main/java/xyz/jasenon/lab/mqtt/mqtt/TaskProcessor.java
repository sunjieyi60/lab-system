package xyz.jasenon.lab.mqtt.mqtt;

import lombok.SneakyThrows;
import xyz.jasenon.lab.mqtt.mqtt.client.MqttAcceptClient;
import xyz.jasenon.lab.mqtt.mqtt.client.MqttSendClient;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class TaskProcessor {

    private final String sendTopic;
    private final String acceptTopic;
    private final MqttSendClient mqttSendClient;
    private final MqttAcceptClient mqttAcceptClient;
    
    private final PriorityBlockingQueue<MqttTask> taskQueue = new PriorityBlockingQueue<>();
    private final ReentrantLock queueLock = new ReentrantLock();
    private final Condition queueNotEmpty = queueLock.newCondition();

    public TaskProcessor(String sendTopic, String acceptTopic,
                        MqttSendClient mqttSendClient, MqttAcceptClient mqttAcceptClient) {
        this.sendTopic = sendTopic;
        this.acceptTopic = acceptTopic;
        this.mqttSendClient = mqttSendClient;
        this.mqttAcceptClient = mqttAcceptClient;
        Thread thread = new Thread(this::processTasks);
        thread.setDaemon(true);
        thread.start();
    }

    @SneakyThrows
    private void processTasks() {
        while (true) {
            MqttTask task = null;
            try {
                queueLock.lock();
                if (taskQueue.isEmpty()) {
                    queueNotEmpty.await();
                }
                task = taskQueue.poll();
            } finally {
                queueLock.unlock();
            }
            if (task != null) {
                if (mqttSendClient.mqttNx.tryLock()) {
                    mqttSendClient.submitMqttTask(task);
                }
            }
        }
    }

    public void addTask(MqttTask task){
        try {
            queueLock.lock();
            taskQueue.add(task);
            queueNotEmpty.signal();
        } finally {
            queueLock.unlock();
        }
    }

}
