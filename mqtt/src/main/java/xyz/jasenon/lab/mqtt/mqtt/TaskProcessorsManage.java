package xyz.jasenon.lab.mqtt.mqtt;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.entity.device.gateway.RS485Gateway;
import xyz.jasenon.lab.mqtt.mqtt.client.MqttAcceptClient;
import xyz.jasenon.lab.mqtt.mqtt.client.MqttClientProperties;
import xyz.jasenon.lab.mqtt.mqtt.client.MqttSendClient;
import xyz.jasenon.lab.mqtt.mqtt.client.MqttMessageDispatcher;
import xyz.jasenon.lab.mqtt.service.IRS485GatewayService;
import xyz.jasenon.lab.mqtt.setnx.MqttNx;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jasenon_ce
 * @date 2025/11/25
 */
@Component
public class TaskProcessorsManage {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private IRS485GatewayService rs485GatewayService;
    @Autowired
    private MqttClientProperties mqttProperties;
    @Resource(name = "messageHandlingExecutor")
    private ThreadPoolTaskExecutor executor;
    @Autowired
    private MqttMessageDispatcher mqttMessageDispatcher;

    private final Map<Long, TaskProcessor> taskProcessors = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() throws MqttException {
        initManage();
    }

    private void initManage() throws MqttException {
        List<RS485Gateway> rs485Gateways = rs485GatewayService.listAllRS485Gateway();
        for (RS485Gateway rs485Gateway : rs485Gateways) {
            MqttNx mqttNx = new MqttNx(redissonClient);
            MqttSendClient mqttSendClient = new MqttSendClient(mqttProperties, mqttNx, rs485Gateway.getSendTopic(), rs485Gateway.getId());
            MqttAcceptClient mqttAcceptClient = new MqttAcceptClient(mqttProperties, mqttNx, rs485Gateway.getAcceptTopic(),
                    rs485Gateway.getId(), executor, mqttMessageDispatcher);
            TaskProcessor taskProcessor = new TaskProcessor(rs485Gateway.getSendTopic(), rs485Gateway.getAcceptTopic(), mqttSendClient, mqttAcceptClient);
            taskProcessors.put(rs485Gateway.getId(), taskProcessor);
        }
    }

    public void addTask(MqttTask mqttTask) {
        Long rs485Id = mqttTask.getRs485Id();
        TaskProcessor taskProcessor = taskProcessors.get(rs485Id);
        if (taskProcessor == null) {
            throw new RuntimeException("rs485Id not found");
        }
        taskProcessor.addTask(mqttTask);
    }

    public void addTaskProcessor() {
        // TODO: Implement this method
    }

    public void removeTaskProcessor() {
        // TODO: Implement this method
    };

}
