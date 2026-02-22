package xyz.jasenon.lab.tioprotocol;

/**
 * 校验和计算工具
 * 使用简单的累加校验算法，计算协议头部的校验和
 * 
 * @author Jasenon_ce
 */
public class CheckSumCalculator {
    
    /**
     * 计算协议头部的校验和
     * 算法：对头部除checkSum字段外的所有字节进行累加，取低8位
     * 
     * @param packet 数据包
     * @return 校验和
     */
    public static byte calculate(ProtocolPacket packet) {
        int lengthValue = (packet.getPayload() != null) ? packet.getPayload().length : 0;
        
        int sum = 0;
        
        // magic (4 bytes)
        int magic = packet.getMagic();
        sum += (magic >>> 24) & 0xFF;
        sum += (magic >>> 16) & 0xFF;
        sum += (magic >>> 8) & 0xFF;
        sum += magic & 0xFF;
        
        // version (1 byte)
        sum += packet.getVersion() & 0xFF;
        
        // cmdType (1 byte)
        sum += packet.getCmdType() & 0xFF;
        
        // seqId (2 bytes)
        short seqId = packet.getSeqId();
        sum += (seqId >>> 8) & 0xFF;
        sum += seqId & 0xFF;
        
        // qos (1 byte)
        sum += packet.getQos() & 0xFF;
        
        // flags (1 byte)
        sum += packet.getFlags() & 0xFF;
        
        // reserved (1 byte)
        sum += packet.getReserved() & 0xFF;
        
        // checkSum位置跳过（索引14，不累加）
        
        // length (4 bytes)
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
    public static boolean verify(ProtocolPacket packet) {
        byte calculated = calculate(packet);
        return calculated == packet.getCheckSum();
    }
    
    /**
     * 计算并设置数据包的校验和
     * 
     * @param packet 数据包
     */
    public static void calculateAndSet(ProtocolPacket packet) {
        byte checkSum = calculate(packet);
        packet.setCheckSum(checkSum);
    }
}
