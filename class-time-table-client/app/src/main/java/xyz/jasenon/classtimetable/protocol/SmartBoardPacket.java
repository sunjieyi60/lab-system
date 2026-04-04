package xyz.jasenon.classtimetable.protocol;

import org.tio.core.intf.Packet;

/**
 * 智能班牌数据包协议
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
public class SmartBoardPacket extends Packet {

    public final static Integer MAGIC_NUMBER = 0X5A5A5A5A;
    /** 协议头长度：magic(4)+version(1)+cmdType(1)+seqId(2)+qos(1)+flags(1)+reserved(1)+checkSum(1)+length(4)=16 */
    public final static Integer HEADER_LENGTH = 16;

    private Integer magic = MAGIC_NUMBER;
    private Byte version = 0x01;
    private Byte cmdType;
    private Short seqId;
    private Byte qos;
    private Byte flags;
    private Byte reserved;
    private Byte checkSum;
    private Integer length;
    // --- End of Header ---

    // --- Start of Payload ---
    private byte[] payload;
    // --- End of Payload ---


    public Integer getMagic() {
        return magic;
    }

    public void setMagic(Integer magic) {
        this.magic = magic;
    }

    public Byte getVersion() {
        return version;
    }

    public void setVersion(Byte version) {
        this.version = version;
    }

    public Byte getCmdType() {
        return cmdType;
    }

    public void setCmdType(Byte cmdType) {
        this.cmdType = cmdType;
    }

    public Short getSeqId() {
        return seqId;
    }

    public void setSeqId(Short seqId) {
        this.seqId = seqId;
    }

    public Byte getQos() {
        return qos;
    }

    public void setQos(Byte qos) {
        this.qos = qos;
    }

    public Byte getFlags() {
        return flags;
    }

    public void setFlags(Byte flags) {
        this.flags = flags;
    }

    public Byte getReserved() {
        return reserved;
    }

    public void setReserved(Byte reserved) {
        this.reserved = reserved;
    }

    public Byte getCheckSum() {
        return checkSum;
    }

    public void setCheckSum(Byte checkSum) {
        this.checkSum = checkSum;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

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
