package xyz.jasenon.rsocket.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.rsocket.ConnectionManager;
import xyz.jasenon.rsocket.core.model.ClassTimeTable;
import xyz.jasenon.rsocket.core.model.Config;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.server.service.ClassTimeTableService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 设备管理 HTTP 控制器
 * 
 * 提供具体业务的 REST API：
 * - 开门
 * - 更新配置
 * - 重启设备
 * - 远程截图
 * - 更新课表
 * - 更新人脸库
 */
@Slf4j
@RestController
@RequestMapping("/api/device")
public class DeviceManageController {

    @Autowired
    private ConnectionManager connectionManager;

    @Autowired
    private ClassTimeTableService deviceService;

    // ==================== 设备状态查询 ====================

    /**
     * 获取在线设备数量
     */
    @GetMapping("/online/count")
    public Mono<Map<String, Integer>> getOnlineCount() {
        return Mono.just(Map.of("count", connectionManager.getOnlineCount()));
    }

    /**
     * 获取所有在线设备
     */
    @GetMapping("/online")
    public Mono<Map<String, Object>> getOnlineDevices() {
        return Mono.just(Map.of(
                "count", connectionManager.getOnlineCount(),
                "devices", connectionManager.getAllConnections().keySet()
        ));
    }

    /**
     * 检查设备是否在线
     */
    @GetMapping("/online/{deviceDbId}")
    public Mono<Map<String, Object>> isOnline(@PathVariable Long deviceDbId) {
        boolean online = connectionManager.isOnline(deviceDbId);
        return deviceService.getById(deviceDbId)
                .map(device -> Map.<String, Object>of(
                        "deviceDbId", deviceDbId,
                        Const.Status.ONLINE, online,
                        "uuid", device.getUuid(),
                        "laboratoryId", device.getLaboratoryId()
                ))
                .defaultIfEmpty(Map.of(
                        "deviceDbId", deviceDbId,
                        Const.Status.ONLINE, false,
                        "error", "设备不存在"
                ));
    }

    /**
     * 查询所有在线设备详细信息
     */
    @GetMapping("/online/details")
    public Mono<List<ClassTimeTable>> getOnlineDeviceDetails() {
        return deviceService.getOnlineDevices();
    }

    // ==================== 开门控制 ====================

    /**
     * 远程开门
     */
    @PostMapping("/{deviceDbId}/door/open")
    public Mono<Map<String, Object>> openDoor(
            @PathVariable Long deviceDbId,
            @RequestBody OpenDoorRequest request) {
        
        request.setRequestTime(Instant.now());
        
        return connectionManager.requestResponse(deviceDbId, wrapMessage(request), "device.door.open")
                .map(response -> extractResponse(response, deviceDbId, "开门"));
    }

    // ==================== 配置管理 ====================

    /**
     * 更新设备配置
     */
    @PostMapping("/{deviceDbId}/config")
    public Mono<Map<String, Object>> updateConfig(
            @PathVariable Long deviceDbId,
            @RequestBody Config config) {
        
        return deviceService.updateConfig(deviceDbId, config)
                .flatMap(success -> {
                    if (success && connectionManager.isOnline(deviceDbId)) {
                        UpdateConfigRequest request = new UpdateConfigRequest();
                        request.setConfig(config);
                        request.setImmediate(true);
                        request.setRequestTime(Instant.now());
                        
                        return connectionManager.requestResponse(deviceDbId, wrapMessage(request), "device.config.update")
                                .map(response -> {
                                    Object payload = response.getPayload();
                                    if (payload instanceof UpdateConfigResponse resp) {
                                        return Map.<String, Object>of(
                                                "success", true,
                                                "deviceDbId", deviceDbId,
                                                "configUpdated", true,
                                                "configPushed", resp.isSuccess(),
                                                "currentVersion", resp.getCurrentVersion()
                                        );
                                    }
                                    return Map.<String, Object>of(
                                            "success", true,
                                            "deviceDbId", deviceDbId,
                                            "configUpdated", true,
                                            "message", "配置已更新"
                                    );
                                });
                    }
                    return Mono.just(Map.<String, Object>of(
                            "success", success,
                            "deviceDbId", deviceDbId,
                            "configUpdated", success,
                            "message", success ? "配置已更新" : "配置更新失败"
                    ));
                });
    }

    /**
     * 获取设备配置
     */
    @GetMapping("/{deviceDbId}/config")
    public Mono<Map<String, Object>> getConfig(@PathVariable Long deviceDbId) {
        return deviceService.getById(deviceDbId)
                .map(device -> Map.<String, Object>of(
                        "success", true,
                        "deviceDbId", deviceDbId,
                        "config", device.getConfig() != null ? device.getConfig() : Config.Default(),
                        Const.Status.ONLINE, connectionManager.isOnline(deviceDbId)
                ))
                .defaultIfEmpty(Map.of(
                        "success", false,
                        "deviceDbId", deviceDbId,
                        "error", "设备不存在"
                ));
    }

    // ==================== 设备控制 ====================

    /**
     * 重启设备
     */
    @PostMapping("/{deviceDbId}/reboot")
    public Mono<Map<String, Object>> reboot(
            @PathVariable Long deviceDbId,
            @RequestBody RebootRequest request) {
        
        request.setRequestTime(Instant.now());
        
        return connectionManager.requestResponse(deviceDbId, wrapMessage(request), "device.reboot")
                .map(response -> extractResponse(response, deviceDbId, "重启"));
    }

    /**
     * 远程截图
     */
    @PostMapping("/{deviceDbId}/screenshot")
    public Mono<Map<String, Object>> screenshot(
            @PathVariable Long deviceDbId,
            @RequestBody ScreenshotRequest request) {
        
        request.setRequestTime(Instant.now());
        
        return connectionManager.requestResponse(deviceDbId, wrapMessage(request), "device.screenshot")
                .map(response -> {
                    Object payload = response.getPayload();
                    Map<String, Object> result = new java.util.HashMap<>();
                    result.put("deviceDbId", deviceDbId);
                    result.put("operation", "截图");
                    
                    if (payload instanceof ScreenshotResponse resp) {
                        result.put("success", resp.isSuccess());
                        result.put("imageData", resp.getImageData());
                        result.put("format", resp.getFormat());
                        result.put("captureTime", resp.getCaptureTime());
                    } else {
                        result.putAll(extractResponse(response, deviceDbId, "截图"));
                    }
                    return result;
                });
    }

    // ==================== 数据同步 ====================

    /**
     * 更新课表
     */
    @PostMapping("/{deviceDbId}/schedule")
    public Mono<Map<String, Object>> updateSchedule(
            @PathVariable Long deviceDbId,
            @RequestBody UpdateScheduleRequest request) {
        
        request.setRequestTime(Instant.now());
        
        return connectionManager.requestResponse(deviceDbId, wrapMessage(request), "device.schedule.update")
                .map(response -> extractResponse(response, deviceDbId, "课表更新"));
    }

    /**
     * 更新人脸库
     */
    @PostMapping("/{deviceDbId}/face-library")
    public Mono<Map<String, Object>> updateFaceLibrary(
            @PathVariable Long deviceDbId,
            @RequestBody UpdateFaceLibraryRequest request) {
        
        request.setRequestTime(Instant.now());
        
        return connectionManager.requestResponse(deviceDbId, wrapMessage(request), "device.face-library.update")
                .map(response -> extractResponse(response, deviceDbId, "人脸库更新"));
    }

    // ==================== 广播接口 ====================

    /**
     * 向实验室广播开门指令
     */
    @PostMapping("/lab/{laboratoryId}/door/open")
    public Mono<Map<String, Object>> broadcastOpenDoor(
            @PathVariable Long laboratoryId,
            @RequestBody OpenDoorRequest request) {
        
        request.setRequestTime(Instant.now());
        
        return connectionManager.broadcastToLaboratory(laboratoryId, request, "device.door.open",
                        labId -> deviceService.getDeviceIdsByLaboratory(labId))
                .map(count -> Map.<String, Object>of(
                        "success", count > 0,
                        "laboratoryId", laboratoryId,
                        "sentCount", count,
                        "message", "开门指令已发送给 " + count + " 个在线设备"
                ));
    }

    /**
     * 向实验室广播更新课表
     */
    @PostMapping("/lab/{laboratoryId}/schedule")
    public Mono<Map<String, Object>> broadcastUpdateSchedule(
            @PathVariable Long laboratoryId,
            @RequestBody UpdateScheduleRequest request) {
        
        request.setRequestTime(Instant.now());
        
        return connectionManager.broadcastToLaboratory(laboratoryId, request, "device.schedule.update",
                        labId -> deviceService.getDeviceIdsByLaboratory(labId))
                .map(count -> Map.<String, Object>of(
                        "success", count > 0,
                        "laboratoryId", laboratoryId,
                        "sentCount", count,
                        "message", "课表更新已发送给 " + count + " 个在线设备"
                ));
    }

    // ==================== 辅助方法 ====================

    /**
     * 将 DTO 包装为 Message<?>
     */
    private Message<?> wrapMessage(Object payload) {
        Message<Object> message = new Message<>();
        message.setType(Message.Type.REQUEST_RESPONSE);
        message.setPayload(payload);
        message.setTimestamp(Instant.now());
        return message;
    }

    /**
     * 从 Message<?> 中提取响应数据
     */
    private Map<String, Object> extractResponse(Message<?> message, Long deviceDbId, String operation) {
        Object payload = message.getPayload();
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("deviceDbId", deviceDbId);
        result.put("operation", operation);
        result.put("timestamp", message.getTimestamp());
        
        if (payload instanceof Map<?, ?> respMap) {
            respMap.forEach((k, v) -> result.put(k.toString(), v));
        } else {
            result.put("payload", payload);
        }
        
        return result;
    }
}
