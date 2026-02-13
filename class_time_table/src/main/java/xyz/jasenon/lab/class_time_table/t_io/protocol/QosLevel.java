package xyz.jasenon.lab.class_time_table.t_io.protocol;

/**
 * QoS（服务质量）级别定义
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
public enum QosLevel {
    
    /**
     * 最多一次 (0) - 不保证送达，不重复发送
     * 适用于：心跳包、状态上报等可丢失数据
     */
    AT_MOST_ONCE((byte) 0),
    
    /**
     * 至少一次 (1) - 保证送达，但可能重复
     * 适用于：指令下发、数据同步等需要保证送达的场景
     */
    AT_LEAST_ONCE((byte) 1),
    
    /**
     * 精确一次 (2) - 保证送达且不重复
     * 适用于：关键业务操作、金融交易等不允许重复的场景
     */
    EXACTLY_ONCE((byte) 2);
    
    private final byte value;
    
    QosLevel(byte value) {
        this.value = value;
    }
    
    public byte getValue() {
        return value;
    }
    
    /**
     * 根据值获取QoS级别
     * 
     * @param value 值
     * @return QoS级别，如果值无效则返回AT_MOST_ONCE
     */
    public static QosLevel fromValue(byte value) {
        for (QosLevel level : values()) {
            if (level.value == value) {
                return level;
            }
        }
        return AT_MOST_ONCE; // 默认值
    }
    
    /**
     * 判断是否需要确认
     * 
     * @return 是否需要确认
     */
    public boolean requiresAck() {
        return this != AT_MOST_ONCE;
    }
    
    /**
     * 判断是否需要去重处理
     * 
     * @return 是否需要去重
     */
    public boolean requiresDeduplication() {
        return this == EXACTLY_ONCE;
    }
}




