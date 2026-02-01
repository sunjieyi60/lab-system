package xyz.jasenon.lab.class_time_table.t_io.protocol;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * QoS管理器
 * 处理QoS确认机制，支持AT_LEAST_ONCE和EXACTLY_ONCE级别的消息确认
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Slf4j
@Component
public class QosManager {
    
    /** 等待确认的消息缓存，key为channelId:seqId */
    private final ConcurrentHashMap<String, PendingMessage> pendingMessages = new ConcurrentHashMap<>();
    
    /** 已确认的消息ID集合（用于EXACTLY_ONCE去重），key为channelId:seqId */
    private final ConcurrentHashMap<String, Long> confirmedMessages = new ConcurrentHashMap<>();
    
    /** 定时任务执行器，用于超时重传 */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    /** 默认重传超时时间（毫秒） */
    private static final long DEFAULT_RETRY_TIMEOUT = 5000;
    
    /** 确认消息保留时间（毫秒），用于去重 */
    private static final long CONFIRMATION_RETENTION_TIME = 60000;
    
    /**
     * 待确认消息
     */
    private static class PendingMessage {
        final SmartBoardPacket packet;
        final ChannelContext channelContext;
        @SuppressWarnings("unused")
        final long timestamp; // 用于未来扩展：统计消息延迟
        final int retryCount;
        
        PendingMessage(SmartBoardPacket packet, ChannelContext channelContext, int retryCount) {
            this.packet = packet;
            this.channelContext = channelContext;
            this.timestamp = System.currentTimeMillis();
            this.retryCount = retryCount;
        }
    }
    
    /**
     * 发送需要确认的消息
     * 
     * @param packet 数据包
     * @param channelContext 通道上下文
     */
    public void sendWithQos(SmartBoardPacket packet, ChannelContext channelContext) {
        QosLevel qosLevel = packet.getQosLevel();
        
        if (!qosLevel.requiresAck()) {
            // AT_MOST_ONCE级别，直接发送，不需要确认
            Tio.send(channelContext, packet);
            return;
        }
        
        // 检查是否需要去重（EXACTLY_ONCE）
        if (qosLevel.requiresDeduplication()) {
            String messageKey = getMessageKey(channelContext, packet.getSeqId());
            if (confirmedMessages.containsKey(messageKey)) {
                log.warn("检测到重复消息，已丢弃: channel={}, seqId={}", 
                        channelContext.getId(), packet.getSeqId());
                return;
            }
        }
        
        // 发送消息
        Tio.send(channelContext, packet);
        
        // 添加到待确认列表
        String key = getMessageKey(channelContext, packet.getSeqId());
        PendingMessage pending = new PendingMessage(packet, channelContext, 0);
        pendingMessages.put(key, pending);
        
        // 启动超时重传任务
        scheduleRetry(key, pending, DEFAULT_RETRY_TIMEOUT);
        
        log.debug("发送需要确认的消息: channel={}, seqId={}, qos={}", 
                channelContext.getId(), packet.getSeqId(), qosLevel);
    }
    
    /**
     * 处理ACK确认
     * 
     * @param ackSeqId 确认的序列号
     * @param channelContext 通道上下文
     */
    public void handleAck(Short ackSeqId, ChannelContext channelContext) {
        String key = getMessageKey(channelContext, ackSeqId);
        PendingMessage pending = pendingMessages.remove(key);
        
        if (pending != null) {
            // 如果是EXACTLY_ONCE级别，记录到已确认集合
            if (pending.packet.getQosLevel().requiresDeduplication()) {
                confirmedMessages.put(key, System.currentTimeMillis());
                
                // 定时清理已确认消息（避免内存泄漏）
                scheduler.schedule(() -> confirmedMessages.remove(key), 
                        CONFIRMATION_RETENTION_TIME, TimeUnit.MILLISECONDS);
            }
            
            log.debug("收到ACK确认: channel={}, seqId={}", channelContext.getId(), ackSeqId);
        } else {
            log.warn("收到未知序列号的ACK: channel={}, seqId={}", channelContext.getId(), ackSeqId);
        }
    }
    
    /**
     * 安排重传任务
     * 
     * @param key 消息key
     * @param pending 待确认消息
     * @param delay 延迟时间（毫秒）
     */
    private void scheduleRetry(String key, PendingMessage pending, long delay) {
        scheduler.schedule(() -> {
            PendingMessage current = pendingMessages.get(key);
            if (current == null) {
                // 已经收到ACK，不需要重传
                return;
            }
            
            // 检查通道是否还连接
            if (current.channelContext.isClosed || current.channelContext.isRemoved) {
                pendingMessages.remove(key);
                log.warn("通道已关闭，取消重传: channel={}, seqId={}", 
                        current.channelContext.getId(), current.packet.getSeqId());
                return;
            }
            
            // 重传消息（最多重传3次）
            if (current.retryCount < 3) {
                log.warn("消息未收到ACK，开始重传: channel={}, seqId={}, retryCount={}", 
                        current.channelContext.getId(), current.packet.getSeqId(), current.retryCount + 1);
                
                // 设置重传标志
                current.packet.setFlags(PacketFlags.setFlag(current.packet.getFlags(), PacketFlags.RETRANSMIT));
                current.packet.calculateCheckSum(); // 重新计算校验和
                
                Tio.send(current.channelContext, current.packet);
                
                // 更新重传次数
                PendingMessage retryPending = new PendingMessage(
                        current.packet, 
                        current.channelContext, 
                        current.retryCount + 1
                );
                pendingMessages.put(key, retryPending);
                
                // 安排下次重传（指数退避）
                scheduleRetry(key, retryPending, delay * 2);
            } else {
                // 超过最大重传次数，移除
                pendingMessages.remove(key);
                log.error("消息重传超过最大次数，放弃: channel={}, seqId={}", 
                        current.channelContext.getId(), current.packet.getSeqId());
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 生成消息key
     * 
     * @param channelContext 通道上下文
     * @param seqId 序列号
     * @return 消息key
     */
    private String getMessageKey(ChannelContext channelContext, Short seqId) {
        return channelContext.getId() + ":" + seqId;
    }
    
    /**
     * 清理指定通道的所有待确认消息（通道断开时调用）
     * 
     * @param channelContext 通道上下文
     */
    public void clearChannel(ChannelContext channelContext) {
        String channelId = channelContext.getId();
        pendingMessages.entrySet().removeIf(entry -> entry.getKey().startsWith(channelId + ":"));
        confirmedMessages.entrySet().removeIf(entry -> entry.getKey().startsWith(channelId + ":"));
        log.debug("清理通道的QoS数据: channel={}", channelId);
    }
    
    /**
     * 关闭管理器，清理资源
     */
    public void shutdown() {
        scheduler.shutdown();
        pendingMessages.clear();
        confirmedMessages.clear();
    }
}

