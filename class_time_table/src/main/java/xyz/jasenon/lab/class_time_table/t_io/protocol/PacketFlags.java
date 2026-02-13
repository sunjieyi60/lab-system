package xyz.jasenon.lab.class_time_table.t_io.protocol;

/**
 * 数据包分片标志位定义
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
public class PacketFlags {
    
    /** 无标志 */
    public static final byte NONE = 0x00;
    
    /** 分片开始标志 (bit 0) */
    public static final byte START = 0x01;
    
    /** 分片结束标志 (bit 1) */
    public static final byte END = 0x02;
    
    /** 分片中间标志 (START | END = 0x03 表示完整包) */
    public static final byte MID = 0x04;
    
    /** 需要确认标志 (bit 3) */
    public static final byte ACK_REQUIRED = 0x08;
    
    /** 重传标志 (bit 4) */
    public static final byte RETRANSMIT = 0x10;
    
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
    
    /**
     * 判断是否为完整包（非分片）
     * 
     * @param flags 标志位
     * @return 是否为完整包
     */
    public static boolean isComplete(byte flags) {
        return !hasFlag(flags, START) && !hasFlag(flags, END) && !hasFlag(flags, MID);
    }
    
    /**
     * 判断是否为分片包
     * 
     * @param flags 标志位
     * @return 是否为分片包
     */
    public static boolean isFragment(byte flags) {
        return hasFlag(flags, START) || hasFlag(flags, END) || hasFlag(flags, MID);
    }
}




