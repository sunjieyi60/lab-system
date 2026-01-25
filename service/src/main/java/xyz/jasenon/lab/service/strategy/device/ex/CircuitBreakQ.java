package xyz.jasenon.lab.service.strategy.device.ex;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.command.CommandLine;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.dto.task.TaskPriority;
import xyz.jasenon.lab.common.entity.device.CircuitBreak;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.service.dto.device.CreateCircuitBreak;
import xyz.jasenon.lab.service.dto.device.CreateDevice;
import xyz.jasenon.lab.service.mapper.record.CircuitBreakMapper;
import xyz.jasenon.lab.service.strategy.device.DeviceFactory;
import xyz.jasenon.lab.service.strategy.device.DeviceQ;
import xyz.jasenon.lab.service.strategy.device.PollingScheduleExecutorPool;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Component
@Slf4j
public class CircuitBreakQ extends DeviceQ<CircuitBreakMapper, CircuitBreak> {
    public CircuitBreakQ(CircuitBreakMapper deviceMapper, PollingScheduleExecutorPool pollingScheduleExecutorPool) {
        super(deviceMapper, pollingScheduleExecutorPool);
    }

    @Override
    protected void register() {
        DeviceFactory.registerDeviceQMethod(DeviceType.CircuitBreak, this);
    }

    @Override
    protected void afterPropertiesSet() {
        log.info("CircuitBreakRecordQ registered");
    }

    @Override
    protected CircuitBreak createDevice(CreateDevice createDevice) {
        CreateCircuitBreak createCircuitBreak = (CreateCircuitBreak) createDevice;
        CircuitBreak circuitBreak = (CircuitBreak) new CircuitBreak()
                .setAddress(createCircuitBreak.getAddress())
                .setRs485GatewayId(createCircuitBreak.getRs485GatewayId())
                .setDeviceName(createCircuitBreak.getDeviceName())
                .setDeviceType(createCircuitBreak.getDeviceType())
                .setBelongToLaboratoryId(createCircuitBreak.getBelongToLaboratoryId());
        return circuitBreak;
    }

    @Override
    public List<CircuitBreak> list(List<Long> laboratoryIds) {
        List<CircuitBreak> res = new ArrayList<>();
        for(Long laboratoryId: laboratoryIds){
            List<CircuitBreak> part = super.deviceMapper.selectList(
                new LambdaQueryWrapper<CircuitBreak>()
                .eq(CircuitBreak::getBelongToLaboratoryId, laboratoryId)
            );
            res.addAll(part);
        }
        return res;
    }

    @Override
    public void startPolling(CircuitBreak circuitBreak) {
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
