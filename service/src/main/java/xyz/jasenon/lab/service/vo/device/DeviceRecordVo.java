package xyz.jasenon.lab.service.vo.device;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.device.DeviceType;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Getter
@Setter
public class DeviceRecordVo<T> {

    private T data;
    private DeviceType deviceType;

}
