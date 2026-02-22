package xyz.jasenon.lab.class_time_table.t_io.adapter;

import lombok.EqualsAndHashCode;
import org.tio.core.intf.Packet;
import xyz.jasenon.lab.tioprotocol.ProtocolPacket;

/**
 * t-io 数据包适配器
 * 将 tio-protocol 的 ProtocolPacket 适配为 t-io 的 Packet
 * 
 * @author Jasenon_ce
 */
@EqualsAndHashCode(callSuper = false)
public class TioPacketAdapter extends Packet {
    
    private final ProtocolPacket protocolPacket;
    
    public TioPacketAdapter(ProtocolPacket protocolPacket) {
        this.protocolPacket = protocolPacket;
    }
    
    /**
     * 获取封装的 ProtocolPacket
     */
    public ProtocolPacket getProtocolPacket() {
        return protocolPacket;
    }
    
    /**
     * 快捷访问方法：获取命令类型
     */
    public byte getCmdType() {
        return protocolPacket.getCmdType();
    }
    
    /**
     * 快捷访问方法：获取序列号
     */
    public short getSeqId() {
        return protocolPacket.getSeqId();
    }
    
    /**
     * 快捷访问方法：获取 QoS 级别
     */
    public byte getQos() {
        return protocolPacket.getQos();
    }
    
    /**
     * 快捷访问方法：获取标志
     */
    public byte getFlags() {
        return protocolPacket.getFlags();
    }
    
    /**
     * 快捷访问方法：获取负载
     */
    public byte[] getPayload() {
        return protocolPacket.getPayload();
    }
    
    /**
     * 快捷访问方法：是否需要确认
     */
    public boolean requiresAck() {
        return protocolPacket.requiresAck();
    }
    
    /**
     * 创建 ACK 包
     */
    public TioPacketAdapter createAck() {
        return new TioPacketAdapter(protocolPacket.createAckPacket());
    }
    
    @Override
    public String toString() {
        return "TioPacketAdapter{" +
                "protocolPacket=" + protocolPacket +
                '}';
    }
}
