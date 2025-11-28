package xyz.jasenon.lab.service.strategy.device.ex;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.dto.command.CommandLine;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.dto.task.TaskPriority;
import xyz.jasenon.lab.common.entity.device.CircuitBreak;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.service.dto.device.CreateCircuitBreak;
import xyz.jasenon.lab.service.dto.device.CreateDevice;
import xyz.jasenon.lab.service.mapper.CircuitBreakMapper;
import xyz.jasenon.lab.service.strategy.device.DeviceCreateFactory;
import xyz.jasenon.lab.service.strategy.device.DeviceCreateStrategy;
import xyz.jasenon.lab.service.strategy.device.PollingScheduleExecutorPool;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Component
@Slf4j
public class CircuitBreakCreateStrategy extends DeviceCreateStrategy<CircuitBreakMapper, CircuitBreak> {
    public CircuitBreakCreateStrategy(CircuitBreakMapper deviceMapper, PollingScheduleExecutorPool pollingScheduleExecutorPool) {
        super(deviceMapper, pollingScheduleExecutorPool);
    }

    @Override
    protected void register() {
        DeviceCreateFactory.registerDeviceCreateStrategy(DeviceType.CircuitBreak, this);
    }

    @Override
    protected void afterPropertiesSet() {
        log.info("CircuitBreakCreateStrategy registered");
    }

    @Override
    protected CircuitBreak createDevice(CreateDevice createDevice) {
        CreateCircuitBreak createCircuitBreak = (CreateCircuitBreak) createDevice;
        CircuitBreak circuitBreak = (CircuitBreak) new CircuitBreak()
                .setAddress(createCircuitBreak.address())
                .setRs485GatewayId(createCircuitBreak.rs485GatewayId())
                .setDeviceName(createCircuitBreak.getDeviceName())
                .setDeviceType(createCircuitBreak.getDeviceType())
                .setBelongToLaboratoryId(createCircuitBreak.getBelongToLaboratoryId());
        return circuitBreak;
    }

    @Override
    protected void startPolling(CircuitBreak circuitBreak) {
        Task task = new Task();
        task.setDeviceType(DeviceType.CircuitBreak);
        task.setDeviceId(circuitBreak.getId());
        task.setPriority(TaskPriority.AUTOMATIC);
        task.setCommandLine(CommandLine.REQUEST_CIRCUITBREAK_DATA);
        task.setArgs(new Integer[]{circuitBreak.getAddress()});
        Runnable pollingTask = pollingTask(task);
        pollingScheduleExecutorPool.submit(pollingTask);

    }
}
