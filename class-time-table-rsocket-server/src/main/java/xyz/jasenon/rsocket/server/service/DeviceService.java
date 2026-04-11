package xyz.jasenon.rsocket.server.service;

import com.github.yulichang.base.MPJBaseService;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.model.ClassTimeTable;
import xyz.jasenon.rsocket.core.packet.UpdateConfigRequest;
import xyz.jasenon.rsocket.core.packet.Heartbeat;
import xyz.jasenon.rsocket.core.packet.RegisterRequest;
import xyz.jasenon.rsocket.core.packet.RegisterResponse;
import xyz.jasenon.rsocket.core.packet.UpdateConfigResponse;

import java.util.List;

/**
 * 班牌设备服务接口
 * <p>
 * 定义班牌设备的核心业务操作，包括设备注册、状态管理、配置更新等功能。
 * 继承自 MyBatis Plus 的 {@link MPJBaseService}，提供基础 CRUD 能力。
 * </p>
 *
 * @author Jasenon_ce
 * @see ClassTimeTable
 * @see DeviceServiceImpl
 * @since 1.0.0
 */
public interface DeviceService extends MPJBaseService<ClassTimeTable> {

    /**
     * 设备注册
     * <p>
     * 处理班牌设备的注册请求。如果设备不存在则自动创建设备记录，
     * 如果设备已存在则更新相关信息。注册成功后返回设备配置信息。
     * </p>
     *
     * @param request 设备注册请求，包含设备UUID和实验室ID
     * @return 注册响应的异步 Mono，包含设备配置和注册状态
     */
    Mono<RegisterResponse> register(RegisterRequest request);

    /**
     * 处理设备心跳（已弃用）
     * <p>
     * <strong>注意：此方法已弃用。</strong>
     * RSocket 协议本身提供了协议级的心跳机制。
     * </p>
     *
     * @param heartbeat 心跳包数据
     * @return 空响应的异步 Mono
     * @deprecated 使用 RSocket 协议级心跳替代
     */
    Mono<Void> heartbeat(Heartbeat heartbeat);

    /**
     * 查询设备列表
     * <p>
     * 根据状态条件查询班牌设备列表。支持按在线/离线状态过滤，
     * 不传状态参数则查询所有设备。
     * </p>
     *
     * @param status 设备状态过滤条件，可选值为 ONLINE/OFFLINE，null 表示查询所有
     * @return 设备列表
     * @throws xyz.jasenon.lab.common.exception.BusinessException 当状态参数不合法时抛出
     */
    List<ClassTimeTable> listAll(String status);

    /**
     * 更新设备配置并推送到设备
     * <p>
     * 更新数据库中的设备配置信息，如果设备当前在线，
     * 将配置通过 RSocket 实时推送到设备端。
     * </p>
     *
     * @param request 配置更新请求，包含设备UUID、配置信息等
     * @return 配置推送响应的异步 Mono，包含推送结果状态
     */
    Mono<UpdateConfigResponse> updateConfigAndPush(UpdateConfigRequest request);

    /**
     * 更新设备关联实验室并推送档案
     * <p>
     * 更新设备所属的实验室信息，如果设备当前在线，
     * 将实验室变更同步推送到设备端（推送内容包含配置和实验室信息）。
     * </p>
     *
     * @param uuid 设备UUID
     * @param laboratoryId 目标实验室ID
     * @return 档案推送结果的异步 Mono，包含更新状态和推送结果
     */
    Mono<UpdateConfigResponse> updateLaboratoryAndPush(String uuid, Long laboratoryId);

}
