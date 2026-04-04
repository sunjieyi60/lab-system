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

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/manager")
public class DeviceManagerController {

    private final DeviceService deviceService;

    /**
     * 查询设备列表
     */
    @GetMapping("/list")
    public R<List<ClassTimeTable>> listAll(@RequestParam(value = "status", required = false) String status){
        return R.success(deviceService.listAll(status));
    }

    /**
     * 下发班牌配置
     */
    @PostMapping("/device/config")
    public Mono<R<DeviceProfilePushResultVo>> pushDeviceConfig(@Valid @RequestBody PushDeviceConfig body) {
        return deviceService.updateConfigAndPush(body.getUuid(), body.getConfig()).map(PushConfigResult::toR);
    }

    /**
     * 修改班牌关联实验室
     */
    @PatchMapping("/device/laboratory")
    public Mono<R<DeviceProfilePushResultVo>> updateDeviceLaboratory(@Valid @RequestBody EditDeviceLaboratory body) {
        return deviceService.updateLaboratoryAndPush(body.getUuid(), body.getLaboratoryId()).map(PushConfigResult::toR);
    }

}
