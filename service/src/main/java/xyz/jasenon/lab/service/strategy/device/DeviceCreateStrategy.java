package xyz.jasenon.lab.service.strategy.device;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.device.CreateDevice;
import xyz.jasenon.lab.service.strategy.task.TaskDispatch;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
public abstract class DeviceCreateStrategy<M extends BaseMapper<T>,T> {

    private final M deviceMapper;
    protected final PollingScheduleExecutorPool pollingScheduleExecutorPool;

    public DeviceCreateStrategy(M deviceMapper, PollingScheduleExecutorPool pollingScheduleExecutorPool) {
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

    protected abstract void startPolling(T t);

    protected Runnable pollingTask(Task task){
        return () ->{
            TaskDispatch.dispatch(task);
        };
    }
}
