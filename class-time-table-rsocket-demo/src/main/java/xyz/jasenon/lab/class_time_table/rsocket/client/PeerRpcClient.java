package xyz.jasenon.lab.class_time_table.rsocket.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import io.rsocket.RSocket;
import io.rsocket.util.DefaultPayload;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.jasenon.lab.class_time_table.rsocket.model.*;
import xyz.jasenon.lab.class_time_table.rsocket.protocol.Command;

import java.time.Duration;
import java.util.List;

/**
 * Peer RPC 客户端封装
 * 提供类型安全的远程调用方法
 */
@Slf4j
public class PeerRpcClient {
    
    private final RSocket socket;
    private final String localUuid;
    
    public PeerRpcClient(RSocket socket, String localUuid) {
        this.socket = socket;
        this.localUuid = localUuid;
    }
    
    // ==================== 设备管理 ====================
    
    /**
     * 注册设备
     */
    public Mono<RpcMessage<DeviceStatus>> register(DeviceStatus status) {
        return requestResponse(Command.REGISTER, status, new TypeReference<RpcMessage<DeviceStatus>>() {});
    }
    
    /**
     * 发送心跳
     */
    public Mono<Void> heartbeat(DeviceStatus status) {
        return fireAndForget(Command.HEARTBEAT, status);
    }
    
    /**
     * 获取设备状态
     */
    public Mono<RpcMessage<DeviceStatus>> getDeviceStatus(String uuid) {
        return requestResponse(Command.GET_DEVICE_STATUS, uuid, new TypeReference<RpcMessage<DeviceStatus>>() {});
    }
    
    // ==================== 课表相关 ====================
    
    /**
     * 获取当前课表
     */
    public Mono<RpcMessage<TimeTable>> getCurrentTimeTable(String deviceUuid) {
        return requestResponse(Command.GET_CURRENT_TIME_TABLE, deviceUuid, new TypeReference<RpcMessage<TimeTable>>() {});
    }
    
    /**
     * 获取课表列表
     */
    public Mono<RpcMessage<List<TimeTable>>> getTimeTableList(String deviceUuid, String date) {
        TimeTableRequest req = new TimeTableRequest(deviceUuid, date);
        return requestResponse(Command.GET_TIME_TABLE_LIST, req, new TypeReference<RpcMessage<List<TimeTable>>>() {});
    }
    
    /**
     * 订阅课表更新
     */
    public Flux<RpcMessage<TimeTable>> subscribeTimeTableUpdates(String deviceUuid) {
        RpcMessage<String> request = RpcMessage.<String>builder()
                .command(Command.SUBSCRIBE_TIME_TABLE_UPDATES)
                .payload(deviceUuid)
                .build();
        
        return socket.requestStream(DefaultPayload.create(JSON.toJSONString(request)))
                .map(payload -> {
                    String data = payload.getDataUtf8();
                    return JSON.parseObject(data, new TypeReference<RpcMessage<TimeTable>>() {});
                });
    }
    
    // ==================== 人脸识别 ====================
    
    /**
     * 下发人脸数据
     */
    public Mono<RpcMessage<String>> pushFaceData(FaceData faceData) {
        return requestResponse(Command.PUSH_FACE_DATA, faceData, new TypeReference<RpcMessage<String>>() {});
    }
    
    /**
     * 删除人脸
     */
    public Mono<RpcMessage<Boolean>> removeFace(String faceId) {
        return requestResponse(Command.REMOVE_FACE, faceId, new TypeReference<RpcMessage<Boolean>>() {});
    }
    
    /**
     * 获取人脸列表
     */
    public Mono<RpcMessage<List<FaceData>>> getDeviceFaceList(String deviceUuid) {
        return requestResponse(Command.GET_DEVICE_FACE_LIST, deviceUuid, new TypeReference<RpcMessage<List<FaceData>>>() {});
    }
    
    /**
     * 上报访问记录
     */
    public Mono<Void> reportAccess(AccessRecord record) {
        return fireAndForget(Command.REPORT_ACCESS, record);
    }
    
    // ==================== 配置管理 ====================
    
    /**
     * 获取设备配置
     */
    public Mono<RpcMessage<String>> getDeviceConfig(String deviceUuid) {
        return requestResponse(Command.GET_DEVICE_CONFIG, deviceUuid, new TypeReference<RpcMessage<String>>() {});
    }
    
    /**
     * 更新设备配置
     */
    public Mono<RpcMessage<Boolean>> updateDeviceConfig(String deviceUuid, String configJson) {
        ConfigUpdateRequest req = new ConfigUpdateRequest(deviceUuid, configJson);
        return requestResponse(Command.UPDATE_DEVICE_CONFIG, req, new TypeReference<RpcMessage<Boolean>>() {});
    }
    
    /**
     * 远程重启设备
     */
    public Mono<RpcMessage<Boolean>> rebootDevice(String deviceUuid) {
        return requestResponse(Command.REBOOT_DEVICE, deviceUuid, new TypeReference<RpcMessage<Boolean>>() {});
    }
    
    /**
     * 远程开门
     */
    public Mono<RpcMessage<Boolean>> remoteOpenDoor(String deviceUuid, String operator) {
        OpenDoorRequest req = new OpenDoorRequest(deviceUuid, operator);
        return requestResponse(Command.REMOTE_OPEN_DOOR, req, new TypeReference<RpcMessage<Boolean>>() {});
    }
    
    // ==================== 内部工具方法 ====================
    
    private <T> Mono<RpcMessage<T>> requestResponse(Command command, Object payload, TypeReference<RpcMessage<T>> typeRef) {
        RpcMessage<Object> request = RpcMessage.builder()
                .command(command)
                .payload(payload)
                .fromUuid(localUuid)
                .build();
        
        return socket.requestResponse(DefaultPayload.create(JSON.toJSONString(request)))
                .map(responsePayload -> {
                    String data = responsePayload.getDataUtf8();
                    return JSON.parseObject(data, typeRef);
                })
                .timeout(Duration.ofSeconds(10))
                .doOnError(e -> log.error("[{}] 调用 {} 失败: {}", localUuid, command, e.getMessage()));
    }
    
    private Mono<Void> fireAndForget(Command command, Object payload) {
        RpcMessage<Object> request = RpcMessage.builder()
                .command(command)
                .payload(payload)
                .fromUuid(localUuid)
                .build();
        
        return socket.fireAndForget(DefaultPayload.create(JSON.toJSONString(request)))
                .timeout(Duration.ofSeconds(5))
                .doOnError(e -> log.error("[{}] 发送 {} 失败: {}", localUuid, command, e.getMessage()))
                .onErrorResume(e -> Mono.empty()); // Fire-and-forget 不抛异常
    }
    
    // ==================== 内部请求对象 ====================
    
    private record TimeTableRequest(String deviceUuid, String date) {}
    private record ConfigUpdateRequest(String deviceUuid, String configJson) {}
    private record OpenDoorRequest(String deviceUuid, String operator) {}
}
