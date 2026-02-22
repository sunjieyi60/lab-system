package xyz.jasenon.lab.class_time_table.t_io.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tio.core.ChannelContext;
import org.tio.core.intf.Packet;
import org.tio.server.intf.TioServerListener;
import xyz.jasenon.lab.class_time_table.t_io.adapter.TioQosAdapter;

/**
 * 智能班牌 t-io 监听器
 * 处理连接生命周期事件
 * 
 * @author Jasenon_ce
 */
@Component
public class SmartBoardTioListener implements TioServerListener {
    
    private static final Logger LOG = LoggerFactory.getLogger(SmartBoardTioListener.class);
    
    private final TioQosAdapter qosAdapter;
    
    public SmartBoardTioListener(TioQosAdapter qosAdapter) {
        this.qosAdapter = qosAdapter;
    }
    
    @Override
    public boolean onHeartbeatTimeout(ChannelContext channelContext, Long interval, int heartbeatTimeoutCount) {
        LOG.warn("心跳超时: channel={}, interval={}, count={}", 
                channelContext.getClientNode(), interval, heartbeatTimeoutCount);
        return false;
    }
    
    @Override
    public void onAfterConnected(ChannelContext channelContext, boolean isConnected, boolean isReconnect) {
        LOG.info("设备连接: channel={}, isConnected={}, isReconnect={}", 
                channelContext.getClientNode(), isConnected, isReconnect);
    }
    
    @Override
    public void onAfterDecoded(ChannelContext channelContext, Packet packet, int packetSize) {
        // 解码后的回调，通常不需要处理
    }
    
    @Override
    public void onAfterReceivedBytes(ChannelContext channelContext, int receivedBytes) {
        // 收到字节后的回调，通常不需要处理
    }
    
    @Override
    public void onAfterSent(ChannelContext channelContext, Packet packet, boolean isSentSuccess) {
        // 发送后的回调，通常不需要处理
    }
    
    @Override
    public void onAfterHandled(ChannelContext channelContext, Packet packet, long cost) {
        // 处理完成后的回调，用于性能统计
    }
    
    @Override
    public void onBeforeClose(ChannelContext channelContext, Throwable throwable, String remark, boolean isRemove) {
        // 通道关闭前，清理 QoS 数据
        String channelId = channelContext.getId();
        qosAdapter.clearChannel(channelContext);
        
        if (throwable != null) {
            LOG.warn("通道关闭: channel={}, remark={}, error={}", 
                    channelContext.getClientNode(), remark, throwable.getMessage());
        } else {
            LOG.info("通道关闭: channel={}, remark={}", 
                    channelContext.getClientNode(), remark);
        }
    }
}
