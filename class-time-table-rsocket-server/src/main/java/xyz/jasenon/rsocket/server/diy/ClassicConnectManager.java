package xyz.jasenon.rsocket.server.diy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.cache.Cache;
import xyz.jasenon.rsocket.core.model.ClassTimeTable;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.rsocket.AbstractConnectionManager;
import xyz.jasenon.rsocket.core.utils.AsyncExecutor;
import xyz.jasenon.rsocket.server.mapper.DeviceMapper;

import java.time.Duration;

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

    /**
     * 发送消息到指定设备并等待响应
     *
     * @param uuid         设备UUID
     * @param message      消息（继承 Message）
     * @param responseType 响应类型
     * @param <T>          请求消息类型
     * @param <R>          响应消息类型
     * @return Mono<R> 设备响应
     */
    public <T extends Message, R extends Message> Mono<R> sendTo(String uuid, T message, Class<R> responseType) {
        RSocketRequester requester = getRequester(uuid);
        if (requester == null) {
            log.warn("设备 {} 不在线，无法发送消息", uuid);
            return Mono.error(new RuntimeException("设备不在线"));
        }
        
        // 设置目标设备
        message.setTo(uuid);
        message.setFrom(Const.Role.SERVER);
        
        String route = message.getRoute();
        if (route == null || route.isEmpty()) {
            return Mono.error(new IllegalArgumentException("消息 route 不能为空"));
        }
        
        log.info("向设备 {} 发送消息，route: {}", uuid, route);
        
        return requester.route(route)
                .data(message)
                .retrieveMono(responseType)
                .timeout(Duration.ofMillis(Const.Default.COMMAND_TIMEOUT))
                .doOnSuccess(resp -> log.info("设备 {} 响应成功: {}", uuid, resp.isSuccess()))
                .doOnError(e -> log.error("向设备 {} 发送消息失败: {}", uuid, e.getMessage()));
    }

}
