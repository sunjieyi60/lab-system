package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.device.Device;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.device.DeleteDevice;
import xyz.jasenon.lab.service.mapper.DeviceMapper;
import xyz.jasenon.lab.service.service.IDeviceService;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements IDeviceService {

    @Override
    public R deleteDevice(DeleteDevice deleteDevice) {
        baseMapper.deleteById(deleteDevice.getDeviceId());
        return R.success("删除成功");
    }

}
