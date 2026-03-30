package xyz.jasenon.lab.service.strategy.device.ex;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.command.CommandLine;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.dto.task.TaskPriority;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.device.Sensor;
import xyz.jasenon.lab.common.entity.record.SensorRecord;
import xyz.jasenon.lab.service.dto.device.CreateDevice;
import xyz.jasenon.lab.service.dto.device.CreateSensor;
import xyz.jasenon.lab.service.mapper.record.SensorMapper;
import xyz.jasenon.lab.service.mapper.record.SensorRecordMapper;
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
public class SensorQ extends DeviceQ<SensorMapper, Sensor> {
    private final SensorRecordMapper sensorRecordMapper;

    public SensorQ(SensorMapper deviceMapper,
                   PollingScheduleExecutorPool pollingScheduleExecutorPool,
                   SensorRecordMapper sensorRecordMapper) {
        super(deviceMapper, pollingScheduleExecutorPool);
        this.sensorRecordMapper = sensorRecordMapper;
    }

    @Override
    protected void register() {
        DeviceFactory.registerDeviceQMethod(DeviceType.Sensor, this);
    }

    @Override
    protected void afterPropertiesSet() {
        log.info("SensorQ registered");
    }

    @Override
    protected Sensor createDevice(CreateDevice createDevice) {
        CreateSensor createSensor = (CreateSensor) createDevice;
        Sensor sensor = (Sensor) new Sensor()
                .setAddress(createSensor.getAddress())
                .setSelfId(createSensor.getSelfId())
                .setRs485GatewayId(createSensor.getRs485GatewayId())
                .setDeviceName(createSensor.getDeviceName())
                .setDeviceType(createSensor.getDeviceType())
                .setBelongToLaboratoryId(createSensor.getBelongToLaboratoryId());
        return sensor;
    }

    @Override
    public List<Sensor> list(List<Long> laboratoryIds) {
        List<Sensor> res = new ArrayList<>();
        for(Long laboratoryId: laboratoryIds){
            List<Sensor> part = super.deviceMapper.selectList(
                new LambdaQueryWrapper<Sensor>()
                .eq(Sensor::getBelongToLaboratoryId, laboratoryId)
                        .eq(Sensor::getDeviceType, DeviceType.Sensor)
            );
            res.addAll(part);
        }
        return res;
    }

    @Override
    public void startPolling(Sensor sensor) {
        Task task = new Task();
        task.setPriority(TaskPriority.POLLING);
        task.setDeviceId(sensor.getId());
        task.setDeviceType(DeviceType.Sensor);
        task.setCommandLine(CommandLine.REQUEST_SENSOR_DATA);
        task.setArgs(new Integer[]{sensor.getAddress(), sensor.getSelfId()});
        task.setDevice(sensor);
        Runnable pollingTask = pollingTask(task);
        pollingScheduleExecutorPool.submit(sensor.getId(), pollingTask);
    }

    @Override
    protected void initDefaultRecord(Sensor device) {
        SensorRecord record = new SensorRecord();
        record.setDeviceId(device.getId());
        record.setAddress(device.getAddress());
        record.setSelfId(device.getSelfId());
        record.setTemperature(0.0);
        record.setHumidity(0.0);
        record.setLight(0.0);
        record.setSmoke(0);
        sensorRecordMapper.insert(record);
    }
}
