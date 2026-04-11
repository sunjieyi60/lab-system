package xyz.jasenon.rsocket.server.controller.rsocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.packet.Heartbeat;
import xyz.jasenon.rsocket.core.packet.RegisterRequest;
import xyz.jasenon.rsocket.core.packet.RegisterResponse;
import xyz.jasenon.rsocket.core.packet.SetUp;
import xyz.jasenon.rsocket.core.rsocket.AbstractConnectionManager;
import xyz.jasenon.rsocket.server.service.DeviceService;

/**
 * RSocket 连接控制器
 * <p>
 * 处理班牌设备与服务器之间的 RSocket 连接生命周期管理。
 * 包括连接建立、设备注册、心跳检测等核心功能。
 * </p>
 *
 * @author Jasenon_ce
 * @see DeviceService
 * @see AbstractConnectionManager
 * @since 1.0.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ConnectController {

    /** 班牌设备服务 */
    private final DeviceService deviceService;

    /** 连接管理器，用于管理设备连接状态 */
    private final AbstractConnectionManager manager;

    /**
     * 处理 RSocket Setup 连接建立
     * <p>
     * 在 RSocket 连接建立时首先调用，用于验证和处理连接请求。
     * </p>
     * <ul>
     *   <li>返回 {@code Mono.empty()} 表示接受连接</li>
     *   <li>返回 {@code Mono.error()} 表示拒绝连接</li>
     * </ul>
     *
     * @param requester RSocket 请求者对象，代表客户端连接
     * @param setUp Setup 帧携带的数据，包含连接元数据
     * @return 空 Mono 表示接受连接
     */
    @ConnectMapping
    public Mono<Void> logSetup(RSocketRequester requester, @Payload SetUp setUp) {
        log.info("✅ 收到 Setup 帧，Data内容: {}", setUp);
        log.info("连接对象: " + requester);
        return Mono.empty(); // 返回空表示接受连接
    }

    /**
     * 处理设备注册请求
     * <p>
     * 设备完成自身初始化后，通过此接口向服务器注册。
     * 注册成功后，设备连接将被纳入连接管理器统一管理。
     * </p>
     *
     * @param request 设备注册请求，包含 UUID 和实验室ID等信息
     * @param requester RSocket 请求者对象，用于保存设备连接
     * @return 注册响应，包含设备配置信息和注册状态
     */
    @MessageMapping(Const.Route.DEVICE_REGISTER)
    public Mono<RegisterResponse> registerResponse(
            @Payload RegisterRequest request,
            RSocketRequester requester
            ){

        log.info("收到设备注册请求: uuid={}, laboratoryId={}",
                request.getUuid(), request.getLaboratoryId());

        return deviceService.register(request)
                .map(resp -> {
                    manager.register(resp.getUuid(), requester);
                    return resp;
                });
    }

    /**
     * 处理设备心跳（已弃用）
     * <p>
     * <strong>注意：此方法已弃用。</strong>
     * RSocket 协议本身提供了协议级的心跳机制，无需应用层额外实现。
     * </p>
     *
     * @param heartbeat 心跳包数据
     * @return 空响应
     * @deprecated 使用 RSocket 协议级心跳替代
     */
    @Deprecated
    @MessageMapping(Const.Route.DEVICE_HEARTBEAT)
    public Mono<Void> heartbeat(
            @Payload Heartbeat heartbeat
            ){
        log.debug("接收到传来的心跳包{}",heartbeat.getUuid());
        return Mono.empty();
    }

}
