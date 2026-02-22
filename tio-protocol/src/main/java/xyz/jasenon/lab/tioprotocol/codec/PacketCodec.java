package xyz.jasenon.lab.tioprotocol.codec;

import xyz.jasenon.lab.tioprotocol.PacketHeader;
import xyz.jasenon.lab.tioprotocol.ProtocolPacket;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 协议数据包编解码器
 * 负责 ProtocolPacket 与字节数组之间的转换
 * 
 * <p>编解码规则：</p>
 * <ul>
 *   <li>固定 16 字节头部 + 变长负载</li>
 *   <li>大端字节序（Big Endian）</li>
 *   <li>校验和覆盖整个头部（除 checkSum 字段本身）</li>
 * </ul>
 * 
 * @author Jasenon_ce
 */
public class PacketCodec {
    
    private final ByteOrder byteOrder;
    
    /**
     * 创建默认编解码器（大端字节序）
     */
    public PacketCodec() {
        this(ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * 创建指定字节序的编解码器
     * 
     * @param byteOrder 字节序
     */
    public PacketCodec(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }
    
    /**
     * 将数据包编码为字节数组
     * 
     * @param packet 数据包
     * @return 字节数组
     */
    public byte[] encode(ProtocolPacket packet) {
        byte[] payload = (packet.getPayload() != null) ? packet.getPayload() : new byte[0];
        int bodyLength = payload.length;
        
        // 确保校验和已计算
        if (packet.getCheckSum() == 0 && packet.getPayload() != null) {
            packet.calculateCheckSum();
        }
        
        ByteBuffer buffer = ByteBuffer.allocate(PacketHeader.HEADER_LENGTH + bodyLength);
        buffer.order(byteOrder);
        
        // 写入头部
        buffer.putInt(packet.getMagic());
        buffer.put(packet.getVersion());
        buffer.put(packet.getCmdType());
        buffer.putShort(packet.getSeqId());
        buffer.put(packet.getQos());
        buffer.put(packet.getFlags());
        buffer.put(packet.getReserved());
        buffer.put(packet.getCheckSum());
        buffer.putInt(bodyLength);
        
        // 写入负载
        if (bodyLength > 0) {
            buffer.put(payload);
        }
        
        return buffer.array();
    }
    
    /**
     * 从字节数组解码数据包
     * 
     * @param data 字节数组
     * @return 数据包
     * @throws DecodeException 解码失败
     */
    public ProtocolPacket decode(byte[] data) throws DecodeException {
        if (data == null || data.length < PacketHeader.HEADER_LENGTH) {
            throw new DecodeException("数据长度不足，无法解析头部");
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(byteOrder);
        
        ProtocolPacket packet = new ProtocolPacket();
        
        // 读取头部
        packet.setMagic(buffer.getInt());
        packet.setVersion(buffer.get());
        packet.setCmdType(buffer.get());
        packet.setSeqId(buffer.getShort());
        packet.setQos(buffer.get());
        packet.setFlags(buffer.get());
        packet.setReserved(buffer.get());
        packet.setCheckSum(buffer.get());
        int bodyLength = buffer.getInt();
        
        // 验证魔数
        if (packet.getMagic() != PacketHeader.MAGIC_NUMBER) {
            throw new DecodeException("魔数不匹配: expected=0x" + 
                    Integer.toHexString(PacketHeader.MAGIC_NUMBER) + 
                    ", actual=0x" + Integer.toHexString(packet.getMagic()));
        }
        
        // 验证版本
        if (packet.getVersion() != PacketHeader.VERSION) {
            throw new DecodeException("协议版本不匹配: expected=" + PacketHeader.VERSION + 
                    ", actual=" + packet.getVersion());
        }
        
        // 验证数据长度
        if (bodyLength < 0) {
            throw new DecodeException("消息体长度无效: " + bodyLength);
        }
        
        int totalLength = PacketHeader.HEADER_LENGTH + bodyLength;
        if (data.length < totalLength) {
            throw new DecodeException("数据长度不足，期望 " + totalLength + " 字节，实际 " + data.length + " 字节");
        }
        
        // 读取负载
        if (bodyLength > 0) {
            byte[] payload = new byte[bodyLength];
            buffer.get(payload);
            packet.setPayload(payload);
        } else {
            packet.setPayload(new byte[0]);
        }
        
        // 验证校验和
        if (!packet.verifyCheckSum()) {
            throw new DecodeException("校验和验证失败");
        }
        
        return packet;
    }
    
    /**
     * 尝试从 ByteBuffer 解码数据包（用于流式解码）
 * 
     * @param buffer 字节缓冲区
     * @return 数据包，如果数据不足则返回 null
     * @throws DecodeException 解码失败
     */
    public ProtocolPacket decode(ByteBuffer buffer) throws DecodeException {
        if (buffer.remaining() < PacketHeader.HEADER_LENGTH) {
            return null; // 数据不足，等待更多数据
        }
        
        // 标记当前位置
        buffer.mark();
        
        // 读取头部
        int magic = buffer.getInt();
        if (magic != PacketHeader.MAGIC_NUMBER) {
            buffer.reset();
            throw new DecodeException("魔数不匹配: 0x" + Integer.toHexString(magic));
        }
        
        byte version = buffer.get();
        byte cmdType = buffer.get();
        short seqId = buffer.getShort();
        byte qos = buffer.get();
        byte flags = buffer.get();
        byte reserved = buffer.get();
        byte checkSum = buffer.get();
        int bodyLength = buffer.getInt();
        
        // 检查数据是否完整
        if (buffer.remaining() < bodyLength) {
            buffer.reset(); // 恢复位置，等待更多数据
            return null;
        }
        
        // 读取负载
        byte[] payload = null;
        if (bodyLength > 0) {
            payload = new byte[bodyLength];
            buffer.get(payload);
        }
        
        // 构建数据包
        ProtocolPacket packet = new ProtocolPacket();
        packet.setMagic(magic);
        packet.setVersion(version);
        packet.setCmdType(cmdType);
        packet.setSeqId(seqId);
        packet.setQos(qos);
        packet.setFlags(flags);
        packet.setReserved(reserved);
        packet.setCheckSum(checkSum);
        packet.setPayload(payload);
        
        // 验证校验和
        if (!packet.verifyCheckSum()) {
            throw new DecodeException("校验和验证失败");
        }
        
        return packet;
    }
    
    /**
     * 获取数据包长度（从头部信息）
 * 
     * @param headerData 头部数据（至少16字节）
     * @return 完整数据包长度（头部+负载），如果数据不足返回 -1
     */
    public static int getPacketLength(byte[] headerData) {
        if (headerData == null || headerData.length < PacketHeader.HEADER_LENGTH) {
            return -1;
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(headerData);
        // 跳过前12字节
        buffer.position(12);
        int bodyLength = buffer.getInt();
        
        return PacketHeader.HEADER_LENGTH + bodyLength;
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
