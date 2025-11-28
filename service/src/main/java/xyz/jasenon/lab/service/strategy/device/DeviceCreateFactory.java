package xyz.jasenon.lab.service.strategy.device;

import xyz.jasenon.lab.common.entity.device.DeviceType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
public class DeviceCreateFactory {

    private final static Map<DeviceType,DeviceCreateStrategy<?,?>> STRATEGY = new ConcurrentHashMap<>();

    public static DeviceCreateStrategy<?,?> getDeviceCreateStrategy(DeviceType deviceType) {
        return STRATEGY.get(deviceType);
    }

    public static void registerDeviceCreateStrategy(DeviceType deviceType, DeviceCreateStrategy<?,?> strategy) {
        STRATEGY.put(deviceType,strategy);
    }

}
