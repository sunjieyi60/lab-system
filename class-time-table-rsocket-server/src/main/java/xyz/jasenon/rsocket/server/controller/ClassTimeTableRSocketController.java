//package xyz.jasenon.rsocket.server.controller;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.messaging.rsocket.RSocketRequester;
//import org.springframework.messaging.rsocket.annotation.ConnectMapping;
//import org.springframework.stereotype.Controller;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import xyz.jasenon.rsocket.core.Const;
//import xyz.jasenon.rsocket.core.packet.*;
//import xyz.jasenon.rsocket.core.protocol.Command;
//import xyz.jasenon.rsocket.core.protocol.Message;
//import xyz.jasenon.rsocket.core.protocol.Status;
//import xyz.jasenon.rsocket.core.rsocket.ConnectionManager;
//import xyz.jasenon.rsocket.server.service.ClassTimeTableService;
//
//import java.time.Duration;
//import java.time.Instant;
//
///**
// * 智慧班牌 RSocket 控制器 - 自有协议
// *
// * 处理设备注册、心跳、具体业务命令
// * 支持四种 RSocket 交互模式：
// * 1. REQUEST_RESPONSE - 请求-响应（注册、配置查询）
// * 2. FIRE_AND_FORGET - 单向发送（心跳）
// * 3. REQUEST_STREAM - 请求流（实时数据推送）
// * 4. REQUEST_CHANNEL - 双向流（实时双向通信）
// *
// * 注意：所有 packet 类（如 RegisterRequest, RegisterResponse 等）都继承自 Message
// * 使用自有协议的 Status 包装（code=10000 表示成功）
// */
//@Slf4j
//@Controller
//public class ClassTimeTableRSocketController {
//
//    @Autowired
//    private ClassTimeTableService deviceService;
//
//    @Autowired
//    private ConnectionManager connectionManager;
//
//    /**
//     * RSocket 连接建立
//     */
//    @ConnectMapping
//    public Mono<Void> handleConnect(RSocketRequester requester) {
//        log.info("新的 RSocket 连接建立");
//
//        requester.rsocket()
//                .onClose()
//                .doOnTerminate(() -> log.warn("RSocket 连接关闭"))
//                .subscribe();
//
//        return Mono.empty();
//    }
//
//    // ==================== 1. REQUEST_RESPONSE - 请求响应 ====================
//
//    /**
//     * 设备注册
//     *
//     * 接收 RegisterRequest（继承自 Message）
//     * 返回 RegisterResponse（继承自 Message）
//     * 使用 uuid 作为设备唯一标识
//     */
//    @MessageMapping("device.register")
//    public Mono<RegisterResponse> register(
//            @Payload RegisterRequest request,
//            RSocketRequester requester) {
//
//        log.info("收到设备注册请求: uuid={}, laboratoryId={}",
//                request.getUuid(), request.getLaboratoryId());
//
//        return deviceService.register(request)
//                .map(response -> {
//                    // 注册成功，保存连接（使用 uuid 作为唯一标识）
//                    connectionManager.register(response.getUuid(), requester);
//                    log.info("设备 {} 注册成功，配置已下发", response.getUuid());
//
//                    // 设置自有协议 Status 包装
//                    response.setCommand(Command.REGISTER);
//                    response.setFrom(Const.Role.SERVER);
//                    response.setTo(request.getUuid());
//                    response.setStatus(Status.C10000, "注册成功");
//                    response.setTimestamp(Instant.now());
//                    return response;
//                });
//    }
//
//    /**
//     * 更新配置请求处理
//     */
//    @MessageMapping("device.config.update")
//    public Mono<UpdateConfigResponse> handleUpdateConfig(
//            @Payload UpdateConfigRequest request) {
//
//        log.info("收到更新配置请求: immediate={}", request.getImmediate());
//
//        // 实际业务逻辑：更新设备本地配置
//        UpdateConfigResponse response = UpdateConfigResponse.success(request.getVersion());
//        response.setStatus(Status.C10000, "配置更新成功");
//        response.setTimestamp(Instant.now());
//        return Mono.just(response);
//    }
//
//    // ==================== 2. FIRE_AND_FORGET - 单向发送 ====================
//
//    /**
//     * 设备心跳
//     *
//     * 接收 Heartbeat（继承自 Message）
//     * 使用 uuid 作为设备唯一标识
//     */
//    @MessageMapping("device.heartbeat")
//    public Mono<Void> heartbeat(
//            @Payload Heartbeat heartbeat,
//            RSocketRequester requester) {
//
//        String uuid = heartbeat.getUuid();
//
//        if (uuid == null || uuid.isEmpty()) {
//            log.warn("收到无效心跳请求：uuid为空");
//            return Mono.empty().then(Mono.defer(
//                    () -> requester.rsocket().onClose()
//            ));
//        }
//
//        log.debug("收到设备 {} 心跳", uuid);
//
//        return deviceService.updateStatus(uuid, Const.Status.ONLINE)
//                .then();
//    }
//
//    // ==================== 3. REQUEST_STREAM - 请求流 ====================
//
//    /**
//     * 测试流接口 - 服务器向客户端推送多条消息
//     *
//     * 客户端请求后，服务器持续推送数据（如传感器数据、状态更新等）
//     */
//    @MessageMapping("stream.test")
//    public Flux<Message> testStream(@Payload Message message) {
//        log.info("收到流请求: {}", message);
//
//        // 模拟持续推送5条消息，间隔500ms
//        return Flux.interval(Duration.ofMillis(500))
//                .take(5)
//                .map(i -> {
//                    String data = "Stream message #" + (i + 1);
//                    log.debug("发送流数据: {}", data);
//                    // 使用自有协议 Status
//                    Message response = new Message();
//                    response.setStatus(Status.C10000, data);
//                    response.setTimestamp(Instant.now());
//                    return response;
//                });
//    }
//
//    /**
//     * 设备状态流 - 实时监控设备状态变化
//     */
//    @MessageMapping("stream.device.status")
//    public Flux<Message> deviceStatusStream(@Payload Message message) {
//        // 从 extras 中获取 uuid
//        String uuid = message.getExtra("uuid");
//        if (uuid == null) {
//            log.warn("设备状态流请求缺少 uuid");
//            return Flux.empty();
//        }
//        log.info("设备 {} 订阅状态流", uuid);
//
//        // 模拟每2秒推送一次状态更新
//        return Flux.interval(Duration.ofSeconds(2))
//                .take(10)  // 最多推送10次
//                .map(i -> {
//                    String status = i % 2 == 0 ? "ONLINE" : "BUSY";
//                    Message response = new Message();
//                    response.setStatus(Status.C10000, "Device " + uuid + " status: " + status);
//                    response.setTimestamp(Instant.now());
//                    return response;
//                })
//                .doOnCancel(() -> log.info("设备 {} 取消状态流订阅", uuid));
//    }
//
//    // ==================== 4. REQUEST_CHANNEL - 双向流 ====================
//
//    /**
//     * 心跳通道 - 双向实时通信
//     *
//     * 客户端持续发送心跳，服务端持续响应确认
//     * 适用于需要保持长连接并实时交互的场景
//     */
//    @MessageMapping("channel.heartbeat")
//    public Flux<Heartbeat> heartbeatChannel(
//            @Payload Flux<Heartbeat> heartbeats) {
//
//        log.info("建立心跳双向流通道");
//
//        return heartbeats
//                .doOnNext(hb -> log.debug("收到设备 {} 心跳，间隔 {} 秒",
//                        hb.getUuid(), hb.getInterval()))
//                .map(request -> {
//                    // 更新设备状态
//                    deviceService.updateStatus(request.getUuid(), Const.Status.ONLINE)
//                            .subscribe();
//
//                    // 返回确认心跳（使用自有协议 Status）
//                    Heartbeat response = new Heartbeat();
//                    response.setUuid(request.getUuid());
//                    response.setInterval(request.getInterval());
//                    response.setConfigUpdated(false);  // 通知客户端是否有配置更新
//                    response.setStatus(Status.C10000, "心跳成功");
//                    response.setTimestamp(Instant.now());
//                    return response;
//                })
//                .doOnCancel(() -> log.info("心跳通道关闭"))
//                .doOnError(e -> log.error("心跳通道错误", e));
//    }
//
//    /**
//     * 实时命令通道 - 服务端可以向客户端发送控制命令
//     *
//     * 客户端建立连接后，服务端可以随时下发命令（开门、重启等）
//     */
//    @MessageMapping("channel.command")
//    public Flux<Message> commandChannel(
//            @Payload Flux<Message> clientMessages,
//            RSocketRequester requester) {
//
//        log.info("建立命令双向流通道");
//
//        // 处理客户端消息
//        clientMessages
//                .doOnNext(msg -> log.info("收到客户端消息: code={}, msg={}", msg.getCode(), msg.getMsg()))
//                .subscribe();
//
//        // 模拟服务端向客户端推送命令
//        return Flux.interval(Duration.ofSeconds(5))
//                .take(3)
//                .map(i -> {
//                    if (i == 0) {
//                        // 发送配置更新命令
//                        UpdateConfigRequest cmd = new UpdateConfigRequest();
//                        cmd.setVersion(System.currentTimeMillis());
//                        cmd.setStatus(Status.C10000, "配置更新命令");
//                        cmd.setTimestamp(Instant.now());
//                        return cmd;
//                    } else {
//                        // 发送心跳检查
//                        Message ping = new Message();
//                        ping.setStatus(Status.C10000, "PING");
//                        ping.setTimestamp(Instant.now());
//                        return ping;
//                    }
//                })
//                .doOnCancel(() -> log.info("命令通道关闭"));
//    }
//}
