package xyz.jasenon.lab.tioprotocol;

/**
 * 协议头定义
 * 固定 16 字节头部结构：
 * <pre>
 * | 字段      | 长度   | 说明                |
 * |-----------|--------|---------------------|
 * | magic     | 4字节  | 魔数 0x5A5A5A5A     |
 * | version   | 1字节  | 协议版本            |
 * | cmdType   | 1字节  | 命令类型            |
 * | seqId     | 2字节  | 序列号              |
 * | qos       | 1字节  | QoS级别             |
 * | flags     | 1字节  | 标志位              |
 * | reserved  | 1字节  | 保留字段            |
 * | checkSum  | 1字节  | 校验和              |
 * | length    | 4字节  | 负载长度            |
 * </pre>
 * 
 * @author Jasenon_ce
 */
public class PacketHeader {
    
    /** 魔数 */
    public static final int MAGIC_NUMBER = 0x5A5A5A5A;
    
    /** 协议头长度 */
    public static final int HEADER_LENGTH = 16;
    
    /** 协议版本 */
    public static final byte VERSION = 0x01;
    
    private PacketHeader() {
        // 工具类
    }
}
