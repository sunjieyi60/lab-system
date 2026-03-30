//package xyz.jasenon.rsocket.server.controller;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//import reactor.core.publisher.Mono;
//import xyz.jasenon.rsocket.core.Const;
//import xyz.jasenon.rsocket.core.packet.UpdateConfigRequest;
//import xyz.jasenon.rsocket.core.packet.UpdateConfigResponse;
//import xyz.jasenon.rsocket.core.rsocket.ConnectionManager;
//import xyz.jasenon.rsocket.core.model.ClassTimeTable;
//import xyz.jasenon.rsocket.core.model.Config;
//import xyz.jasenon.rsocket.core.rsocket.Server;
//import xyz.jasenon.rsocket.server.service.ClassTimeTableService;
//
//import java.time.Instant;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//
///**
// * 设备管理 HTTP 控制器
// *
// * 提供具体业务的 REST API：
// * - 开门
// * - 更新配置
// * - 重启设备
// * - 远程截图
// * - 更新课表
// * - 更新人脸库
// *
// * 使用 uuid 作为设备唯一标识
// */
//@Slf4j
//@RestController
//@RequestMapping("/api/device")
//public class DeviceManageController {
//
//    @Autowired
//    private Server server;
//
//    @Autowired
//    private ConnectionManager connectionManager;
//
//    @Autowired
//    private ClassTimeTableService deviceService;
//
//    // ==================== 设备状态查询 ====================
//
//    /**
//     * 获取在线设备数量
//     */
//    @GetMapping("/online/count")
//    public Mono<Map<String, Integer>> getOnlineCount() {
//        return Mono.just(Map.of("count", connectionManager.getOnlineCount()));
//    }
//
//    /**
//     * 获取所有在线设备
//     */
//    @GetMapping("/online")
//    public Mono<Map<String, Object>> getOnlineDevices() {
//        return Mono.just(Map.of(
//                "count", connectionManager.getOnlineCount(),
//                "devices", connectionManager.getAllConnections().keySet()
//        ));
//    }
//
//    /**
//     * 检查设备是否在线
//     */
//    @GetMapping("/online/{uuid}")
//    public Mono<Map<String, Object>> isOnline(@PathVariable String uuid) {
//        boolean online = connectionManager.isOnline(uuid);
//        return deviceService.getByUuid(uuid)
//                .map(device -> Map.<String, Object>of(
//                        "uuid", uuid,
//                        Const.Status.ONLINE, online,
//                        "laboratoryId", device.getLaboratoryId()
//                ))
//                .defaultIfEmpty(Map.of(
//                        "uuid", uuid,
//                        Const.Status.ONLINE, false,
//                        "error", "设备不存在"
//                ));
//    }
//
//    /**
//     * 查询所有在线设备详细信息
//     */
//    @GetMapping("/online/details")
//    public Mono<List<ClassTimeTable>> getOnlineDeviceDetails() {
//        return deviceService.getOnlineDevices();
//    }
//
//    // ==================== 配置管理 ====================
//
//    /**
//     * 更新设备配置
//     */
//    @PostMapping("/{uuid}/config")
//    public Mono<Map<String, Object>> updateConfig(
//            @PathVariable String uuid,
//            @RequestBody Config config) {
//
//        return deviceService.updateConfig(uuid, config)
//                .flatMap(success -> {
//                    if (success && connectionManager.isOnline(uuid)) {
//                        UpdateConfigRequest request = new UpdateConfigRequest();
//                        request.setConfig(config);
//                        request.setImmediate(true);
//                        request.setRequestTime(System.currentTimeMillis());
//
//                        return server.sendTo(uuid, request)
//                                .map(response -> {
//                                    // response 是 UpdateConfigResponse（继承自 Message）
//                                    boolean pushed = response instanceof UpdateConfigResponse
//                                            && ((UpdateConfigResponse) response).isSuccess();
//                                    return Map.<String, Object>of(
//                                           "success", true,
//                                           "uuid", uuid,
//                                           "configUpdated", true,
//                                           "configPushed", pushed
//                                    );
//                                });
////                        return server.requestResponse(uuid, wrapMessage(request), "device.config.update")
////                                .map(response -> {
////                                    Object payload = response.getPayload();
////                                    if (payload instanceof UpdateConfigResponse resp) {
////                                        return Map.<String, Object>of(
////                                                "success", true,
////                                                "uuid", uuid,
////                                                "configUpdated", true,
////                                                "configPushed", resp.isSuccess(),
////                                                "currentVersion", resp.getCurrentVersion()
////                                        );
////                                    }
////                                    return Map.<String, Object>of(
////                                            "success", true,
////                                            "uuid", uuid,
////                                            "configUpdated", true,
////                                            "message", "配置已更新"
////                                    );
////                                });
//                    }
//                    return Mono.just(Map.<String, Object>of(
//                            "success", success,
//                            "uuid", uuid,
//                            "configUpdated", success,
//                            "message", success ? "配置已更新" : "配置更新失败"
//                    ));
//                });
//    }
//
//    /**
//     * 获取设备配置
//     */
//    @GetMapping("/{uuid}/config")
//    public Mono<Map<String, Object>> getConfig(@PathVariable String uuid) {
//        return deviceService.getByUuid(uuid)
//                .map(device -> Map.<String, Object>of(
//                        "success", true,
//                        "uuid", uuid,
//                        "config", device.getConfig() != null ? device.getConfig() : Config.Default(),
//                        Const.Status.ONLINE, connectionManager.isOnline(uuid)
//                ))
//                .defaultIfEmpty(Map.of(
//                        "success", false,
//                        "uuid", uuid,
//                        "error", "设备不存在"
//                ));
//    }
//
//}
