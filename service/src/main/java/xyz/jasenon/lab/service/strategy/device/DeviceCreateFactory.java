package xyz.jasenon.lab.service.strategy.device;

import xyz.jasenon.lab.common.entity.device.DeviceType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
public class DeviceCreateFactory {

    private final static Map<DeviceType, DeviceCreate> STRATEGY = new ConcurrentHashMap<>();

    public static DeviceCreate getDeviceCreateMethod(DeviceType deviceType) {
        return STRATEGY.get(deviceType);
    }

    public static void registerDeviceCreateMethod(DeviceType deviceType, DeviceCreate<?,?> strategy) {
        STRATEGY.put(deviceType,strategy);
    }

}
