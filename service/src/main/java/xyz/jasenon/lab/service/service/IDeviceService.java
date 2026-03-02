package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.device.Device;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.device.DeleteDevice;
import xyz.jasenon.lab.service.dto.device.UpdateDevice;
import xyz.jasenon.lab.service.vo.device.DeviceVo;
import xyz.jasenon.lab.service.vo.device.Rs485GatewayVo;
import xyz.jasenon.lab.service.vo.device.SocketGatewayVo;

import java.util.List;
import java.util.Map;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
public interface IDeviceService extends IService<Device> {

    R deleteDevice(DeleteDevice deleteDevice);

    R updateDevice(UpdateDevice updateDevice);

    R<Map<Long, List<Rs485GatewayVo>>> getRs485GatewayTree();

    R<Map<Long, List<SocketGatewayVo>>> getSocketGatewayTree();

    R<Map<Long,List<DeviceVo>>> listDevice(List<Long> laboratoryIds, DeviceType deviceType);

    /**
     * 开启指定设备的轮询
     */
    R enablePolling(Long deviceId);

    /**
     * 关闭指定设备的轮询
     */
    R disablePolling(Long deviceId);
}
