package xyz.jasenon.lab.service.strategy.device.ex;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.command.CommandLine;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.dto.task.TaskPriority;
import xyz.jasenon.lab.common.entity.device.AirCondition;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.service.dto.device.CreateAirCondition;
import xyz.jasenon.lab.service.dto.device.CreateDevice;
import xyz.jasenon.lab.service.mapper.AirConditionMapper;
import xyz.jasenon.lab.service.strategy.device.DeviceCreateFactory;
import xyz.jasenon.lab.service.strategy.device.DeviceCreateStrategy;
import xyz.jasenon.lab.service.strategy.device.PollingScheduleExecutorPool;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Component
@Slf4j
public class AirConditionCreateStrategy extends DeviceCreateStrategy<AirConditionMapper, AirCondition> {

    public AirConditionCreateStrategy(AirConditionMapper deviceMapper, PollingScheduleExecutorPool pollingScheduleExecutorPool) {
        super(deviceMapper, pollingScheduleExecutorPool);
    }

    @Override
    protected void register() {
        DeviceCreateFactory.registerDeviceCreateStrategy(DeviceType.AirCondition,this);
    }

    @Override
    protected void afterPropertiesSet() {
        log.info("AirConditionCreateStrategy registered");
    }

    @Override
    protected AirCondition createDevice(CreateDevice createDevice) {
        CreateAirCondition createAirCondition = (CreateAirCondition) createDevice;
        boolean mustHaveOneGateway = createAirCondition.getRs485GatewayId()!=null || createAirCondition.socketGatewayId()!=null;
        if (!mustHaveOneGateway) {
            throw new IllegalArgumentException("rs485GatewayId or socketGatewayId must be set");
        }
        AirCondition airCondition = (AirCondition) new AirCondition()
                .setAddress(createAirCondition.getAddress())
                .setSelfId(createAirCondition.getSelfId())
                .setRs485GatewayId(createAirCondition.getRs485GatewayId())
                .setSocketGatewayId(createAirCondition.getSocketGatewayId())
                .setIsLock(false)
                .setDeviceName(createAirCondition.getDeviceName())
                .setDeviceType(createAirCondition.getDeviceType())
                .setBelongToLaboratoryId(createAirCondition.getBelongToLaboratoryId());
        return airCondition;
    }

    @Override
    public List<AirCondition> list(List<Long> laboratoryIds) {
        return null;
    }

    @Override
    public void startPolling(AirCondition airCondition) {
        Task task = new Task();
        if (airCondition.getRs485GatewayId() != null){
            task.setPriority(TaskPriority.AUTOMATIC);
            task.setDeviceId(airCondition.getId());
            task.setDeviceType(DeviceType.AirCondition);
            task.setCommandLine(CommandLine.REQUEST_AIR_CONDITION_DATA_RS485);
            task.setArgs(new Integer[]{airCondition.getAddress(), airCondition.getSelfId()});
        }
        if (airCondition.getSocketGatewayId() != null){
            task.setPriority(TaskPriority.AUTOMATIC);
            task.setDeviceId(airCondition.getId());
            task.setDeviceType(DeviceType.AirCondition);
            task.setCommandLine(CommandLine.REQUEST_AIR_CONDITION_DATA_SOCKET);
            task.setArgs(new Integer[]{airCondition.getAddress(), airCondition.getSelfId()});
        }
        Runnable pollingTask = pollingTask(task);
        pollingScheduleExecutorPool.submit(pollingTask);
    }
}
