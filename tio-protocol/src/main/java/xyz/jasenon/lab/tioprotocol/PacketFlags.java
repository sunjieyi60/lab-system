package xyz.jasenon.lab.tioprotocol;

/**
 * 数据包标志位定义
 * 
 * @author Jasenon_ce
 */
public class PacketFlags {
    
    /** 无标志 */
    public static final byte NONE = 0x00;
    
    /** 需要确认标志 (bit 0) */
    public static final byte ACK_REQUIRED = 0x01;
    
    /** 重传标志 (bit 1) */
    public static final byte RETRANSMIT = 0x02;
    
    /**
     * 检查是否包含指定标志
     * 
     * @param flags 标志位值
     * @param flag 要检查的标志
     * @return 是否包含
     */
    public static boolean hasFlag(byte flags, byte flag) {
        return (flags & flag) != 0;
    }
    
    /**
     * 设置标志位
     * 
     * @param flags 原始标志位
     * @param flag 要设置的标志
     * @return 设置后的标志位
     */
    public static byte setFlag(byte flags, byte flag) {
        return (byte) (flags | flag);
    }
    
    /**
     * 清除标志位
     * 
     * @param flags 原始标志位
     * @param flag 要清除的标志
     * @return 清除后的标志位
     */
    public static byte clearFlag(byte flags, byte flag) {
        return (byte) (flags & ~flag);
    }
}
