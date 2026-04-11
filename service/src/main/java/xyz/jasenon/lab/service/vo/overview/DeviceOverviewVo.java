package xyz.jasenon.lab.service.vo.overview;

import lombok.Data;
import xyz.jasenon.lab.common.entity.device.Device;
import xyz.jasenon.lab.common.entity.device.DeviceType;

import java.util.List;

@Data
public class DeviceOverviewVo {
    /**
     * 设备信息
     */
    private List<Device> devices;
    /**
     * 总数
     */
    private Integer total;
    /**
     * 在线数
     */
    private Integer online;

}
