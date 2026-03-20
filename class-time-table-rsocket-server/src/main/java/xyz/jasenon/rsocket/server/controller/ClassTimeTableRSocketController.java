package xyz.jasenon.rsocket.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.model.Config;
import xyz.jasenon.rsocket.core.packet.*;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Status;
import xyz.jasenon.rsocket.core.rsocket.ConnectionManager;
import xyz.jasenon.rsocket.server.service.ClassTimeTableService;

import java.time.Duration;
import java.time.Instant;

/**
 * 智慧班牌 RSocket 控制器
 * 
 * 处理设备注册、心跳、具体业务命令
 * 使用 Message<T> 包装具体业务 DTO
 */
@Slf4j
@Controller
public class ClassTimeTableRSocketController {

    @Autowired
    private ClassTimeTableService deviceService;

    @Autowired
    private ConnectionManager connectionManager;

    /**
     * RSocket 连接建立
     */
    @ConnectMapping
    public Mono<Void> handleConnect(RSocketRequester requester) {
        log.debug("新的 RSocket 连接建立");
        
        requester.rsocket()
                .onClose()
                .doOnTerminate(() -> log.debug("RSocket 连接关闭"))
                .subscribe();
                
        return Mono.empty();
    }

    // ==================== 基础通信 ====================

    /**
     * 设备注册
     * 
     * 使用 Message<RegisterRequest> 包装请求
     * 返回 Message<RegisterResponse> 包装响应
     */
    @MessageMapping("device.register")
    public Mono<Message<RegisterResponse>> register(
            @Payload Message<RegisterRequest> message,
            RSocketRequester requester) {
        
        RegisterRequest request = message.getPayload();
        log.info("收到设备注册请求: uuid={}, laboratoryId={}", 
                request.getUuid(), request.getLaboratoryId());

        return deviceService.register(request)
                .map(response -> {
                    // 注册成功，保存连接
                    connectionManager.registerConnection(response.getDeviceDbId(), requester);
                    log.info("设备 {} 注册成功，配置已下发", request.getUuid());
                    
                    return wrapResponse(Message.Type.REQUEST_RESPONSE, response, Status.C10000);
                })
                .onErrorResume(e -> {
                    log.error("设备注册失败: {}", e.getMessage(), e);
                    RegisterResponse errorResponse = new RegisterResponse();
                    return Mono.just(wrapResponse(Message.Type.REQUEST_RESPONSE, errorResponse, Status.C10000));
                });
    }

    /**
     * 设备心跳
     * 
     * 使用 Message<HeartbeatPayload> 包装
     */
    @MessageMapping("device.heartbeat")
    public Mono<Message<Heartbeat>> heartbeat(
            @Payload Message<Heartbeat> message,
            RSocketRequester requester) {
        
        Heartbeat request = message.getPayload();
        Long deviceDbId = request.getDeviceDbId();
        
        if (deviceDbId == null) {
            log.warn("收到无效心跳请求：deviceDbId为空");
            Heartbeat response = new Heartbeat();
            response.setInterval(Const.Default.HEARTBEAT_INTERVAL);
            return Mono.just(wrapResponse(Message.Type.REQUEST_RESPONSE, response, Status.C10000));
        }

        log.debug("收到设备 {} 心跳", deviceDbId);

        // 更新设备在线状态
        return deviceService.updateStatus(deviceDbId, Const.Status.ONLINE)
                .then(Mono.defer(() -> {
                    Heartbeat response = new Heartbeat();
                    response.setDeviceDbId(deviceDbId);
                    response.setInterval(Const.Default.HEARTBEAT_INTERVAL);
                    response.setConfigUpdated(false);
                    return Mono.just(wrapResponse(Message.Type.REQUEST_RESPONSE, response, Status.C10000));
                }))
                .onErrorResume(e -> {
                    log.error("处理设备 {} 心跳失败: {}", deviceDbId, e.getMessage());
                    Heartbeat response = new Heartbeat();
                    response.setDeviceDbId(deviceDbId);
                    response.setInterval(Const.Default.HEARTBEAT_INTERVAL);
                    return Mono.just(wrapResponse(Message.Type.REQUEST_RESPONSE, response, Status.C10000));
                });
    }

    /**
     * 更新配置请求处理
     * 
     * 接收 Message<UpdateConfigRequest>，返回 Message<UpdateConfigResponse>
     */
    @MessageMapping("device.config.update")
    public Mono<Message<UpdateConfigResponse>> handleUpdateConfig(@Payload Message<UpdateConfigRequest> message) {
        UpdateConfigRequest request = message.getPayload();
        log.info("收到更新配置请求: immediate={}", request.getImmediate());
        
        // 实际业务逻辑：更新设备本地配置
        UpdateConfigResponse response = UpdateConfigResponse.success(request.getVersion());
        
        return Mono.just(wrapResponse(Message.Type.REQUEST_RESPONSE, response, Status.C10000));
    }

    /**
     * 更新课表请求处理
     * 
     * 接收 Message<UpdateScheduleRequest>，返回 Message<UpdateScheduleResponse>
     */
    @MessageMapping("device.schedule.update")
    public Mono<Message<UpdateScheduleResponse>> handleUpdateSchedule(@Payload Message<UpdateScheduleRequest> message) {
        UpdateScheduleRequest request = message.getPayload();
        log.info("收到更新课表请求: version={}", request.getScheduleVersion());
        
        // 实际业务逻辑：更新课表
        UpdateScheduleResponse response = UpdateScheduleResponse.success(request.getScheduleVersion());
        
        return Mono.just(wrapResponse(Message.Type.REQUEST_RESPONSE, response, Status.C10000));
    }

    /**
     * 双向通信通道（Request-Channel）
     */
    @MessageMapping("device.channel")
    public Flux<Message<Object>> deviceChannel(
            @Payload Flux<Message<Object>> deviceEvents,
            RSocketRequester requester) {
        
        log.info("建立双向通信通道");
        
        // 处理设备发送的事件
        deviceEvents.subscribe(
                event -> log.debug("收到设备事件: {}", event.getPayload()),
                error -> log.error("设备事件流错误: {}", error.getMessage()),
                () -> log.info("设备事件流完成")
        );
        
        // 返回心跳流
        return Flux.interval(Duration.ofSeconds(10))
                .map(tick -> {
                    Heartbeat heartbeat = new Heartbeat();
                    heartbeat.setInterval(Const.Default.HEARTBEAT_INTERVAL);
                    return wrapResponse(Message.Type.REQUEST_CHANNEL, heartbeat, Status.C10000);
                });
    }

    // ==================== 辅助方法 ====================

    /**
     * 包装响应为 Message<T>
     */
    private <T> Message<T> wrapResponse(Message.Type type, T payload, Status status) {
        Message<T> message = new Message<>();
        message.setType(type);
        message.setPayload(payload);
        message.setStatus(status);
        message.setTimestamp(Instant.now());
        return message;
    }
}
