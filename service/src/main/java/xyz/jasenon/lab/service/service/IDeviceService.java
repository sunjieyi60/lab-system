package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.device.Device;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.device.DeleteDevice;
import xyz.jasenon.lab.service.vo.Rs485GatewayVo;
import xyz.jasenon.lab.service.vo.SocketGatewayVo;

import java.util.List;
import java.util.Map;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
public interface IDeviceService extends IService<Device> {

    R deleteDevice(DeleteDevice deleteDevice);

    R<Map<Long, Rs485GatewayVo>> getRs485GatewayTree();

    R<Map<Long, SocketGatewayVo>> getSocketGatewayTree();

    R<List<Device>> listDevice(List<Long> laboratoryIds, DeviceType deviceType);
}
