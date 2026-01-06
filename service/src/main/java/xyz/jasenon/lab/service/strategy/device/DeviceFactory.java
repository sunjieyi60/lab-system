package xyz.jasenon.lab.service.strategy.device;

import xyz.jasenon.lab.common.entity.device.DeviceType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
public class DeviceFactory {

    private final static Map<DeviceType, DeviceQ> STRATEGY = new ConcurrentHashMap<>();

    public static DeviceQ getDeviceQMethod(DeviceType deviceType) {
        return STRATEGY.get(deviceType);
    }

    public static void registerDeviceQMethod(DeviceType deviceType, DeviceQ<?,?> strategy) {
        STRATEGY.put(deviceType,strategy);
    }

}
