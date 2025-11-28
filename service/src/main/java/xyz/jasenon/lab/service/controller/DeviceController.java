package xyz.jasenon.lab.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.device.CreateDevice;
import xyz.jasenon.lab.service.dto.device.DeleteDevice;
import xyz.jasenon.lab.service.service.IDeviceService;
import xyz.jasenon.lab.service.strategy.device.DeviceCreateFactory;

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

}
