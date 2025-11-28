package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.device.Device;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.device.DeleteDevice;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
public interface IDeviceService extends IService<Device> {

    R deleteDevice(DeleteDevice deleteDevice);

}
