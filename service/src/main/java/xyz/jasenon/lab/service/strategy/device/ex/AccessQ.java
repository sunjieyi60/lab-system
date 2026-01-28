package xyz.jasenon.lab.service.strategy.device.ex;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.command.CommandLine;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.dto.task.TaskPriority;
import xyz.jasenon.lab.common.entity.device.Access;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.service.dto.device.CreateAccess;
import xyz.jasenon.lab.service.dto.device.CreateDevice;
import xyz.jasenon.lab.service.mapper.record.AccessMapper;
import xyz.jasenon.lab.service.strategy.device.DeviceQ;
import xyz.jasenon.lab.service.strategy.device.DeviceFactory;
import xyz.jasenon.lab.service.strategy.device.PollingScheduleExecutorPool;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Component
@Slf4j
public class AccessQ extends DeviceQ<AccessMapper, Access> {

    public AccessQ(AccessMapper deviceMapper, PollingScheduleExecutorPool pollingScheduleExecutorPool) {
        super(deviceMapper, pollingScheduleExecutorPool);
    }

    @Override
    protected void register() {
        DeviceFactory.registerDeviceQMethod(DeviceType.Access, this);
    }

    @Override
    protected void afterPropertiesSet() {
        log.info("AccessQ registered");
    }

    @Override
    protected Access createDevice(CreateDevice createDevice) {
        CreateAccess createAccess = (CreateAccess) createDevice;
        Access access = (Access) new Access()
                .setAddress(createAccess.getAddress())
                .setSelfId(createAccess.getSelfId())
                .setRs485GatewayId(createAccess.getRs485GatewayId())
                .setIsLock(false)
                .setBelongToLaboratoryId(createAccess.getBelongToLaboratoryId())
                .setDeviceName(createAccess.getDeviceName())
                .setDeviceType(createAccess.getDeviceType());
        return access;
    }

    @Override
    public List<Access> list(List<Long> laboratoryIds) {
        List<Access> res = new ArrayList<>();
        for(Long laboratoryId: laboratoryIds){
            List<Access> part = super.deviceMapper.selectList(
                new LambdaQueryWrapper<Access>()
                .eq(Access::getBelongToLaboratoryId, laboratoryId)
                        .eq(Access::getDeviceType, DeviceType.Access)
            );
            res.addAll(part);
        }
        return res;
    }

    @Override
    public void startPolling(Access access) {
        Task task = new Task();
        task.setDeviceId(access.getId());
        task.setPriority(TaskPriority.AUTOMATIC);
        task.setDeviceType(DeviceType.Access);
        task.setCommandLine(CommandLine.REQUEST_ACCESS_DATA);
        task.setArgs(new Integer[] { access.getAddress(), access.getSelfId() });
        Runnable pollingTask = pollingTask(task);
        pollingScheduleExecutorPool.submit(access.getId(), pollingTask);
    }
}
