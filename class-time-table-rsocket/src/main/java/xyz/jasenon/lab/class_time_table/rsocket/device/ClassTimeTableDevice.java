package xyz.jasenon.lab.class_time_table.rsocket.device;

import io.rsocket.RSocket;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.jasenon.lab.class_time_table.rsocket.client.PeerRpcClient;
import xyz.jasenon.lab.class_time_table.rsocket.client.RSocketPeerConnector;
import xyz.jasenon.lab.class_time_table.rsocket.model.*;
import xyz.jasenon.lab.class_time_table.rsocket.protocol.Command;
import xyz.jasenon.lab.class_time_table.rsocket.protocol.PeerProtocol;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 班牌设备端实现
 * 支持作为客户端连接管理端，或作为服务端被管理端直连
 */
@Slf4j
public class ClassTimeTableDevice implements PeerProtocol {
    
    @Getter
    private final String uuid;
    private final String deviceName;
    private final String roomId;
    
    private final RSocketPeerConnector connector;
    private PeerRpcClient rpcClient;
    private Disposable heartbeatDisposable;
    
    // 本地状态
    private final AtomicReference<DeviceStatus> currentStatus = new AtomicReference<>();
    private final List<TimeTable> localTimeTables = new CopyOnWriteArrayList<>();
    private final List<FaceData> localFaces = new CopyOnWriteArrayList<>();
    
    // 配置
    private String deviceConfig = "{}";
    
    public ClassTimeTableDevice(String uuid, String deviceName, String roomId) {
        this.uuid = uuid;
        this.deviceName = deviceName;
        this.roomId = roomId;
        this.connector = new RSocketPeerConnector();
        
        // 初始化状态
        currentStatus.set(DeviceStatus.builder()
                .uuid(uuid)
                .deviceName(deviceName)
                .roomId(roomId)
                .status(DeviceStatus.Status.OFFLINE)
                .terminal("android")
                .reportTime(Instant.now())
                .build());
    }
    
    /**
     * 作为客户端连接到管理端
     */
    public Mono<Void> connectToManager(String managerHost, int managerPort) {
        return connector.connectAsClient(managerHost, managerPort, uuid, this)
                .flatMap(socket -> {
                    this.rpcClient = new PeerRpcClient(socket, uuid);
                    
                    // 上报在线状态
                    DeviceStatus onlineStatus = DeviceStatus.builder()
                            .uuid(uuid)
                            .deviceName(deviceName)
                            .status(DeviceStatus.Status.ONLINE)
                            .roomId(roomId)
                            .terminal("android")
                            .ipAddress(getLocalIp())
                            .reportTime(Instant.now())
                            .build();
                    
                    currentStatus.set(onlineStatus);
                    
                    return rpcClient.register(onlineStatus)
                            .doOnSuccess(resp -> {
                                if (resp.isSuccess()) {
                                    log.info("[{}] 注册成功: {}", uuid, resp.getPayload());
                                    startHeartbeat();
                                } else {
                                    log.error("[{}] 注册失败: {}", uuid, resp.getErrorMsg());
                                }
                            })
                            .then();
                });
    }
    
    /**
     * 作为服务端启动（允许管理端直连调试）
     */
    public Mono<Void> startAsServer(int port) {
        return connector.listenAsServer(port, uuid, this)
                .then();
    }
    
    /**
     * 启动心跳定时器
     */
    private void startHeartbeat() {
        heartbeatDisposable = Flux.interval(Duration.ofSeconds(30))
                .flatMap(tick -> {
                    DeviceStatus status = currentStatus.get();
                    status.setReportTime(Instant.now());
                    return rpcClient.heartbeat(status)
                            .doOnError(e -> log.warn("[{}] 心跳发送失败: {}", uuid, e.getMessage()))
                            .onErrorResume(e -> Mono.empty());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
        
        log.info("[{}] 心跳定时器已启动", uuid);
    }
    
    /**
     * 上报人脸识别记录
     */
    public Mono<Void> reportFaceAccess(String faceId, String personName, 
                                        AccessRecord.Result result, Integer matchScore) {
        if (rpcClient == null) {
            log.warn("[{}] 尚未连接到管理端，无法上报", uuid);
            return Mono.empty();
        }
        
        AccessRecord record = AccessRecord.builder()
                .recordId(generateId())
                .deviceUuid(uuid)
                .faceId(faceId)
                .personName(personName)
                .accessTime(Instant.now())
                .result(result)
                .method(AccessRecord.AccessMethod.FACE)
                .matchScore(matchScore)
                .build();
        
        return rpcClient.reportAccess(record);
    }
    
    /**
     * 获取 RPC 客户端（用于主动调用）
     */
    public PeerRpcClient getRpcClient() {
        return rpcClient;
    }
    
    /**
     * 断开连接
     */
    public Mono<Void> disconnect() {
        if (heartbeatDisposable != null && !heartbeatDisposable.isDisposed()) {
            heartbeatDisposable.dispose();
        }
        
        DeviceStatus offlineStatus = currentStatus.get();
        offlineStatus.setStatus(DeviceStatus.Status.OFFLINE);
        
        return connector.close();
    }
    
    // ==================== PeerProtocol 实现（处理来自管理端的请求） ====================
    
    @Override
    public Mono<RpcMessage<DeviceStatus>> register(DeviceStatus status) {
        // 设备端通常不需要处理其他设备的注册请求
        return Mono.just(RpcMessage.success(Command.REGISTER, currentStatus.get()));
    }
    
    @Override
    public Mono<Void> heartbeat(DeviceStatus status) {
        // 设备端不需要处理心跳请求
        return Mono.empty();
    }
    
    @Override
    public Mono<RpcMessage<DeviceStatus>> getDeviceStatus(String uuid) {
        if (this.uuid.equals(uuid)) {
            return Mono.just(RpcMessage.success(Command.GET_DEVICE_STATUS, currentStatus.get()));
        }
        return Mono.just(RpcMessage.error(Command.GET_DEVICE_STATUS, StatusCode.DEVICE_OFFLINE, "设备不存在"));
    }
    
    @Override
    public Mono<RpcMessage<TimeTable>> getCurrentTimeTable(String deviceUuid) {
        // 返回当前正在上的课
        TimeTable current = localTimeTables.stream()
                .filter(t -> isCurrentCourse(t))
                .findFirst()
                .orElse(null);
        
        if (current != null) {
            return Mono.just(RpcMessage.success(Command.GET_CURRENT_TIME_TABLE, current));
        }
        return Mono.just(RpcMessage.error(Command.GET_CURRENT_TIME_TABLE, StatusCode.TIME_TABLE_EMPTY, "当前无课程"));
    }
    
    @Override
    public Mono<RpcMessage<List<TimeTable>>> getTimeTableList(String deviceUuid, String date) {
        return Mono.just(RpcMessage.success(Command.GET_TIME_TABLE_LIST, localTimeTables));
    }
    
    @Override
    public Flux<RpcMessage<TimeTable>> subscribeTimeTableUpdates(String deviceUuid) {
        // 设备端作为服务端时，管理端可以订阅课表更新
        // 实际实现：当本地课表变更时推送
        return Flux.empty();
    }
    
    @Override
    public Mono<RpcMessage<String>> pushFaceData(FaceData faceData) {
        // 管理端下发人脸数据到设备
        localFaces.removeIf(f -> f.getFaceId().equals(faceData.getFaceId()));
        localFaces.add(faceData);
        
        log.info("[{}] 收到人脸数据: {} - {}", uuid, faceData.getFaceId(), faceData.getFaceName());
        
        // 这里可以触发本地人脸库更新
        updateLocalFaceDatabase();
        
        return Mono.just(RpcMessage.success(Command.PUSH_FACE_DATA, "注册成功"));
    }
    
    @Override
    public Mono<RpcMessage<Boolean>> removeFace(String faceId) {
        boolean removed = localFaces.removeIf(f -> f.getFaceId().equals(faceId));
        if (removed) {
            updateLocalFaceDatabase();
            return Mono.just(RpcMessage.success(Command.REMOVE_FACE, true));
        }
        return Mono.just(RpcMessage.error(Command.REMOVE_FACE, StatusCode.FACE_NOT_FOUND, "人脸不存在"));
    }
    
    @Override
    public Mono<RpcMessage<List<FaceData>>> getDeviceFaceList(String deviceUuid) {
        return Mono.just(RpcMessage.success(Command.GET_DEVICE_FACE_LIST, localFaces));
    }
    
    @Override
    public Mono<Void> reportAccess(AccessRecord record) {
        // 设备端作为服务端时，其他设备上报访问记录（一般不需要）
        return Mono.empty();
    }
    
    @Override
    public Flux<RpcMessage<String>> batchPushFaces(Flux<FaceData> faces) {
        // 流式接收人脸数据
        return faces.map(face -> {
            localFaces.add(face);
            return RpcMessage.success(Command.BATCH_PUSH_FACES, "Received: " + face.getFaceId());
        });
    }
    
    @Override
    public Mono<RpcMessage<String>> getDeviceConfig(String deviceUuid) {
        return Mono.just(RpcMessage.success(Command.GET_DEVICE_CONFIG, deviceConfig));
    }
    
    @Override
    public Mono<RpcMessage<Boolean>> updateDeviceConfig(String deviceUuid, String configJson) {
        this.deviceConfig = configJson;
        log.info("[{}] 配置已更新", uuid);
        applyConfig(configJson);
        return Mono.just(RpcMessage.success(Command.UPDATE_DEVICE_CONFIG, true));
    }
    
    @Override
    public Mono<RpcMessage<Boolean>> rebootDevice(String deviceUuid) {
        log.info("[{}] 收到重启指令", uuid);
        // 实际实现：触发系统重启
        Schedulers.boundedElastic().schedule(() -> {
            try {
                Thread.sleep(1000);
                Runtime.getRuntime().exec("reboot");
            } catch (Exception e) {
                log.error("重启失败", e);
            }
        });
        return Mono.just(RpcMessage.success(Command.REBOOT_DEVICE, true));
    }
    
    @Override
    public Mono<RpcMessage<Boolean>> remoteOpenDoor(String deviceUuid, String operator) {
        log.info("[{}] 收到远程开门指令，操作人: {}", uuid, operator);
        // 实际实现：触发继电器开门
        triggerDoorOpen();
        return Mono.just(RpcMessage.success(Command.REMOTE_OPEN_DOOR, true));
    }
    
    @Override
    public Flux<RpcMessage<String>> duplexChannel(Flux<RpcMessage<String>> input) {
        // 双向流：接收管理端指令，返回设备状态
        return input.map(msg -> {
            log.info("[{}] 收到通道消息: {}", uuid, msg.getPayload());
            return RpcMessage.success(Command.DUPLEX_CHANNEL, "Echo: " + msg.getPayload());
        });
    }
    
    // ==================== 私有工具方法 ====================
    
    private boolean isCurrentCourse(TimeTable table) {
        // 判断是否是当前课程
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return now.isAfter(table.getStartTime()) && now.isBefore(table.getEndTime());
    }
    
    private void updateLocalFaceDatabase() {
        // 更新本地人脸库文件
        log.info("[{}] 本地人脸库已更新，共 {} 条", uuid, localFaces.size());
    }
    
    private void applyConfig(String configJson) {
        // 应用配置到设备
        log.info("[{}] 正在应用新配置", uuid);
    }
    
    private void triggerDoorOpen() {
        // 触发开门动作
        log.info("[{}] 正在开门...", uuid);
    }
    
    private String getLocalIp() {
        // 获取本地 IP
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
    
    private String generateId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}
