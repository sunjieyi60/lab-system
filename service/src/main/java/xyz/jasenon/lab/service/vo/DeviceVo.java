package xyz.jasenon.lab.service.vo;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.device.Device;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Getter
@Setter
public class DeviceVo {
    /**
     * 设备信息
     */
    private Device device;
    /**
     * 设备记录信息
     */
    private DeviceRecordVo deviceRecord;

    public Long getDeviceId() {
        return device.getId();
    }
}
