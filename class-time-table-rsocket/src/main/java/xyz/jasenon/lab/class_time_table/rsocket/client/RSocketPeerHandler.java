package xyz.jasenon.lab.class_time_table.rsocket.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.DefaultPayload;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.jasenon.lab.class_time_table.rsocket.model.*;
import xyz.jasenon.lab.class_time_table.rsocket.protocol.Command;
import xyz.jasenon.lab.class_time_table.rsocket.protocol.PeerProtocol;

import java.time.Duration;

/**
 * RSocket Peer Handler
 * 处理来自远程对等节点的请求
 */
@Slf4j
public class RSocketPeerHandler implements RSocket {
    
    private final PeerProtocol protocol;
    private final String localUuid;
    
    public RSocketPeerHandler(PeerProtocol protocol, String localUuid) {
        this.protocol = protocol;
        this.localUuid = localUuid;
    }
    
    @Override
    public Mono<Payload> requestResponse(Payload payload) {
        return Mono.fromCallable(() -> {
            String data = payload.getDataUtf8();
            RpcMessage<?> request = JSON.parseObject(data, new TypeReference<RpcMessage<?>>() {});
            
            // 将字符串 command 转换为枚举
            Command cmd = request.getCommand() != null 
                    ? request.getCommand() 
                    : Command.fromCode(JSON.parseObject(data).getString("command"));
            
            log.debug("[{}] 收到请求: {} - {}", localUuid, cmd, request.getMessageId());
            
            RpcMessage<?> response = handleRequest(request, cmd);
            return DefaultPayload.create(JSON.toJSONString(response));
        }).onErrorResume(e -> {
            log.error("[{}] 处理请求失败: {}", localUuid, e.getMessage());
            RpcMessage<Object> error = RpcMessage.error(Command.UNKNOWN, e.getMessage());
            return Mono.just(DefaultPayload.create(JSON.toJSONString(error)));
        });
    }
    
    @Override
    public Mono<Void> fireAndForget(Payload payload) {
        return Mono.fromRunnable(() -> {
            String data = payload.getDataUtf8();
            RpcMessage<?> request = JSON.parseObject(data, new TypeReference<RpcMessage<?>>() {});
            
            Command cmd = request.getCommand() != null 
                    ? request.getCommand() 
                    : Command.fromCode(JSON.parseObject(data).getString("command"));
            
            log.debug("[{}] 收到单向消息: {}", localUuid, cmd);
            
            // 异步处理，不返回响应
            handleFireAndForget(request, cmd)
                    .subscribe(
                            null,
                            e -> log.error("[{}] 处理单向消息失败: {}", localUuid, e.getMessage())
                    );
        });
    }
    
    @Override
    public Flux<Payload> requestStream(Payload payload) {
        String data = payload.getDataUtf8();
        RpcMessage<?> request = JSON.parseObject(data, new TypeReference<RpcMessage<?>>() {});
        
        Command cmd = request.getCommand() != null 
                ? request.getCommand() 
                : Command.fromCode(JSON.parseObject(data).getString("command"));
        
        log.debug("[{}] 收到流式请求: {}", localUuid, cmd);
        
        return handleStream(request, cmd)
                .map(msg -> DefaultPayload.create(JSON.toJSONString(msg)));
    }
    
    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
        // 双向流处理
        Flux<RpcMessage<?>> inputFlux = Flux.from(payloads)
                .map(payload -> {
                    String data = payload.getDataUtf8();
                    return JSON.parseObject(data, new TypeReference<RpcMessage<?>>() {});
                });
        
        return handleChannel(inputFlux)
                .map(msg -> DefaultPayload.create(JSON.toJSONString(msg)));
    }
    
    /**
     * 处理 Request-Response 请求
     */
    @SuppressWarnings("unchecked")
    private RpcMessage<?> handleRequest(RpcMessage<?> request, Command cmd) {
        Object payload = request.getPayload();
        
        try {
            switch (cmd) {
                // 设备管理
                case REGISTER:
                    DeviceStatus status = JSON.parseObject(JSON.toJSONString(payload), DeviceStatus.class);
                    return protocol.register(status).block(Duration.ofSeconds(5));
                    
                case GET_DEVICE_STATUS:
                    String uuid = (String) payload;
                    return protocol.getDeviceStatus(uuid).block(Duration.ofSeconds(5));
                    
                // 课表相关
                case GET_CURRENT_TIME_TABLE:
                    String deviceUuid = (String) payload;
                    return protocol.getCurrentTimeTable(deviceUuid).block(Duration.ofSeconds(5));
                    
                case GET_TIME_TABLE_LIST:
                    // TODO: 解析 payload 中的 deviceUuid 和 date
                    return protocol.getTimeTableList(deviceUuid, null).block(Duration.ofSeconds(5));
                    
                // 人脸相关
                case PUSH_FACE_DATA:
                    FaceData faceData = JSON.parseObject(JSON.toJSONString(payload), FaceData.class);
                    return protocol.pushFaceData(faceData).block(Duration.ofSeconds(10));
                    
                case REMOVE_FACE:
                    String faceId = (String) payload;
                    return protocol.removeFace(faceId).block(Duration.ofSeconds(5));
                    
                case GET_DEVICE_FACE_LIST:
                    String devUuid = (String) payload;
                    return protocol.getDeviceFaceList(devUuid).block(Duration.ofSeconds(5));
                    
                // 配置管理
                case GET_DEVICE_CONFIG:
                    String configUuid = (String) payload;
                    return protocol.getDeviceConfig(configUuid).block(Duration.ofSeconds(5));
                    
                case UPDATE_DEVICE_CONFIG:
                    // TODO: 解析 deviceUuid 和 configJson
                    return protocol.updateDeviceConfig(configUuid, JSON.toJSONString(payload)).block(Duration.ofSeconds(5));
                    
                case REBOOT_DEVICE:
                    String rebootUuid = (String) payload;
                    return protocol.rebootDevice(rebootUuid).block(Duration.ofSeconds(5));
                    
                case REMOTE_OPEN_DOOR:
                    // TODO: 解析 deviceUuid 和 operator
                    return protocol.remoteOpenDoor(rebootUuid, "admin").block(Duration.ofSeconds(5));
                    
                default:
                    return RpcMessage.error(cmd, StatusCode.BAD_REQUEST, "不支持的命令: " + cmd);
            }
        } catch (Exception e) {
            log.error("[{}] 处理命令 {} 失败: {}", localUuid, cmd, e.getMessage());
            return RpcMessage.error(cmd, StatusCode.INTERNAL_ERROR, e.getMessage());
        }
    }
    
    /**
     * 处理 Fire-and-Forget 请求
     */
    private Mono<Void> handleFireAndForget(RpcMessage<?> request, Command cmd) {
        Object payload = request.getPayload();
        
        switch (cmd) {
            case HEARTBEAT:
                DeviceStatus status = JSON.parseObject(JSON.toJSONString(payload), DeviceStatus.class);
                return protocol.heartbeat(status);
                
            case REPORT_ACCESS:
                AccessRecord record = JSON.parseObject(JSON.toJSONString(payload), AccessRecord.class);
                return protocol.reportAccess(record);
                
            default:
                log.warn("[{}] 未知的单向消息命令: {}", localUuid, cmd);
                return Mono.empty();
        }
    }
    
    /**
     * 处理 Stream 请求
     */
    private Flux<RpcMessage<?>> handleStream(RpcMessage<?> request, Command cmd) {
        switch (cmd) {
            case SUBSCRIBE_TIME_TABLE_UPDATES:
                String deviceUuid = (String) request.getPayload();
                return protocol.subscribeTimeTableUpdates(deviceUuid);
                
            default:
                return Flux.error(new IllegalArgumentException("未知流式命令: " + cmd));
        }
    }
    
    /**
     * 处理 Channel 请求（双向流）
     */
    private Flux<RpcMessage<String>> handleChannel(Flux<RpcMessage<?>> input) {
        // 将 RpcMessage<?> 转换为 RpcMessage<String>
        Flux<RpcMessage<String>> stringInput = input.map(msg -> {
            @SuppressWarnings("unchecked")
            RpcMessage<String> typed = (RpcMessage<String>) msg;
            return typed;
        });
        
        return protocol.duplexChannel(stringInput);
    }
}
