package xyz.jasenon.lab.service.strategy.device.ex;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.command.CommandLine;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.dto.task.TaskPriority;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.device.Light;
import xyz.jasenon.lab.common.entity.record.LightRecord;
import xyz.jasenon.lab.service.dto.device.CreateDevice;
import xyz.jasenon.lab.service.dto.device.CreateLight;
import xyz.jasenon.lab.service.mapper.record.LightMapper;
import xyz.jasenon.lab.service.mapper.record.LightRecordMapper;
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
public class LightQ extends DeviceQ<LightMapper, Light> {
    private final LightRecordMapper lightRecordMapper;

    public LightQ(LightMapper deviceMapper,
                  PollingScheduleExecutorPool pollingScheduleExecutorPool,
                  LightRecordMapper lightRecordMapper) {
        super(deviceMapper, pollingScheduleExecutorPool);
        this.lightRecordMapper = lightRecordMapper;
    }

    @Override
    protected void register() {
        DeviceFactory.registerDeviceQMethod(DeviceType.Light, this);
    }

    @Override
    protected void afterPropertiesSet() {
        log.info("LightQ registered");
    }

    @Override
    protected Light createDevice(CreateDevice createDevice) {
        CreateLight createLight = (CreateLight) createDevice;
        Long duplicated = deviceMapper.selectCount(new LambdaQueryWrapper<Light>()
                .eq(Light::getDeviceType, DeviceType.Light)
                .eq(Light::getRs485GatewayId, createLight.getRs485GatewayId())
                .eq(Light::getAddress, createLight.getAddress())
                .eq(Light::getSelfId, createLight.getSelfId()));
        if (duplicated != null && duplicated > 0) {
            throw new IllegalArgumentException("同一RS485网关下该地址的selfId已存在");
        }
        Light light = (Light) new Light()
                .setAddress(createLight.getAddress())
                .setSelfId(createLight.getSelfId())
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
                        .eq(Light::getDeviceType, DeviceType.Light)
            );
            res.addAll(part);
        }
        return res;
    }

    @Override
    public void startPolling(Light light) {
        Task task = new Task();
        task.setPriority(TaskPriority.POLLING);
        task.setDeviceId(light.getId());
        task.setCommandLine(CommandLine.REQUEST_LIGHT_DATA);
        task.setDeviceType(DeviceType.Light);
        task.setArgs(new Integer[]{light.getAddress(), light.getSelfId()});
        task.setDevice(light);
        Runnable pollingTask = pollingTask(task);
        pollingScheduleExecutorPool.submit(light.getId(), pollingTask);
    }

    @Override
    protected void initDefaultRecord(Light device) {
        LightRecord record = new LightRecord();
        record.setDeviceId(device.getId());
        record.setAddress(device.getAddress());
        record.setSelfId(device.getSelfId());
        record.setIsOpen(false);
        record.setIsLock(Boolean.TRUE.equals(device.getIsLock()));
        lightRecordMapper.insert(record);
    }
}
