package xyz.jasenon.lab.tioprotocol;

/**
 * 命令类型定义
 * 
 * @author Jasenon_ce
 */
public class CommandType {
    
    // QoS 确认
    public static final byte QOS_ACK = 0x00;
    
    // 注册
    public static final byte REGISTER = 0x01;
    public static final byte REGISTER_ACK = 0x02;
    
    // 心跳
    public static final byte HEARTBEAT = 0x10;
    public static final byte HEARTBEAT_ACK = 0x11;
    
    // 保留给业务扩展的命令类型范围:
    // 0x20 - 0x2F: 人脸/生物识别
    // 0x30 - 0x3F: 门禁控制
    // 0x40 - 0x4F: 课表/日程
    // 0x50 - 0x5F: OTA升级
    // 0x60 - 0x7F: 预留
    // 0x80 - 0xFF: 用户自定义
    
    private CommandType() {
        // 工具类
    }
}
