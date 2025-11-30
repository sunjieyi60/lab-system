package xyz.jasenon.lab.service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.device.CreateDevice;
import xyz.jasenon.lab.service.dto.device.DeleteDevice;
import xyz.jasenon.lab.service.service.IDeviceService;
import xyz.jasenon.lab.service.strategy.device.DeviceCreateFactory;
import xyz.jasenon.lab.service.strategy.task.TaskDispatch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@RestController
@RequestMapping("/device")
@CrossOrigin("*")
public class DeviceController {

    @Autowired
    private IDeviceService deviceService;

    @RequestPermission(allowed = { Permissions.DEVICE_ADD })
    @PostMapping("/create")
    @Operation(summary = "创建设备", requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "CreateAirCondition", value = "{\n  \"deviceType\": \"AirCondition\",\n  \"address\": 35,\n  \"selfId\": 1,\n  \"rs485GatewayId\": 5,\n  \"socketGatewayId\": null,\n  \"belongToLaboratoryId\": 101\n}"))))
    public R createDevice(@RequestBody CreateDevice createDevice) {
        return DeviceCreateFactory.getDeviceCreateStrategy(createDevice.getDeviceType())
                .insertDevice(createDevice);
    }

    @PostMapping("/list/device")
    public R listDevice(@RequestParam List<Long> laboratoryIds,
            @RequestParam DeviceType deviceType) {
        return deviceService.listDevice(laboratoryIds, deviceType);
    }

    @RequestPermission(allowed = { Permissions.DEVICE_ADD })
    @DeleteMapping("/delete")
    public R deleteDevice(@RequestBody DeleteDevice deleteDevice) {
        return deviceService.deleteDevice(deleteDevice);
    }

    @RequestPermission(allowed = { Permissions.DEVICE_CONTROL })
    @PostMapping("/control")
    @Operation(summary = "下发设备控制任务", requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "ControlLight", value = "{\n  \"priority\": \"NORMAL\",\n  \"deviceType\": \"Light\",\n  \"deviceId\": 1001,\n  \"commandLine\": \"OPEN_LIGHT\",\n  \"args\": [41, 1]\n}"))))
    public R controlDevice(@RequestBody Task task) {
        TaskDispatch.dispatch(task);
        return R.success("控制任务下达成功");
    }

    @RequestPermission(allowed = { Permissions.DEVICE_ADD })
    @GetMapping("/list/rs485")
    public R listRs485Gateway() {
        return deviceService.getRs485GatewayTree();
    }

    @RequestPermission(allowed = { Permissions.DEVICE_ADD })
    @GetMapping("/list/socket")
    public R listSocketGateway() {
        return deviceService.getSocketGatewayTree();
    }

}
