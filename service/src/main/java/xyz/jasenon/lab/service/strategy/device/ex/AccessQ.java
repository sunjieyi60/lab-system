package xyz.jasenon.lab.service.strategy.device.ex;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.command.CommandLine;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.dto.task.TaskPriority;
import xyz.jasenon.lab.common.entity.device.Access;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.record.AccessRecord;
import xyz.jasenon.lab.service.dto.device.CreateAccess;
import xyz.jasenon.lab.service.dto.device.CreateDevice;
import xyz.jasenon.lab.service.mapper.record.AccessMapper;
import xyz.jasenon.lab.service.mapper.record.AccessRecordMapper;
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
public class AccessQ extends DeviceQ<AccessMapper, Access> {
    private final AccessRecordMapper accessRecordMapper;

    public AccessQ(AccessMapper deviceMapper,
                   PollingScheduleExecutorPool pollingScheduleExecutorPool,
                   AccessRecordMapper accessRecordMapper) {
        super(deviceMapper, pollingScheduleExecutorPool);
        this.accessRecordMapper = accessRecordMapper;
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
        Long duplicated = deviceMapper.selectCount(new LambdaQueryWrapper<Access>()
                .eq(Access::getDeviceType, DeviceType.Access)
                .eq(Access::getRs485GatewayId, createAccess.getRs485GatewayId())
                .eq(Access::getAddress, createAccess.getAddress()));
        if (duplicated != null && duplicated > 0) {
            throw new IllegalArgumentException("同一RS485网关下该地址的门禁已存在");
        }
        Access access = (Access) new Access()
                .setAddress(createAccess.getAddress())
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
        task.setPriority(TaskPriority.POLLING);
        task.setDeviceType(DeviceType.Access);
        task.setCommandLine(CommandLine.REQUEST_ACCESS_DATA);
        task.setArgs(new Integer[] { access.getAddress() });
        task.setDevice(access);
        Runnable pollingTask = pollingTask(task);
        pollingScheduleExecutorPool.submit(access.getId(), pollingTask);
    }

    @Override
    protected void initDefaultRecord(Access device) {
        AccessRecord record = new AccessRecord();
        record.setDeviceId(device.getId());
        record.setAddress(device.getAddress());
        record.setIsOpen(false);
        record.setIsLock(Boolean.TRUE.equals(device.getIsLock()));
        record.setLockStatus(0);
        record.setDelayTime(0);
        accessRecordMapper.insert(record);
    }
}
