package xyz.jasenon.lab.mqtt.mqtt;

import lombok.SneakyThrows;
import xyz.jasenon.lab.common.dto.task.TaskPriority;
import xyz.jasenon.lab.common.entity.log.AlarmLog;
import xyz.jasenon.lab.mqtt.config.AlarmReportClient;
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
    private final AlarmReportClient alarmReportClient;

    private final PriorityBlockingQueue<MqttTask> taskQueue = new PriorityBlockingQueue<>();
    private final ReentrantLock queueLock = new ReentrantLock();
    private final Condition queueNotEmpty = queueLock.newCondition();

    public TaskProcessor(String sendTopic, String acceptTopic,
                        MqttSendClient mqttSendClient, MqttAcceptClient mqttAcceptClient,
                        AlarmReportClient alarmReportClient) {
        this.sendTopic = sendTopic;
        this.acceptTopic = acceptTopic;
        this.mqttSendClient = mqttSendClient;
        this.mqttAcceptClient = mqttAcceptClient;
        this.alarmReportClient = alarmReportClient;
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
                    if (mqttSendClient.mqttNx.wasPendingSend()) {
                        AlarmLog alarmLog = new AlarmLog()
                                .setCategory("设备异常")
                                .setAlarmType("网关应答超时")
                                .setRoom(null)
                                .setDevice("网关-" + mqttSendClient.getRs485Id())
                                .setContent("上一条消息未在超时内收到应答，已超时释放")
                                .setGatewayId(mqttSendClient.getRs485Id());
                        alarmReportClient.reportAlarm(alarmLog);
                        mqttSendClient.mqttNx.clearPendingSend();
                    }
                    mqttSendClient.mqttNx.markPendingSend();
                    mqttSendClient.submitMqttTask(task);
                } else if (!task.getPriority().equals(TaskPriority.POLLING)) {
                    taskQueue.add(task);
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
