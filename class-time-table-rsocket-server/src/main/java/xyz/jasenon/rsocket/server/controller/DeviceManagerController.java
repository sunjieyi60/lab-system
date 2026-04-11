package xyz.jasenon.rsocket.server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.rsocket.core.model.ClassTimeTable;
import xyz.jasenon.rsocket.server.dto.EditDeviceLaboratory;
import xyz.jasenon.rsocket.server.dto.PushDeviceConfig;
import xyz.jasenon.rsocket.server.service.DeviceService;
import xyz.jasenon.rsocket.server.vo.DeviceProfilePushResultVo;
import xyz.jasenon.rsocket.server.vo.PushConfigResult;

import java.util.List;

/**
 * 班牌设备管理控制器
 * <p>
 * 提供班牌设备的管理接口，包括设备列表查询、配置下发、实验室关联修改等功能。
 * 所有接口返回统一的响应格式 {@link R}。
 * </p>
 *
 * @author Jasenon_ce
 * @see DeviceService
 * @since 1.0.0
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/manager")
public class DeviceManagerController {

    /** 班牌设备服务 */
    private final DeviceService deviceService;

    /**
     * 查询班牌设备列表
     * <p>
     * 支持按状态过滤设备，状态可选值为：ONLINE（在线）、OFFLINE（离线）
     * </p>
     *
     * @param status 设备状态过滤条件，可选参数。不传则查询所有设备
     * @return 统一响应格式的设备列表
     */
    @GetMapping("/list")
    public R<List<ClassTimeTable>> listAll(@RequestParam(value = "status", required = false) String status){
        return R.success(deviceService.listAll(status));
    }

    /**
     * 下发班牌配置
     * <p>
     * 更新设备的配置信息，如果设备在线，将配置实时推送到设备端。
     * 配置内容包括设备显示设置、网络参数等。
     * </p>
     *
     * @param body 设备配置下发请求体，包含设备UUID和配置信息
     * @return 配置推送结果的异步响应，包含推送状态和设备响应信息
     */
    @PostMapping("/device/config")
    public Mono<R<DeviceProfilePushResultVo>> pushDeviceConfig(@Valid @RequestBody PushDeviceConfig body) {
//        return deviceService.updateConfigAndPush(body.getUuid(), body.getConfig()).map(PushConfigResult::toR);
        return null;
    }

    /**
     * 修改班牌关联实验室
     * <p>
     * 更新班牌设备所属的实验室信息，如果设备在线，将实验室变更实时同步到设备端。
     * 实验室变更可能影响设备上显示的课程表等信息。
     * </p>
     *
     * @param body 实验室修改请求体，包含设备UUID和目标实验室ID
     * @return 档案推送结果的异步响应，包含更新状态和推送结果
     */
    @PatchMapping("/device/laboratory")
    public Mono<R<DeviceProfilePushResultVo>> updateDeviceLaboratory(@Valid @RequestBody EditDeviceLaboratory body) {
//        return deviceService.updateLaboratoryAndPush(body.getUuid(), body.getLaboratoryId()).map(PushConfigResult::toR);
        return null;
    }


}
