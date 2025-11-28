package xyz.jasenon.lab.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.device.CreateDevice;
import xyz.jasenon.lab.service.dto.device.DeleteDevice;
import xyz.jasenon.lab.service.service.IDeviceService;
import xyz.jasenon.lab.service.strategy.device.DeviceCreateFactory;
import xyz.jasenon.lab.service.strategy.task.TaskDispatch;

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

    @PostMapping("/create")
    public R createDevice(@RequestBody CreateDevice createDevice){
        return DeviceCreateFactory.getDeviceCreateStrategy(createDevice.getDeviceType())
                .insertDevice(createDevice);
    }

    @DeleteMapping("/delete")
    public R deleteDevice(@RequestBody DeleteDevice deleteDevice){
        return deviceService.deleteDevice(deleteDevice);
    }

    @PostMapping("/control")
    public R controlDevice(@RequestBody Task task){
        TaskDispatch.dispatch(task);
        return R.success("控制任务下达成功");
    }

    @GetMapping("/list/rs485")
    public R listRs485Gateway(){
        return deviceService.getRs485GatewayTree();
    }

    @GetMapping("/list/socket")
    public R listSocketGateway(){
        return deviceService.getSocketGatewayTree();
    }

}
