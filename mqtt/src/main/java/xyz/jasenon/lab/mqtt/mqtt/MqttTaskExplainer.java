package xyz.jasenon.lab.mqtt.mqtt;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.entity.device.Access;
import xyz.jasenon.lab.common.entity.device.AirCondition;
import xyz.jasenon.lab.common.entity.device.CircuitBreak;
import xyz.jasenon.lab.common.entity.device.Light;
import xyz.jasenon.lab.common.entity.device.Sensor;
import xyz.jasenon.lab.common.entity.device.gateway.RS485Gateway;
import xyz.jasenon.lab.common.explain.TaskExplain;
import xyz.jasenon.lab.mqtt.service.IDeviceService;
import xyz.jasenon.lab.mqtt.service.IRS485GatewayService;

@Component
public class MqttTaskExplainer extends TaskExplain<MqttTask> {

    private final IRS485GatewayService rs485GatewayService;

    public MqttTaskExplainer(IDeviceService deviceService, IRS485GatewayService rs485GatewayService) {
        super(deviceService);
        this.rs485GatewayService = rs485GatewayService;
    }

    @Override
    public MqttTask explainTask(Task task, AirCondition airCondition) {
        RS485Gateway gateway = rs485GatewayService.getRS485GatewayByGatewayId(airCondition.getRs485GatewayId());
        Assert.notNull(gateway, "找不到对应的网关");
        MqttTask mqttTask = new MqttTask(task)
                .rs485Id(gateway.getId())
                .payload(generatePayload(task, airCondition));
        return mqttTask;
    }

    @Override
    public MqttTask explainTask(Task task, Light light) {
        RS485Gateway gateway = rs485GatewayService.getRS485GatewayByGatewayId(light.getRs485GatewayId());
        Assert.notNull(gateway, "找不到对应的网关");
        MqttTask mqttTask = new MqttTask(task)
                .rs485Id(gateway.getId())
                .payload(generatePayload(task, light));
        return mqttTask;
    }

    @Override
    public MqttTask explainTask(Task task, Sensor sensor) {
        RS485Gateway gateway = rs485GatewayService.getRS485GatewayByGatewayId(sensor.getRs485GatewayId());
        Assert.notNull(gateway, "找不到对应的网关");
        MqttTask mqttTask = new MqttTask(task)
                .rs485Id(gateway.getId())
                .payload(generatePayload(task, sensor));
        return mqttTask;
    }

    @Override
    public MqttTask explainTask(Task task, Access access) {
        RS485Gateway gateway = rs485GatewayService.getRS485GatewayByGatewayId(access.getRs485GatewayId());
        Assert.notNull(gateway, "找不到对应的网关");
        MqttTask mqttTask = new MqttTask(task)
                .rs485Id(gateway.getId())
                .payload(generatePayload(task, access));
        return mqttTask;
    }

    @Override
    public MqttTask explainTask(Task task, CircuitBreak circuitBreak) {
        RS485Gateway gateway = rs485GatewayService.getRS485GatewayByGatewayId(circuitBreak.getRs485GatewayId());
        Assert.notNull(gateway, "找不到对应的网关");
        MqttTask mqttTask = new MqttTask(task)
                .rs485Id(gateway.getId())
                .payload(generatePayload(task, circuitBreak));
        return mqttTask;
    }

}
