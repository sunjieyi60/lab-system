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

    /**
     * 创建设备后初始化一条默认记录。
     * 子类可按设备类型覆盖该方法；默认不处理。
     */
    protected void initDefaultRecord(T device) {
    }

    public final R insertDevice(CreateDevice createDevice){
        try {
            T device = createDevice(createDevice);
            deviceMapper.insert(device);
            initDefaultRecord(device);
            if (Boolean.TRUE.equals(device.getPollingEnabled())) {
                startPolling(device);
            }
            return R.success("创建设备成功");
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    public T getDeviceById(Long id){
        return deviceMapper.selectById(id);
    }

    /**
     * 按设备ID启动轮询（编辑设备时「启用检测」用）
     */
    public void startPollingById(Long deviceId) {
        T device = getDeviceById(deviceId);
        if (device != null) {
            startPolling(device);
        }
    }

    public abstract List<T> list(List<Long> laboratoryIds);

    public abstract void startPolling(T t);

    protected Runnable pollingTask(Task task){
        return () ->{
            task.setSendThreadName(Thread.currentThread().getName());
            T device = deviceMapper.selectById(task.getDeviceId());
            if (device == null){
                log.warn("轮询任务对应的设备已不存在，终止轮询任务! deviceId:{}", task.getDeviceId());
                throw new RuntimeException("轮询任务对应的设备已不存在! deviceId: " + task.getDeviceId());
            }
            if (!Boolean.TRUE.equals(device.getPollingEnabled())) {
                log.info("设备已取消检测，终止轮询任务 deviceId:{}", task.getDeviceId());
                pollingScheduleExecutorPool.cancelPolling(task.getDeviceId());
                return;
            }
            log.info("开始执行轮询任务:{}",task);
            TaskDispatch.dispatch(task);
        };
    }
}
