package xyz.jasenon.lab.tio.client.protocol;

import lombok.Data;
import org.tio.core.intf.Packet;

/**
 * 智能班牌数据包协议
 * 与 class_time_table 服务端协议一致
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Data
public class SmartBoardPacket extends Packet {

    public static final int MAGIC_NUMBER = 0x5A5A5A5A;
    /** 协议头长度：magic(4)+version(1)+cmdType(1)+seqId(2)+qos(1)+flags(1)+reserved(1)+checkSum(1)+length(4)=16 */
    public static final int HEADER_LENGTH = 16;

    private Integer magic = MAGIC_NUMBER;
    private Byte version = 0x01;
    private Byte cmdType;
    private Short seqId;
    private Byte qos;
    private Byte flags;
    private Byte reserved;
    private Byte checkSum;
    private Integer length;

    private byte[] payload;

    /**
     * 获取QoS级别
     * 
     * @return QoS级别
     */
    public QosLevel getQosLevel() {
        return QosLevel.fromValue(qos != null ? qos : 0);
    }

    /**
     * 设置QoS级别
     * 
     * @param qosLevel QoS级别
     */
    public void setQosLevel(QosLevel qosLevel) {
        this.qos = qosLevel.getValue();
    }

    /**
     * 检查是否包含指定标志
     * 
     * @param flag 标志位
     * @return 是否包含
     */
    public boolean hasFlag(byte flag) {
        return flags != null && PacketFlags.hasFlag(flags, flag);
    }

    /**
     * 判断是否为分片包
     * 
     * @return 是否为分片包
     */
    public boolean isFragment() {
        return flags != null && PacketFlags.isFragment(flags);
    }

    /**
     * 判断是否为完整包
     * 
     * @return 是否为完整包
     */
    public boolean isComplete() {
        return flags == null || PacketFlags.isComplete(flags);
    }

    /**
     * 判断是否需要确认
     * 
     * @return 是否需要确认
     */
    public boolean requiresAck() {
        return getQosLevel().requiresAck() || hasFlag(PacketFlags.ACK_REQUIRED);
    }

    /**
     * 验证校验和
     * 
     * @return 校验是否通过
     */
    public boolean verifyCheckSum() {
        return CheckSumCalculator.verify(this);
    }

    /**
     * 计算并设置校验和
     */
    public void calculateCheckSum() {
        CheckSumCalculator.setCheckSum(this);
    }
}
