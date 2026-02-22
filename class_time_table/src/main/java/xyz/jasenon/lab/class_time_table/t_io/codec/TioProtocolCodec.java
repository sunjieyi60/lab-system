package xyz.jasenon.lab.class_time_table.t_io.codec;

import org.springframework.stereotype.Component;
import xyz.jasenon.lab.class_time_table.t_io.adapter.TioPacketAdapter;
import xyz.jasenon.lab.tioprotocol.ProtocolPacket;
import xyz.jasenon.lab.tioprotocol.codec.PacketCodec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * t-io 协议编解码器
 * 基于 tio-protocol 的 PacketCodec 封装
 * 
 * @author Jasenon_ce
 */
@Component
public class TioProtocolCodec {
    
    private final PacketCodec packetCodec;
    
    public TioProtocolCodec() {
        this.packetCodec = new PacketCodec(ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * 编码数据包为字节缓冲区
     * 
     * @param packet 适配器数据包
     * @return 字节缓冲区
     */
    public ByteBuffer encode(TioPacketAdapter packet) {
        byte[] data = packetCodec.encode(packet.getProtocolPacket());
        return ByteBuffer.wrap(data);
    }
    
    /**
     * 编码 ProtocolPacket 为字节缓冲区
     */
    public ByteBuffer encode(ProtocolPacket packet) {
        byte[] data = packetCodec.encode(packet);
        return ByteBuffer.wrap(data);
    }
    
    /**
     * 从字节缓冲区解码数据包
     * 
     * @param buffer 字节缓冲区
     * @return 适配器数据包，如果数据不足返回 null
     */
    public TioPacketAdapter decode(ByteBuffer buffer) throws DecodeException {
        try {
            ProtocolPacket protocolPacket = packetCodec.decode(buffer);
            if (protocolPacket == null) {
                return null;
            }
            return new TioPacketAdapter(protocolPacket);
        } catch (xyz.jasenon.lab.tioprotocol.codec.PacketCodec.DecodeException e) {
            throw new DecodeException(e.getMessage(), e);
        }
    }
    
    /**
     * 获取底层 PacketCodec
     */
    public PacketCodec getPacketCodec() {
        return packetCodec;
    }
    
    /**
     * 解码异常
     */
    public static class DecodeException extends Exception {
        public DecodeException(String message) {
            super(message);
        }
        
        public DecodeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
