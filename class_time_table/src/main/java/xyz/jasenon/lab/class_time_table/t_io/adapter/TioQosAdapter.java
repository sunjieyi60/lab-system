package xyz.jasenon.lab.class_time_table.t_io.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.core.TioConfig;
import xyz.jasenon.lab.class_time_table.t_io.codec.TioProtocolCodec;
import xyz.jasenon.lab.tioprotocol.ProtocolPacket;
import xyz.jasenon.lab.tioprotocol.qos.QosManager;

import java.util.concurrent.ScheduledExecutorService;

/**
 * t-io QoS 适配器
 * 将 tio-protocol 的 QosManager 与 t-io 框架集成
 * 
 * @author Jasenon_ce
 */
@Component
public class TioQosAdapter {
    
    private static final Logger LOG = LoggerFactory.getLogger(TioQosAdapter.class);
    
    private final QosManager qosManager;
    private final TioProtocolCodec codec;
    private TioConfig tioConfig;
    
    public TioQosAdapter(TioProtocolCodec codec) {
        this.codec = codec;
        // 创建 QosManager，提供 PacketSender 实现
        this.qosManager = new QosManager(this::doSend);
    }
    
    /**
     * 设置 t-io 配置（需要在服务器启动后设置）
     */
    public void setTioConfig(TioConfig tioConfig) {
        this.tioConfig = tioConfig;
    }
    
    /**
     * 发送消息（带 QoS 支持）
     * 
     * @param channelContext 通道上下文
     * @param packet 数据包
     * @return 是否发送成功
     */
    public boolean send(ChannelContext channelContext, TioPacketAdapter packet) {
        String channelId = channelContext.getId();
        ProtocolPacket protocolPacket = packet.getProtocolPacket();
        protocolPacket.setChannelId(channelId);
        
        return qosManager.send(channelId, protocolPacket);
    }
    
    /**
     * 发送消息（原始 ProtocolPacket）
     */
    public boolean send(ChannelContext channelContext, ProtocolPacket packet) {
        String channelId = channelContext.getId();
        packet.setChannelId(channelId);
        
        return qosManager.send(channelId, packet);
    }
    
    /**
     * 处理收到的消息（自动发送 ACK 如果需要）
     * 
     * @param channelContext 通道上下文
     * @param packet 数据包
     * @return 是否发送了 ACK
     */
    public boolean handleReceived(ChannelContext channelContext, TioPacketAdapter packet) {
        String channelId = channelContext.getId();
        ProtocolPacket protocolPacket = packet.getProtocolPacket();
        
        // 如果是 ACK 消息，处理确认
        if (protocolPacket.getCmdType() == xyz.jasenon.lab.tioprotocol.CommandType.QOS_ACK) {
            boolean handled = qosManager.handleAck(channelId, protocolPacket.getSeqId());
            if (handled) {
                LOG.debug("处理ACK确认: channel={}, seqId={}", channelId, protocolPacket.getSeqId());
            }
            return handled;
        }
        
        // 如果需要确认，发送 ACK
        return qosManager.sendAckIfRequired(channelId, protocolPacket);
    }
    
    /**
     * 处理 ACK 确认
     */
    public boolean handleAck(ChannelContext channelContext, short seqId) {
        return qosManager.handleAck(channelContext.getId(), seqId);
    }
    
    /**
     * 清理通道的 QoS 数据（通道断开时调用）
     */
    public void clearChannel(ChannelContext channelContext) {
        qosManager.clearChannel(channelContext.getId());
    }
    
    /**
     * 关闭适配器
     */
    public void shutdown() {
        qosManager.shutdown();
    }
    
    /**
     * 配置 QoS 参数
     */
    public TioQosAdapter configure(long retryTimeout, int maxRetryCount, long retentionTime) {
        qosManager.setDefaultRetryTimeout(retryTimeout)
                  .setMaxRetryCount(maxRetryCount)
                  .setConfirmationRetentionTime(retentionTime);
        return this;
    }
    
    // ==================== Private Methods ====================
    
    /**
     * 实际发送数据包（由 QosManager 回调）
     */
    private boolean doSend(String channelId, ProtocolPacket packet) {
        if (tioConfig == null) {
            LOG.error("TioConfig 未设置，无法发送消息");
            return false;
        }
        
        // 通过 Tio.send 发送
        ChannelContext channelContext = Tio.getChannelContextById(tioConfig, channelId);
        if (channelContext == null) {
            LOG.warn("通道不存在: {}", channelId);
            return false;
        }
        
        if (channelContext.isClosed) {
            LOG.warn("通道已关闭: {}", channelId);
            return false;
        }
        
        TioPacketAdapter adapter = new TioPacketAdapter(packet);
        return Tio.send(channelContext, adapter);
    }
}
