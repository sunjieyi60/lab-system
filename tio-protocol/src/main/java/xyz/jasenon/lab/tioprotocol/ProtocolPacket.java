package xyz.jasenon.lab.tioprotocol;

import java.util.Arrays;

/**
 * 协议数据包
 * 独立于任何通信框架的基础数据包结构
 * 
 * @author Jasenon_ce
 */
public class ProtocolPacket {
    
    // --- Header Fields ---
    private int magic = PacketHeader.MAGIC_NUMBER;
    private byte version = PacketHeader.VERSION;
    private byte cmdType;
    private short seqId;
    private byte qos;
    private byte flags;
    private byte reserved;
    private byte checkSum;
    private int length;
    
    // --- Payload ---
    private byte[] payload;
    
    // --- Transient Fields (not part of protocol) ---
    private transient String channelId; // 关联的通道ID
    private transient long sendTime;    // 发送时间戳
    
    public ProtocolPacket() {
    }
    
    // ==================== Getters & Setters ====================
    
    public int getMagic() {
        return magic;
    }
    
    public void setMagic(int magic) {
        this.magic = magic;
    }
    
    public byte getVersion() {
        return version;
    }
    
    public void setVersion(byte version) {
        this.version = version;
    }
    
    public byte getCmdType() {
        return cmdType;
    }
    
    public void setCmdType(byte cmdType) {
        this.cmdType = cmdType;
    }
    
    public short getSeqId() {
        return seqId;
    }
    
    public void setSeqId(short seqId) {
        this.seqId = seqId;
    }
    
    public byte getQos() {
        return qos;
    }
    
    public void setQos(byte qos) {
        this.qos = qos;
    }
    
    public byte getFlags() {
        return flags;
    }
    
    public void setFlags(byte flags) {
        this.flags = flags;
    }
    
    public byte getReserved() {
        return reserved;
    }
    
    public void setReserved(byte reserved) {
        this.reserved = reserved;
    }
    
    public byte getCheckSum() {
        return checkSum;
    }
    
    public void setCheckSum(byte checkSum) {
        this.checkSum = checkSum;
    }
    
    public int getLength() {
        return length;
    }
    
    public void setLength(int length) {
        this.length = length;
    }
    
    public byte[] getPayload() {
        return payload;
    }
    
    public void setPayload(byte[] payload) {
        this.payload = payload;
        this.length = (payload != null) ? payload.length : 0;
    }
    
    public String getChannelId() {
        return channelId;
    }
    
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
    
    public long getSendTime() {
        return sendTime;
    }
    
    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * 获取QoS级别
     */
    public QosLevel getQosLevel() {
        return QosLevel.fromValue(qos);
    }
    
    /**
     * 设置QoS级别
     */
    public void setQosLevel(QosLevel qosLevel) {
        this.qos = qosLevel.getValue();
    }
    
    /**
     * 检查是否包含指定标志
     */
    public boolean hasFlag(byte flag) {
        return PacketFlags.hasFlag(flags, flag);
    }
    
    /**
     * 判断是否需要确认
     */
    public boolean requiresAck() {
        return getQosLevel().requiresAck() || hasFlag(PacketFlags.ACK_REQUIRED);
    }
    
    /**
     * 验证校验和
     */
    public boolean verifyCheckSum() {
        return CheckSumCalculator.verify(this);
    }
    
    /**
     * 计算并设置校验和
     */
    public void calculateCheckSum() {
        CheckSumCalculator.calculateAndSet(this);
    }
    
    /**
     * 创建ACK确认包
     */
    public ProtocolPacket createAckPacket() {
        ProtocolPacket ack = new ProtocolPacket();
        ack.setMagic(PacketHeader.MAGIC_NUMBER);
        ack.setVersion(PacketHeader.VERSION);
        ack.setCmdType(CommandType.QOS_ACK);
        ack.setSeqId(this.seqId);
        ack.setQos(QosLevel.AT_MOST_ONCE.getValue());
        ack.setFlags(PacketFlags.NONE);
        ack.setReserved((byte) 0);
        ack.setPayload(new byte[0]);
        ack.calculateCheckSum();
        return ack;
    }
    
    /**
     * 获取消息唯一标识（用于QoS去重）
     */
    public String getMessageKey() {
        return channelId + ":" + seqId;
    }
    
    @Override
    public String toString() {
        return "ProtocolPacket{" +
                "magic=0x" + Integer.toHexString(magic) +
                ", version=" + version +
                ", cmdType=" + cmdType +
                ", seqId=" + seqId +
                ", qos=" + qos +
                ", flags=" + flags +
                ", length=" + length +
                ", payload=" + (payload != null ? payload.length + " bytes" : "null") +
                ", channelId='" + channelId + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProtocolPacket that = (ProtocolPacket) o;
        return magic == that.magic &&
                version == that.version &&
                cmdType == that.cmdType &&
                seqId == that.seqId &&
                qos == that.qos &&
                flags == that.flags &&
                length == that.length &&
                Arrays.equals(payload, that.payload);
    }
    
    @Override
    public int hashCode() {
        int result = magic;
        result = 31 * result + version;
        result = 31 * result + cmdType;
        result = 31 * result + seqId;
        result = 31 * result + qos;
        result = 31 * result + flags;
        result = 31 * result + length;
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }
}
