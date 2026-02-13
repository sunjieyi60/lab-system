package xyz.jasenon.lab.service.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.annotation.log.LogPoint;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.device.CreateDevice;
import xyz.jasenon.lab.service.dto.device.DeleteDevice;
import xyz.jasenon.lab.service.dto.device.UpdateDevice;
import xyz.jasenon.lab.service.service.IDeviceService;
import xyz.jasenon.lab.service.strategy.device.DeviceFactory;
import xyz.jasenon.lab.service.strategy.task.TaskDispatch;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Api("设备")
@RestController
@RequestMapping("/device")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class DeviceController {

    @Autowired
    private IDeviceService deviceService;

    @RequestPermission(allowed = { Permissions.DEVICE_ADD })
    @PostMapping("/create")
    @ApiOperation("创建设备")
    public R createDevice(@Validated @RequestBody CreateDevice createDevice) {
        return DeviceFactory.getDeviceQMethod(createDevice.getDeviceType())
                .insertDevice(createDevice);
    }

    @PostMapping("/list/device")
    @ApiOperation("查询设备列表")
    public R listDevice(@RequestParam List<Long> laboratoryIds,
            @RequestParam DeviceType deviceType) {
        return deviceService.listDevice(laboratoryIds, deviceType);
    }

    @RequestPermission(allowed = { Permissions.DEVICE_ADD })
    @DeleteMapping("/delete")
    @ApiOperation("删除设备")
    public R deleteDevice(@Validated @RequestBody DeleteDevice deleteDevice) {
        return deviceService.deleteDevice(deleteDevice);
    }

    @RequestPermission(allowed = { Permissions.DEVICE_ADD })
    @PutMapping("/update")
    @ApiOperation("编辑设备")
    public R updateDevice(@Validated @RequestBody UpdateDevice updateDevice) {
        return deviceService.updateDevice(updateDevice);
    }

    @RequestPermission(allowed = { Permissions.DEVICE_CONTROL })
    @PostMapping("/control")
    @ApiOperation("控制设备")
    @LogPoint(title = "'设备控制'", sqEl = "#task", clazz = Task.class)
    public R controlDevice(@Validated @RequestBody Task task) {
        task.setSendThreadName(Thread.currentThread().getName());
        TaskDispatch.dispatch(task);
        return R.success("控制任务下达成功");
    }

    @RequestPermission(allowed = { Permissions.DEVICE_ADD })
    @GetMapping("/list/rs485")
    @ApiOperation("查询 RS485 网关列表")
    public R listRs485Gateway() {
        return deviceService.getRs485GatewayTree();
    }

    @RequestPermission(allowed = { Permissions.DEVICE_ADD })
    @GetMapping("/list/socket")
    @ApiOperation("查询 Socket 网关列表")
    public R listSocketGateway() {
        return deviceService.getSocketGatewayTree();
    }

}
