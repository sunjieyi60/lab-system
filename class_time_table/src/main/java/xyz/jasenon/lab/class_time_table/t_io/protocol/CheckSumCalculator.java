package xyz.jasenon.lab.class_time_table.t_io.protocol;

/**
 * 校验和计算工具
 * 使用简单的累加校验算法，计算协议头部的校验和
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
public class CheckSumCalculator {
    
    /**
     * 计算协议头部的校验和
     * 算法：对头部除checkSum字段外的所有字节进行累加，取低8位
     * 
     * @param packet 数据包
     * @return 校验和
     */
    public static byte calculate(SmartBoardPacket packet) {
        // 确保length字段已正确设置
        int lengthValue;
        if (packet.getLength() != null) {
            lengthValue = packet.getLength();
        } else if (packet.getPayload() != null) {
            lengthValue = packet.getPayload().length;
        } else {
            lengthValue = 0;
        }
        
        // 直接计算，避免创建buffer和flip操作
        int sum = 0;
        
        // magic (4 bytes)
        int magic = packet.getMagic() != null ? packet.getMagic() : SmartBoardPacket.MAGIC_NUMBER;
        sum += (magic >>> 24) & 0xFF;
        sum += (magic >>> 16) & 0xFF;
        sum += (magic >>> 8) & 0xFF;
        sum += magic & 0xFF;
        
        // version (1 byte)
        sum += (packet.getVersion() != null ? packet.getVersion() : 0x01) & 0xFF;
        
        // cmdType (1 byte)
        sum += (packet.getCmdType() != null ? packet.getCmdType() : 0) & 0xFF;
        
        // seqId (2 bytes)
        short seqId = packet.getSeqId() != null ? packet.getSeqId() : 0;
        sum += (seqId >>> 8) & 0xFF;
        sum += seqId & 0xFF;
        
        // qos (1 byte)
        sum += (packet.getQos() != null ? packet.getQos() : 0) & 0xFF;
        
        // flags (1 byte)
        sum += (packet.getFlags() != null ? packet.getFlags() : 0) & 0xFF;
        
        // reserved (1 byte)
        sum += (packet.getReserved() != null ? packet.getReserved() : 0) & 0xFF;
        
        // checkSum位置跳过（索引14，不累加）
        
        // length (4 bytes) - 最后4字节
        sum += (lengthValue >>> 24) & 0xFF;
        sum += (lengthValue >>> 16) & 0xFF;
        sum += (lengthValue >>> 8) & 0xFF;
        sum += lengthValue & 0xFF;
        
        // 取低8位作为校验和
        return (byte) (sum & 0xFF);
    }
    
    /**
     * 验证数据包的校验和
     * 
     * @param packet 数据包
     * @return 校验是否通过
     */
    public static boolean verify(SmartBoardPacket packet) {
        if (packet.getCheckSum() == null) {
            return false;
        }
        
        byte calculated = calculate(packet);
        return calculated == packet.getCheckSum();
    }
    
    /**
     * 计算并设置数据包的校验和
     * 
     * @param packet 数据包
     */
    public static void setCheckSum(SmartBoardPacket packet) {
        // 确保length字段已设置（从payload获取或使用已有值）
        if (packet.getLength() == null) {
            int payloadLength = packet.getPayload() != null ? packet.getPayload().length : 0;
            packet.setLength(payloadLength);
        }
        
        byte checkSum = calculate(packet);
        packet.setCheckSum(checkSum);
    }
}


