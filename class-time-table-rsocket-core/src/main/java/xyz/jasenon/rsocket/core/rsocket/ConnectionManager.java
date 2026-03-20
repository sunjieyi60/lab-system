package xyz.jasenon.rsocket.core.rsocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

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
     * @param deviceDbId 设备数据库ID
     * @param requester RSocket 连接
     */
    public void registerConnection(String deviceDbId, RSocketRequester requester) {
        if (deviceDbId == null || requester == null) {
            log.warn("无效的注册参数: deviceDbId={}, requester={}", deviceDbId, requester);
            return;
        }

        // 如果设备已存在连接，先关闭旧连接
        RSocketRequester existing = deviceConnections.get(deviceDbId);
        if (existing != null && existing.rsocket() != null) {
            log.info("设备 {} 存在旧连接，将替换为新连接", deviceDbId);
            try {
                existing.rsocket().dispose();
            } catch (Exception e) {
                log.warn("关闭设备 {} 旧连接时出错: {}", deviceDbId, e.getMessage());
            }
        }

        deviceConnections.put(deviceDbId, requester);
        
        // 监听连接关闭事件
        requester.rsocket()
                .onClose()
                .doOnTerminate(() -> {
                    log.info("设备 {} 的 RSocket 连接已关闭", deviceDbId);
                    removeConnection(deviceDbId);
                })
                .subscribe();

        log.info("设备 {} 已注册，当前在线设备数: {}", deviceDbId, deviceConnections.size());
    }

    /**
     * 移除设备连接
     * 
     * @param deviceDbId 设备数据库ID
     */
    public void removeConnection(String deviceDbId) {
        if (deviceDbId == null) {
            return;
        }

        RSocketRequester removed = deviceConnections.remove(deviceDbId);
        if (removed != null) {
            log.info("设备 {} 的连接已移除，当前在线设备数: {}", deviceDbId, deviceConnections.size());
        }
    }

    /**
     * 获取设备连接
     * 
     * @param deviceDbId 设备数据库ID
     * @return RSocketRequester 或 null
     */
    public RSocketRequester getRequester(String deviceDbId) {
        return deviceConnections.get(deviceDbId);
    }

    /**
     * 检查设备是否在线
     * 
     * @param deviceDbId 设备数据库ID
     * @return 是否在线
     */
    public boolean isOnline(String deviceDbId) {
        if (deviceDbId == null) {
            return false;
        }
        RSocketRequester requester = deviceConnections.get(deviceDbId);
        return requester != null 
                && requester.rsocket() != null 
                && !requester.rsocket().isDisposed();
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
                requester.rsocket().dispose();
            } catch (Exception e) {
                log.warn("关闭设备 {} 连接时出错: {}", deviceId, e.getMessage());
            }
        });
        deviceConnections.clear();
        log.info("所有连接已关闭");
    }
}
