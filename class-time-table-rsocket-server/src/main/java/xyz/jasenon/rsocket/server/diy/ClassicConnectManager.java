package xyz.jasenon.rsocket.server.diy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.cache.Cache;
import xyz.jasenon.rsocket.core.model.ClassTimeTable;
import xyz.jasenon.rsocket.core.rsocket.AbstractConnectionManager;
import xyz.jasenon.rsocket.core.utils.AsyncExecutor;
import xyz.jasenon.rsocket.server.mapper.DeviceMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClassicConnectManager extends AbstractConnectionManager implements Const.Key {

    private final DeviceMapper deviceMapper;
    private final Cache cache;

    /**
     * 实现注册回调  刷新设备状态
     * @param deviceUUID 设备uuid
     */
    @Override
    public void onAfterRegister(@NotNull String deviceUUID) {
        ClassTimeTable device = deviceMapper.selectOne(
                new LambdaQueryWrapper<ClassTimeTable>()
                        .eq(ClassTimeTable::getUuid, deviceUUID)
                            .last("for update")
                                .select(ClassTimeTable::getId)
        );
        device.setStatus(Const.Status.ONLINE);
        AsyncExecutor.runAsyncIO(() -> {
            deviceMapper.updateById(device);
            log.info("设备:{},已上线", deviceUUID);
            cache.delete(CLASS_TIME_TABLE + SUFFIX + UUID + SUFFIX + deviceUUID + SUFFIX + INFO);
        });
    }

    /**
     * 实现离线回调  刷新设备状态
     * @param deviceUUID 设备uuid
     */
    @Override
    public void onAfterClose(@NotNull String deviceUUID) {
        ClassTimeTable device = deviceMapper.selectOne(
                new LambdaQueryWrapper<ClassTimeTable>()
                        .eq(ClassTimeTable::getUuid, deviceUUID)
                            .last("for update")
                                .select(ClassTimeTable::getId)
        );
        device.setStatus(Const.Status.OFFLINE);
        AsyncExecutor.runAsyncIO(()->{
            deviceMapper.updateById(device);
            log.info("设备:{},已离线",deviceUUID);
            cache.delete(CLASS_TIME_TABLE + SUFFIX + UUID + SUFFIX + deviceUUID + SUFFIX + INFO);
        });
    }
}
