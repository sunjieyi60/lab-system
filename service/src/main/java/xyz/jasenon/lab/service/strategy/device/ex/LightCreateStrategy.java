package xyz.jasenon.lab.service.strategy.device.ex;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import xyz.jasenon.lab.common.command.CommandLine;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.dto.task.TaskPriority;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.device.Light;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.device.CreateDevice;
import xyz.jasenon.lab.service.dto.device.CreateLight;
import xyz.jasenon.lab.service.mapper.LightMapper;
import xyz.jasenon.lab.service.strategy.device.DeviceCreateFactory;
import xyz.jasenon.lab.service.strategy.device.DeviceCreateStrategy;
import xyz.jasenon.lab.service.strategy.device.PollingScheduleExecutorPool;

import java.util.ArrayList;
import java.util.List;

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
                .setAddress(createLight.getAddress())
                .setRs485GatewayId(createLight.getRs485GatewayId())
                .setIsLock(false)
                .setDeviceName(createLight.getDeviceName())
                .setDeviceType(createLight.getDeviceType())
                .setBelongToLaboratoryId(createLight.getBelongToLaboratoryId());
        return light;
    }

    @Override
    public List<Light> list(List<Long> laboratoryIds) {
        List<Light> res = new ArrayList<>();
        for(Long laboratoryId: laboratoryIds){
            List<Light> part = super.deviceMapper.selectList(
                new LambdaQueryWrapper<Light>()
                .eq(Light::getBelongToLaboratoryId, laboratoryId)
            );
            res.addAll(part);
        }
        return res;
    }

    @Override
    public void startPolling(Light light) {
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
