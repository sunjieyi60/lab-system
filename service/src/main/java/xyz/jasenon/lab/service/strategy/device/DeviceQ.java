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
public abstract class DeviceQ<M extends BaseMapper<T>,T extends Device > {

    protected final M deviceMapper;
    protected final PollingScheduleExecutorPool pollingScheduleExecutorPool;

    public DeviceQ(M deviceMapper, PollingScheduleExecutorPool pollingScheduleExecutorPool) {
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

    public T getDeviceById(Long id){
        return deviceMapper.selectById(id);
    }

    public abstract List<T> list(List<Long> laboratoryIds);

    public abstract void startPolling(T t);

    protected Runnable pollingTask(Task task){
        return () ->{
            T device = deviceMapper.selectById(task.getDeviceId());
            if (device == null){
                log.warn("轮询任务对应的设备已不存在，终止轮询任务! deviceId:{}", task.getDeviceId());
                // 抛出异常以终止 ScheduledThreadPoolExecutor 的后续调度
                // 这是设计意图：当设备被删除时，通过异常来停止该设备的轮询任务
                throw new RuntimeException("轮询任务对应的设备已不存在! deviceId: " + task.getDeviceId());
            }
            log.info("开始执行轮询任务:{}",task);
            TaskDispatch.dispatch(task);
        };
    }
}
