package xyz.jasenon.lab.service.strategy.device.record;

import xyz.jasenon.lab.common.entity.device.DeviceType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
public class DeviceRecordFactory {

    private final static Map<DeviceType, DeviceRecordQ> STRATEGY = new ConcurrentHashMap<>();

    public static DeviceRecordQ getDeviceRecordMethod(DeviceType deviceType) {
        return STRATEGY.get(deviceType);
    }

    public static void registerDeviceRecordMethod(DeviceType deviceType, DeviceRecordQ<?,?> strategy) {
        STRATEGY.put(deviceType,strategy);
    }

}
