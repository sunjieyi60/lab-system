package xyz.jasenon.lab.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.device.Device;

public interface IDeviceService$ extends IService<Device> {

    Device getDeviceByDeviceId(Long id);

}