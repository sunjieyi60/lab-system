package xyz.jasenon.lab.tioprotocol.qos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.jasenon.lab.tioprotocol.PacketFlags;
import xyz.jasenon.lab.tioprotocol.ProtocolPacket;
import xyz.jasenon.lab.tioprotocol.QosLevel;

import java.util.Map;
import java.util.concurrent.*;

/**
 * QoS 管理器
 * 处理消息确认机制，支持 AT_LEAST_ONCE 和 EXACTLY_ONCE 级别的消息确认
 * 
 * <p>特点：</p>
 * <ul>
 *   <li>纯 Java 实现，不依赖任何通信框架</li>
 *   <li>通过 {@link PacketSender} 接口与底层传输解耦</li>
 *   <li>支持超时重传（指数退避策略）</li>
 *   <li>支持 EXACTLY_ONCE 去重</li>
 * </ul>
 * 
 * @author Jasenon_ce
 */
public class QosManager {
    
    private static final Logger log = LoggerFactory.getLogger(QosManager.class);
    
    /** 等待确认的消息缓存，key 为 channelId:seqId */
    private final Map<String, PendingMessage> pendingMessages = new ConcurrentHashMap<>();
    
    /** 已确认的消息ID集合（用于 EXACTLY_ONCE 去重），key 为 channelId:seqId */
    private final Map<String, Long> confirmedMessages = new ConcurrentHashMap<>();
    
    /** 定时任务执行器，用于超时重传 */
    private final ScheduledExecutorService scheduler;
    
    /** 数据包发送器（由使用方提供） */
    private final PacketSender packetSender;
    
    /** 默认重传超时时间（毫秒） */
    private long defaultRetryTimeout = 5000;
    
    /** 最大重传次数 */
    private int maxRetryCount = 3;
    
    /** 确认消息保留时间（毫秒），用于去重 */
    private long confirmationRetentionTime = 60000;
    
    /**
     * 数据包发送器接口
     * 由使用方实现，用于实际发送数据包
     */
    @FunctionalInterface
    public interface PacketSender {
        /**
         * 发送数据包
         * 
         * @param channelId 通道ID
         * @param packet 数据包
         * @return 是否发送成功
         */
        boolean send(String channelId, ProtocolPacket packet);
    }
    
    /**
     * 消息状态监听器
     */
    public interface MessageStatusListener {
        /**
         * 消息已确认
         * 
         * @param channelId 通道ID
         * @param seqId 序列号
         */
        void onAcknowledged(String channelId, short seqId);
        
        /**
         * 消息发送失败（超过最大重传次数）
         * 
         * @param channelId 通道ID
         * @param seqId 序列号
         * @param packet 数据包
         */
        void onFailed(String channelId, short seqId, ProtocolPacket packet);
    }
    
    /**
     * 待确认消息
     */
    private static class PendingMessage {
        final ProtocolPacket packet;
        final String channelId;
        final long timestamp;
        final int retryCount;
        
        PendingMessage(ProtocolPacket packet, String channelId, int retryCount) {
            this.packet = packet;
            this.channelId = channelId;
            this.timestamp = System.currentTimeMillis();
            this.retryCount = retryCount;
        }
    }
    
    // ==================== Constructors ====================
    
    /**
     * 创建 QoS 管理器
     * 
     * @param packetSender 数据包发送器
     */
    public QosManager(PacketSender packetSender) {
        this(packetSender, Executors.newScheduledThreadPool(2));
    }
    
    /**
     * 创建 QoS 管理器（指定调度器）
     * 
     * @param packetSender 数据包发送器
     * @param scheduler 定时任务调度器
     */
    public QosManager(PacketSender packetSender, ScheduledExecutorService scheduler) {
        this.packetSender = packetSender;
        this.scheduler = scheduler;
    }
    
    // ==================== Configuration ====================
    
    /**
     * 设置默认重传超时时间
     * 
     * @param timeout 超时时间（毫秒）
     * @return this
     */
    public QosManager setDefaultRetryTimeout(long timeout) {
        this.defaultRetryTimeout = timeout;
        return this;
    }
    
    /**
     * 设置最大重传次数
     * 
     * @param count 重传次数
     * @return this
     */
    public QosManager setMaxRetryCount(int count) {
        this.maxRetryCount = count;
        return this;
    }
    
    /**
     * 设置确认消息保留时间（用于去重）
     * 
     * @param time 保留时间（毫秒）
     * @return this
     */
    public QosManager setConfirmationRetentionTime(long time) {
        this.confirmationRetentionTime = time;
        return this;
    }
    
    // ==================== Core Methods ====================
    
    /**
     * 发送消息（自动根据 QoS 级别处理确认）
     * 
     * @param channelId 通道ID
     * @param packet 数据包
     * @return 是否成功发送（对于需要确认的消息，只表示已加入发送队列）
     */
    public boolean send(String channelId, ProtocolPacket packet) {
        if (packet == null || channelId == null || channelId.isEmpty()) {
            log.warn("发送消息失败：channelId 或 packet 为空");
            return false;
        }
        
        QosLevel qosLevel = packet.getQosLevel();
        
        // 设置通道ID
        packet.setChannelId(channelId);
        
        // 如果不需要确认，直接发送
        if (!qosLevel.requiresAck()) {
            return packetSender.send(channelId, packet);
        }
        
        // 检查是否需要去重（EXACTLY_ONCE）
        if (qosLevel.requiresDeduplication()) {
            String messageKey = getMessageKey(channelId, packet.getSeqId());
            if (confirmedMessages.containsKey(messageKey)) {
                log.warn("检测到重复消息，已丢弃: channel={}, seqId={}", channelId, packet.getSeqId());
                return false;
            }
        }
        
        // 添加到待确认列表
        String key = getMessageKey(channelId, packet.getSeqId());
        PendingMessage pending = new PendingMessage(packet, channelId, 0);
        pendingMessages.put(key, pending);
        
        // 先发送一次
        boolean sent = packetSender.send(channelId, packet);
        
        if (sent) {
            // 启动超时重传任务
            scheduleRetry(key, pending, defaultRetryTimeout);
            log.debug("发送需要确认的消息: channel={}, seqId={}, qos={}", channelId, packet.getSeqId(), qosLevel);
        } else {
            pendingMessages.remove(key);
            log.warn("消息发送失败: channel={}, seqId={}", channelId, packet.getSeqId());
        }
        
        return sent;
    }
    
    /**
     * 处理 ACK 确认
     * 
     * @param channelId 通道ID
     * @param ackSeqId 确认的序列号
     * @return 是否找到对应的消息
     */
    public boolean handleAck(String channelId, short ackSeqId) {
        String key = getMessageKey(channelId, ackSeqId);
        PendingMessage pending = pendingMessages.remove(key);
        
        if (pending != null) {
            // 如果是 EXACTLY_ONCE 级别，记录到已确认集合
            if (pending.packet.getQosLevel().requiresDeduplication()) {
                confirmedMessages.put(key, System.currentTimeMillis());
                
                // 定时清理已确认消息
                scheduler.schedule(() -> confirmedMessages.remove(key), 
                        confirmationRetentionTime, TimeUnit.MILLISECONDS);
            }
            
            log.debug("收到ACK确认: channel={}, seqId={}", channelId, ackSeqId);
            return true;
        }
        
        return false;
    }
    
    /**
     * 处理需要确认的消息（收到消息后发送ACK）
     * 
     * @param channelId 通道ID
     * @param packet 收到的数据包
     * @return 是否发送了ACK
     */
    public boolean sendAckIfRequired(String channelId, ProtocolPacket packet) {
        if (packet.requiresAck()) {
            ProtocolPacket ack = packet.createAckPacket();
            ack.setChannelId(channelId);
            boolean sent = packetSender.send(channelId, ack);
            if (sent) {
                log.debug("发送ACK: channel={}, seqId={}", channelId, packet.getSeqId());
            }
            return sent;
        }
        return false;
    }
    
    /**
     * 检查消息是否待确认
     * 
     * @param channelId 通道ID
     * @param seqId 序列号
     * @return 是否待确认
     */
    public boolean isPending(String channelId, short seqId) {
        return pendingMessages.containsKey(getMessageKey(channelId, seqId));
    }
    
    /**
     * 清理指定通道的所有待确认消息（通道断开时调用）
     * 
     * @param channelId 通道ID
     */
    public void clearChannel(String channelId) {
        String prefix = channelId + ":";
        pendingMessages.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
        confirmedMessages.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
        log.debug("清理通道的QoS数据: channel={}", channelId);
    }
    
    /**
     * 关闭管理器，清理资源
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        pendingMessages.clear();
        confirmedMessages.clear();
    }
    
    // ==================== Private Methods ====================
    
    /**
     * 安排重传任务
     */
    private void scheduleRetry(String key, PendingMessage pending, long delay) {
        scheduler.schedule(() -> {
            PendingMessage current = pendingMessages.get(key);
            if (current == null) {
                // 已经收到ACK，不需要重传
                return;
            }
            
            // 重传消息
            if (current.retryCount < maxRetryCount) {
                log.warn("消息未收到ACK，开始重传: channel={}, seqId={}, retryCount={}", 
                        current.channelId, current.packet.getSeqId(), current.retryCount + 1);
                
                // 设置重传标志
                current.packet.setFlags(PacketFlags.setFlag(current.packet.getFlags(), PacketFlags.RETRANSMIT));
                current.packet.calculateCheckSum();
                
                boolean sent = packetSender.send(current.channelId, current.packet);
                
                if (sent) {
                    // 更新重传次数
                    PendingMessage retryPending = new PendingMessage(
                            current.packet, current.channelId, current.retryCount + 1);
                    pendingMessages.put(key, retryPending);
                    
                    // 安排下次重传（指数退避）
                    scheduleRetry(key, retryPending, delay * 2);
                } else {
                    pendingMessages.remove(key);
                    log.error("消息重传发送失败: channel={}, seqId={}", 
                            current.channelId, current.packet.getSeqId());
                }
            } else {
                // 超过最大重传次数，移除
                pendingMessages.remove(key);
                log.error("消息重传超过最大次数，放弃: channel={}, seqId={}", 
                        current.channelId, current.packet.getSeqId());
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 生成消息key
     */
    private String getMessageKey(String channelId, short seqId) {
        return channelId + ":" + seqId;
    }
    
    // ==================== Statistics ====================
    
    /**
     * 获取待确认消息数量
     */
    public int getPendingCount() {
        return pendingMessages.size();
    }
    
    /**
     * 获取已确认消息数量（用于去重缓存）
     */
    public int getConfirmedCount() {
        return confirmedMessages.size();
    }
}
