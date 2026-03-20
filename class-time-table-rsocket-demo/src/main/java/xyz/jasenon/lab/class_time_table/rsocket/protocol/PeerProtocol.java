package xyz.jasenon.lab.class_time_table.rsocket.protocol;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.jasenon.lab.class_time_table.rsocket.model.*;

import java.util.List;

/**
 * RSocket 对等协议接口
 * 
 * 注意：在 P2P 架构中，没有严格的服务端/客户端之分
 * 班牌设备和管理端都可以实现此接口
 */
public interface PeerProtocol {
    
    // ==================== 设备管理 ====================
    
    /**
     * 设备注册/上线通知
     * 班牌 → 管理端：通知上线
     * 管理端 → 班牌：确认并下发配置
     */
    Mono<RpcMessage<DeviceStatus>> register(DeviceStatus status);
    
    /**
     * 心跳上报（Fire-and-Forget）
     * 班牌 → 管理端：定时上报状态
     */
    Mono<Void> heartbeat(DeviceStatus status);
    
    /**
     * 获取设备状态
     * 管理端 → 班牌：查询设备当前状态
     */
    Mono<RpcMessage<DeviceStatus>> getDeviceStatus(String uuid);
    
    // ==================== 课表相关 ====================
    
    /**
     * 获取当前课表
     * 班牌 → 管理端：查询当前/下一节课
     */
    Mono<RpcMessage<TimeTable>> getCurrentTimeTable(String deviceUuid);
    
    /**
     * 获取课表列表
     */
    Mono<RpcMessage<List<TimeTable>>> getTimeTableList(String deviceUuid, String date);
    
    /**
     * 订阅课表更新（流式）
     * 管理端 → 班牌：有更新时主动推送
     */
    Flux<RpcMessage<TimeTable>> subscribeTimeTableUpdates(String deviceUuid);
    
    // ==================== 人脸识别 ====================
    
    /**
     * 下发人脸数据
     * 管理端 → 班牌：注册/更新人脸
     */
    Mono<RpcMessage<String>> pushFaceData(FaceData faceData);
    
    /**
     * 删除人脸
     */
    Mono<RpcMessage<Boolean>> removeFace(String faceId);
    
    /**
     * 获取设备人脸列表
     */
    Mono<RpcMessage<List<FaceData>>> getDeviceFaceList(String deviceUuid);
    
    /**
     * 上报访问记录（Fire-and-Forget）
     * 班牌 → 管理端：有人刷卡/刷脸时上报
     */
    Mono<Void> reportAccess(AccessRecord record);
    
    /**
     * 批量下发人脸（流式上传）
     * 管理端 → 班牌：批量同步人脸库
     */
    Flux<RpcMessage<String>> batchPushFaces(Flux<FaceData> faces);
    
    // ==================== 配置管理 ====================
    
    /**
     * 获取设备配置
     */
    Mono<RpcMessage<String>> getDeviceConfig(String deviceUuid);
    
    /**
     * 更新设备配置
     * 管理端 → 班牌：远程修改配置
     */
    Mono<RpcMessage<Boolean>> updateDeviceConfig(String deviceUuid, String configJson);
    
    /**
     * 远程重启设备
     */
    Mono<RpcMessage<Boolean>> rebootDevice(String deviceUuid);
    
    /**
     * 远程开门（应急）
     */
    Mono<RpcMessage<Boolean>> remoteOpenDoor(String deviceUuid, String operator);
    
    // ==================== 双向流（实时监控） ====================
    
    /**
     * 实时通信通道（双向流）
     * 用于实时控制、日志传输等场景
     */
    Flux<RpcMessage<String>> duplexChannel(Flux<RpcMessage<String>> input);
}
