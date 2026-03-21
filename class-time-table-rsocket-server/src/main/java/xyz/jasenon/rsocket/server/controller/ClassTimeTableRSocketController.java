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
 * 支持四种 RSocket 交互模式：
 * 1. REQUEST_RESPONSE - 请求-响应（注册、配置查询）
 * 2. FIRE_AND_FORGET - 单向发送（心跳）
 * 3. REQUEST_STREAM - 请求流（实时数据推送）
 * 4. REQUEST_CHANNEL - 双向流（实时双向通信）
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

    // ==================== 1. REQUEST_RESPONSE - 请求响应 ====================

    /**
     * 设备注册
     *
     * 使用 Message<RegisterRequest> 包装请求
     * 返回 Message<RegisterResponse> 包装响应
     * 使用 uuid 作为设备唯一标识
     */
    @MessageMapping("device.register")
    public Mono<Message<RegisterResponse>> register(
            @Payload Message<RegisterRequest> message,
            RSocketRequester requester) {

        RegisterRequest request = message.getData();
        log.info("收到设备注册请求: uuid={}, laboratoryId={}",
                request.getUuid(), request.getLaboratoryId());

        return deviceService.register(request)
                .map(response -> {
                    // 注册成功，保存连接（使用 uuid 作为唯一标识）
                    connectionManager.register(response.getUuid(), requester);
                    log.info("设备 {} 注册成功，配置已下发", response.getUuid());

                    return Message.<RegisterResponse>builder()
                            .from(Const.Role.SERVER)
                            .to(request.getUuid())
                            .type(Message.Type.REQUEST_RESPONSE)
                            .data(response)
                            .status(Status.C10000)
                            .timestamp(Instant.now())
                            .build();
                });
    }

    /**
     * 更新配置请求处理
     */
    @MessageMapping("device.config.update")
    public Mono<Message<UpdateConfigResponse>> handleUpdateConfig(
            @Payload Message<UpdateConfigRequest> message) {
        
        UpdateConfigRequest request = message.getData();
        log.info("收到更新配置请求: immediate={}", request.getImmediate());

        // 实际业务逻辑：更新设备本地配置
        return Mono.just(
                Message.<UpdateConfigResponse>builder()
                        .type(Message.Type.REQUEST_RESPONSE)
                        .data(UpdateConfigResponse.success(request.getVersion()))
                        .status(Status.C10000)
                        .timestamp(Instant.now())
                        .build()
        );
    }

//    /**
//     * 更新课表请求处理
//     */
//    @MessageMapping("device.schedule.update")
//    public Mono<Message<UpdateScheduleResponse>> handleUpdateSchedule(
//            @Payload Message<UpdateScheduleRequest> message) {
//
//        UpdateScheduleRequest request = message.getData();
//        log.info("收到更新课表请求: version={}", request.getScheduleVersion());
//
//        return Mono.just(
//                Message.<UpdateScheduleResponse>builder()
//                        .type(Message.Type.REQUEST_RESPONSE)
//                        .data(UpdateScheduleResponse.success(request.getScheduleVersion()))
//                        .status(Status.C10000)
//                        .timestamp(Instant.now())
//                        .build()
//        );
//    }

    // ==================== 2. FIRE_AND_FORGET - 单向发送 ====================

    /**
     * 设备心跳
     *
     * 使用 Message<Heartbeat> 包装
     * 使用 uuid 作为设备唯一标识
     */
    @MessageMapping("device.heartbeat")
    public Mono<Void> heartbeat(
            @Payload Message<Heartbeat> message,
            RSocketRequester requester) {

        Heartbeat request = message.getData();
        String uuid = request.getUuid();

        if (uuid == null || uuid.isEmpty()) {
            log.warn("收到无效心跳请求：uuid为空");
            return Mono.empty().then(Mono.defer(
                    () -> requester.rsocket().onClose()
            ));
        }

        log.debug("收到设备 {} 心跳", uuid);

        return deviceService.updateStatus(uuid, Const.Status.ONLINE)
                .then();
    }

    // ==================== 3. REQUEST_STREAM - 请求流 ====================

    /**
     * 测试流接口 - 服务器向客户端推送多条消息
     * 
     * 客户端请求后，服务器持续推送数据（如传感器数据、状态更新等）
     */
    @MessageMapping("stream.test")
    public Flux<Message<String>> testStream(@Payload Message<String> message) {
        log.info("收到流请求: {}", message.getData());

        // 模拟持续推送5条消息，间隔500ms
        return Flux.interval(Duration.ofMillis(500))
                .take(5)
                .map(i -> {
                    String data = "Stream message #" + (i + 1);
                    log.debug("发送流数据: {}", data);
                    return Message.<String>builder()
                            .type(Message.Type.REQUEST_STREAM)
                            .data(data)
                            .status(Status.C10000)
                            .timestamp(Instant.now())
                            .build();
                });
    }

    /**
     * 设备状态流 - 实时监控设备状态变化
     */
    @MessageMapping("stream.device.status")
    public Flux<Message<String>> deviceStatusStream(@Payload Message<String> message) {
        String uuid = message.getData();
        log.info("设备 {} 订阅状态流", uuid);

        // 模拟每2秒推送一次状态更新
        return Flux.interval(Duration.ofSeconds(2))
                .take(10)  // 最多推送10次
                .map(i -> {
                    String status = i % 2 == 0 ? "ONLINE" : "BUSY";
                    return Message.<String>builder()
                            .type(Message.Type.REQUEST_STREAM)
                            .data("Device " + uuid + " status: " + status)
                            .status(Status.C10000)
                            .timestamp(Instant.now())
                            .build();
                })
                .doOnCancel(() -> log.info("设备 {} 取消状态流订阅", uuid));
    }

    // ==================== 4. REQUEST_CHANNEL - 双向流 ====================

    /**
     * 心跳通道 - 双向实时通信
     * 
     * 客户端持续发送心跳，服务端持续响应确认
     * 适用于需要保持长连接并实时交互的场景
     */
    @MessageMapping("channel.heartbeat")
    public Flux<Message<Heartbeat>> heartbeatChannel(
            @Payload Flux<Message<Heartbeat>> heartbeats) {
        
        log.info("建立心跳双向流通道");

        return heartbeats
                .doOnNext(msg -> {
                    Heartbeat hb = msg.getData();
                    log.debug("收到设备 {} 心跳，间隔 {} 秒", 
                            hb.getUuid(), hb.getInterval());
                })
                .map(msg -> {
                    Heartbeat request = msg.getData();
                    
                    // 更新设备状态
                    deviceService.updateStatus(request.getUuid(), Const.Status.ONLINE)
                            .subscribe();
                    
                    // 返回确认心跳
                    Heartbeat response = new Heartbeat();
                    response.setUuid(request.getUuid());
                    response.setInterval(request.getInterval());
                    response.setConfigUpdated(false);  // 通知客户端是否有配置更新
                    
                    return Message.<Heartbeat>builder()
                            .type(Message.Type.REQUEST_CHANNEL)
                            .data(response)
                            .status(Status.C10000)
                            .timestamp(Instant.now())
                            .build();
                })
                .doOnCancel(() -> log.info("心跳通道关闭"))
                .doOnError(e -> log.error("心跳通道错误", e));
    }

    /**
     * 实时命令通道 - 服务端可以向客户端发送控制命令
     * 
     * 客户端建立连接后，服务端可以随时下发命令（开门、重启等）
     */
    @MessageMapping("channel.command")
    public Flux<Message<?>> commandChannel(
            @Payload Flux<Message<String>> clientMessages,
            RSocketRequester requester) {
        
        log.info("建立命令双向流通道");

        // 处理客户端消息
        clientMessages
                .doOnNext(msg -> log.info("收到客户端消息: {}", msg.getData()))
                .subscribe();

        // 模拟服务端向客户端推送命令
        return Flux.interval(Duration.ofSeconds(5))
                .take(3)
                .<Message<?>>map(i -> {
                    if (i == 0) {
                        // 发送配置更新命令
                        UpdateConfigRequest cmd = new UpdateConfigRequest();
                        cmd.setVersion(System.currentTimeMillis());
                        
                        return Message.<UpdateConfigRequest>builder()
                                .type(Message.Type.REQUEST_CHANNEL)
                                .data(cmd)
                                .status(Status.C10000)
                                .timestamp(Instant.now())
                                .build();
                    } else {
                        // 发送心跳检查
                        return Message.<String>builder()
                                .type(Message.Type.REQUEST_CHANNEL)
                                .data("PING")
                                .status(Status.C10000)
                                .timestamp(Instant.now())
                                .build();
                    }
                })
                .doOnCancel(() -> log.info("命令通道关闭"));
    }
}
