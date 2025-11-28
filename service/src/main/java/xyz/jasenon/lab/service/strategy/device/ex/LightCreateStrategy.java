package xyz.jasenon.lab.service.strategy.device.ex;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.dto.command.CommandLine;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.dto.task.TaskPriority;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.device.Light;
import xyz.jasenon.lab.service.dto.device.CreateDevice;
import xyz.jasenon.lab.service.dto.device.CreateLight;
import xyz.jasenon.lab.service.mapper.LightMapper;
import xyz.jasenon.lab.service.strategy.device.DeviceCreateFactory;
import xyz.jasenon.lab.service.strategy.device.DeviceCreateStrategy;
import xyz.jasenon.lab.service.strategy.device.PollingScheduleExecutorPool;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Component
@Slf4j
public class LightCreateStrategy extends DeviceCreateStrategy<LightMapper, Light> {
    public LightCreateStrategy(LightMapper deviceMapper, PollingScheduleExecutorPool pollingScheduleExecutorPool) {
        super(deviceMapper, pollingScheduleExecutorPool);
    }

    @Override
    protected void register() {
        DeviceCreateFactory.registerDeviceCreateStrategy(DeviceType.Light, this);
    }

    @Override
    protected void afterPropertiesSet() {
        log.info("LightCreateStrategy registered");
    }

    @Override
    protected Light createDevice(CreateDevice createDevice) {
        CreateLight createLight = (CreateLight) createDevice;
        Light light = (Light) new Light()
                .setAddress(createLight.address())
                .setRs485GatewayId(createLight.rs485GatewayId())
                .setIsLock(false)
                .setDeviceName(createLight.getDeviceName())
                .setDeviceType(createLight.getDeviceType())
                .setBelongToLaboratoryId(createLight.getBelongToLaboratoryId());
        return light;
    }

    @Override
    protected void startPolling(Light light) {
        Task task = new Task();
        task.setPriority(TaskPriority.AUTOMATIC);
        task.setDeviceId(light.getId());
        task.setCommandLine(CommandLine.REQUEST_LIGHT_DATA);
        task.setDeviceType(DeviceType.Light);
        task.setArgs(new Integer[]{light.getAddress(), light.getSelfId()});
        Runnable pollingTask = pollingTask(task);
        pollingScheduleExecutorPool.submit(pollingTask);
    }
}
