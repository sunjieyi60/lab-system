package xyz.jasenon.lab.mqtt.mqtt;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import xyz.jasenon.lab.common.dto.task.TaskPriority;
import xyz.jasenon.lab.common.entity.log.AlarmLog;
import xyz.jasenon.lab.mqtt.config.AlarmReportClient;
import xyz.jasenon.lab.mqtt.mqtt.client.MqttAcceptClient;
import xyz.jasenon.lab.mqtt.mqtt.client.MqttSendClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class TaskProcessor {

    private final String sendTopic;
    private final String acceptTopic;
    private final MqttSendClient mqttSendClient;
    private final MqttAcceptClient mqttAcceptClient;
    private final AlarmReportClient alarmReportClient;

    private final PriorityBlockingQueueWithCondition<MqttTask> taskQueue = new PriorityBlockingQueueWithCondition<>(
        task -> {
            if (task.getPriority().equals(TaskPriority.POLLING)){
                return true;
            }
            return false;
        }, MqttTask::getSendThreadName);
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
                try {
                    // 注意：poll() 时已经移除了 flag，所以这里不需要再次移除
                    if (mqttSendClient.mqttNx.tryLock()) {
                        // 成功获取锁，任务即将发送
                        // flag 已经在 poll() 时移除，所以这里不需要再次移除
                        
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
                            log.warn("网关应答超时，rs485Id: {}, 上一条消息未在超时内收到应答", mqttSendClient.getRs485Id());
                        }
                        mqttSendClient.mqttNx.markPendingSend();
                        mqttSendClient.submitMqttTask(task);
                        log.debug("任务已发送，sendThreadName: {}, deviceId: {}, rs485Id: {}, priority: {}", 
                                task.getSendThreadName(), task.getDeviceId(), task.getRs485Id(), task.getPriority());
                        
                        // 优雅等待：等待 acceptCallback 的 unlock 或超时自动唤醒
                        // 使用与 Redis key 相同的超时时间，确保超时后可以继续处理下一个任务
                        long timeoutMs = mqttSendClient.mqttNx.getTimeout();
                        TimeUnit timeoutUnit = mqttSendClient.mqttNx.getTimeUnit();
                        boolean receivedAck = mqttSendClient.mqttNx.await(timeoutMs, timeoutUnit);
                        if (!receivedAck) {
                            // 超时未收到应答，记录告警
                            AlarmLog alarmLog = new AlarmLog()
                                    .setCategory("设备异常")
                                    .setAlarmType("网关应答超时")
                                    .setRoom(null)
                                    .setDevice("网关-" + mqttSendClient.getRs485Id())
                                    .setContent("消息发送后未在超时内收到应答")
                                    .setGatewayId(mqttSendClient.getRs485Id());
                            alarmReportClient.reportAlarm(alarmLog);
                            log.warn("消息发送后等待应答超时，rs485Id: {}, sendThreadName: {}", 
                                    mqttSendClient.getRs485Id(), task.getSendThreadName());
                        } else {
                            log.debug("收到应答，rs485Id: {}, sendThreadName: {}", 
                                    mqttSendClient.getRs485Id(), task.getSendThreadName());
                        }
                    } else {
                        // mqttNx被锁定，重新加入队列等待下次处理
                        // 注意：poll() 时已经移除了 flag，所以重新加入队列时会重新设置 flag
                        boolean added = taskQueue.add(task);
                        if (!added && task.getPriority().equals(TaskPriority.POLLING)) {
                            // POLLING任务添加失败，说明队列中已有相同sendThreadName的任务在等待
                            // 这种情况下，当前任务可以丢弃，因为队列中已有相同任务
                            // 记录日志以便调试
                            log.debug("POLLING任务添加失败（队列中已有相同任务），sendThreadName: {}, deviceId: {}, rs485Id: {}", 
                                    task.getSendThreadName(), task.getDeviceId(), task.getRs485Id());
                        } else if (added) {
                            log.debug("任务重新加入队列，sendThreadName: {}, deviceId: {}, rs485Id: {}, priority: {}", 
                                    task.getSendThreadName(), task.getDeviceId(), task.getRs485Id(), task.getPriority());
                        }
                    }
                } catch (InterruptedException e) {
                    // tryLock 或 await 被中断，重新加入队列
                    taskQueue.add(task);
                    Thread.currentThread().interrupt();
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
