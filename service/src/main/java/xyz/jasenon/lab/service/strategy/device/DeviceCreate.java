package xyz.jasenon.lab.service.strategy.device;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.entity.device.Device;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.device.CreateDevice;
import xyz.jasenon.lab.service.strategy.task.TaskDispatch;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Slf4j
public abstract class DeviceCreate<M extends BaseMapper<T>,T extends Device > {

    protected final M deviceMapper;
    protected final PollingScheduleExecutorPool pollingScheduleExecutorPool;

    public DeviceCreate(M deviceMapper, PollingScheduleExecutorPool pollingScheduleExecutorPool) {
        this.deviceMapper = deviceMapper;
        register();
        afterPropertiesSet();
        this.pollingScheduleExecutorPool = pollingScheduleExecutorPool;
    }

    protected abstract void register();

    protected abstract void afterPropertiesSet();

    protected abstract T createDevice(CreateDevice createDevice);

    public final R insertDevice(CreateDevice createDevice){
        T device = createDevice(createDevice);
        deviceMapper.insert(device);
        startPolling(device);
        return R.success("创建设备成功");
    }

    public abstract List<T> list(List<Long> laboratoryIds);

    public abstract void startPolling(T t);

    protected Runnable pollingTask(Task task){
        return () ->{
            T device = deviceMapper.selectById(task.getDeviceId());
            if (device == null){
                log.info("device:{}不存在!",device);
                throw new RuntimeException("轮询任务对应的设备已不存在!");
            }
            TaskDispatch.dispatch(task);
        };
    }
}
