package xyz.jasenon.lab.class_time_table.t_io.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.server.intf.TioServerHandler;
import xyz.jasenon.lab.class_time_table.t_io.adapter.TioPacketAdapter;
import xyz.jasenon.lab.class_time_table.t_io.adapter.TioQosAdapter;
import xyz.jasenon.lab.class_time_table.t_io.codec.TioProtocolCodec;
import xyz.jasenon.lab.tioprotocol.PacketHeader;
import xyz.jasenon.lab.tioprotocol.ProtocolPacket;

import java.nio.ByteBuffer;

/**
 * 智能班牌 t-io 处理器
 * 使用 tio-protocol 进行编解码，集成 QoS 机制
 * 
 * @author Jasenon_ce
 */
@Component
public class SmartBoardTioHandler implements TioServerHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(SmartBoardTioHandler.class);
    
    private final TioProtocolCodec codec;
    private final TioQosAdapter qosAdapter;
    
    public SmartBoardTioHandler(TioProtocolCodec codec, TioQosAdapter qosAdapter) {
        this.codec = codec;
        this.qosAdapter = qosAdapter;
    }
    
    @Override
    public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, 
                         ChannelContext channelContext) throws TioDecodeException {
        // 检查是否有足够的数据读取头部
        if (readableLength < PacketHeader.HEADER_LENGTH) {
            return null;
        }
        
        // 设置位置
        buffer.position(position);
        
        // 读取魔数（不移动位置）
        int magic = buffer.getInt(position);
        if (magic != PacketHeader.MAGIC_NUMBER) {
            throw new TioDecodeException("魔数不匹配: 0x" + Integer.toHexString(magic));
        }
        
        // 读取长度字段（在头部最后4字节）
        int bodyLength = buffer.getInt(position + 12);
        if (bodyLength < 0) {
            throw new TioDecodeException("消息体长度无效: " + bodyLength);
        }
        
        // 检查是否有完整的数据包
        int totalLength = PacketHeader.HEADER_LENGTH + bodyLength;
        if (readableLength < totalLength) {
            return null;
        }
        
        // 解码数据包
        try {
            TioPacketAdapter packet = codec.decode(buffer);
            if (packet == null) {
                return null;
            }
            
            LOG.debug("收到消息: channel={}, cmdType={}, seqId={}, qos={}, flags={}",
                    channelContext.getClientNode(), packet.getCmdType(), packet.getSeqId(),
                    packet.getQos(), packet.getFlags());
            
            return packet;
        } catch (TioProtocolCodec.DecodeException e) {
            throw new TioDecodeException("解码失败: " + e.getMessage());
        }
    }
    
    @Override
    public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
        TioPacketAdapter adapter = (TioPacketAdapter) packet;
        return codec.encode(adapter);
    }
    
    @Override
    public void handler(Packet packet, ChannelContext channelContext) throws Exception {
        TioPacketAdapter adapter = (TioPacketAdapter) packet;
        ProtocolPacket protocolPacket = adapter.getProtocolPacket();
        byte cmdType = protocolPacket.getCmdType();
        
        // 处理 QoS（发送 ACK 如果需要）
        qosAdapter.handleReceived(channelContext, adapter);
        
        // TODO: 业务处理入口
        // 这里将交给业务层处理，当前版本仅记录日志
        LOG.info("收到业务消息: channel={}, cmdType={}, seqId={}", 
                channelContext.getClientNode(), cmdType, protocolPacket.getSeqId());
        
        // 示例：如果是心跳包，直接回复（实际业务中可能不需要）
        if (cmdType == xyz.jasenon.lab.tioprotocol.CommandType.HEARTBEAT) {
            ProtocolPacket heartbeatAck = new ProtocolPacket();
            heartbeatAck.setMagic(PacketHeader.MAGIC_NUMBER);
            heartbeatAck.setVersion(PacketHeader.VERSION);
            heartbeatAck.setCmdType(xyz.jasenon.lab.tioprotocol.CommandType.HEARTBEAT_ACK);
            heartbeatAck.setSeqId(protocolPacket.getSeqId());
            heartbeatAck.setPayload(new byte[0]);
            heartbeatAck.calculateCheckSum();
            qosAdapter.send(channelContext, heartbeatAck);
        }
    }
}
