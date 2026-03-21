package xyz.jasenon.rsocket.core.rsocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连接管理器
 * 
 * 只负责管理 RSocket 连接的生命周期：
 * - 注册连接
 * - 移除连接
 * - 查询连接
 * 
 * 发送逻辑由 Server/Client 处理
 */
@Slf4j
@Component
public class ConnectionManager {

    /**
     * 设备ID到RSocketRequester的映射
     */
    private final Map<String, RSocketRequester> deviceConnections = new ConcurrentHashMap<>();

    /**
     * 注册设备连接
     *
     * @param deviceId  设备数据库ID
     * @param requester RSocket 连接
     */
    public void register(String deviceId, RSocketRequester requester) {
        if (deviceId == null || requester == null) {
            log.warn("无效的注册参数: deviceId={}, requester={}", deviceId, requester);
            return;
        }

        // 如果设备已存在连接，先关闭旧连接
        RSocketRequester existing = deviceConnections.get(deviceId);
        if (existing != null) {
            log.info("设备 {} 存在旧连接，将替换为新连接", deviceId);
            remove(deviceId);
        }

        deviceConnections.put(deviceId, requester);
        
        // 监听连接关闭事件（延迟订阅，等待 rsocket 可用）
        Mono.fromCallable(requester::rsocket)
                .flatMap(rsocket -> {
                    if (rsocket == null) {
                        log.warn("设备 {} 的 rsocket 为 null，可能连接尚未完全建立", deviceId);
                        return Mono.empty();
                    }
                    return rsocket.onClose()
                            .doOnTerminate(() -> {
                                log.info("设备 {} 的 RSocket 连接已关闭", deviceId);
                                remove(deviceId);
                            });
                })
                .subscribe(
                        null,
                        error -> log.warn("监听设备 {} 连接关闭事件失败: {}", deviceId, error.getMessage())
                );

        log.info("设备 {} 已注册，当前在线设备数: {}", deviceId, deviceConnections.size());
    }

    /**
     * 移除设备连接
     *
     * @param deviceId 设备数据库ID
     */
    public void remove(String deviceId) {
        if (deviceId == null) {
            return;
        }

        RSocketRequester removed = deviceConnections.remove(deviceId);
        if (removed != null) {
            // 尝试关闭连接
            try {
                io.rsocket.RSocket rsocket = removed.rsocket();
                if (rsocket != null && !rsocket.isDisposed()) {
                    rsocket.dispose();
                }
            } catch (Exception e) {
                log.warn("关闭设备 {} 连接时出错: {}", deviceId, e.getMessage());
            }
            log.info("设备 {} 的连接已移除，当前在线设备数: {}", deviceId, deviceConnections.size());
        }
    }

    /**
     * 获取设备连接
     *
     * @param deviceId 设备数据库ID
     * @return RSocketRequester 或 null
     */
    public RSocketRequester getRequester(String deviceId) {
        return deviceConnections.get(deviceId);
    }

    /**
     * 检查设备是否在线
     *
     * @param deviceId 设备数据库ID
     * @return 是否在线
     */
    public boolean isOnline(String deviceId) {
        if (deviceId == null) {
            return false;
        }
        RSocketRequester requester = deviceConnections.get(deviceId);
        if (requester == null) {
            return false;
        }
        try {
            io.rsocket.RSocket rsocket = requester.rsocket();
            return rsocket != null && !rsocket.isDisposed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取在线设备数量
     */
    public int getOnlineCount() {
        return deviceConnections.size();
    }

    /**
     * 获取所有连接
     */
    public Map<String, RSocketRequester> getAllConnections() {
        return new ConcurrentHashMap<>(deviceConnections);
    }

    /**
     * 获取所有 Requester（仅值）
     */
    public Iterable<RSocketRequester> getAllRequesters() {
        return deviceConnections.values();
    }

    /**
     * 关闭所有连接
     */
    public void closeAll() {
        deviceConnections.forEach((deviceId, requester) -> {
            try {
                io.rsocket.RSocket rsocket = requester.rsocket();
                if (rsocket != null) {
                    rsocket.dispose();
                }
            } catch (Exception e) {
                log.warn("关闭设备 {} 连接时出错: {}", deviceId, e.getMessage());
            }
        });
        deviceConnections.clear();
        log.info("所有连接已关闭");
    }
}
