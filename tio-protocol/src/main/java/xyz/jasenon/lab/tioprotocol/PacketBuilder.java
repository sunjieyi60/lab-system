package xyz.jasenon.lab.tioprotocol;

/**
 * 数据包构建器
 * 提供便捷方法构建 ProtocolPacket
 * 
 * @author Jasenon_ce
 */
public class PacketBuilder {
    
    private final SeqIdGenerator seqIdGenerator;
    
    /**
     * 创建默认构建器
     */
    public PacketBuilder() {
        this.seqIdGenerator = new SeqIdGenerator();
    }
    
    /**
     * 使用指定序列号生成器创建构建器
     */
    public PacketBuilder(SeqIdGenerator seqIdGenerator) {
        this.seqIdGenerator = seqIdGenerator;
    }
    
    /**
     * 创建基础数据包
     * 
     * @param cmdType 命令类型
     * @param payload 负载数据
     * @return 数据包
     */
    public ProtocolPacket build(byte cmdType, byte[] payload) {
        return build(cmdType, payload, QosLevel.AT_MOST_ONCE);
    }
    
    /**
     * 创建带 QoS 的数据包
     * 
     * @param cmdType 命令类型
     * @param payload 负载数据
     * @param qosLevel QoS 级别
     * @return 数据包
     */
    public ProtocolPacket build(byte cmdType, byte[] payload, QosLevel qosLevel) {
        ProtocolPacket packet = new ProtocolPacket();
        packet.setMagic(PacketHeader.MAGIC_NUMBER);
        packet.setVersion(PacketHeader.VERSION);
        packet.setCmdType(cmdType);
        packet.setSeqId(seqIdGenerator.nextSeqId());
        packet.setQos(qosLevel.getValue());
        
        // 根据 QoS 级别设置标志
        byte flags = PacketFlags.NONE;
        if (qosLevel.requiresAck()) {
            flags = PacketFlags.setFlag(flags, PacketFlags.ACK_REQUIRED);
        }
        packet.setFlags(flags);
        
        packet.setReserved((byte) 0);
        packet.setPayload(payload);
        packet.calculateCheckSum();
        
        return packet;
    }
    
    /**
     * 创建 ACK 确认包
     * 
     * @param ackSeqId 要确认的序列号
     * @return ACK 数据包
     */
    public ProtocolPacket buildAck(short ackSeqId) {
        ProtocolPacket packet = new ProtocolPacket();
        packet.setMagic(PacketHeader.MAGIC_NUMBER);
        packet.setVersion(PacketHeader.VERSION);
        packet.setCmdType(CommandType.QOS_ACK);
        packet.setSeqId(ackSeqId);
        packet.setQos(QosLevel.AT_MOST_ONCE.getValue());
        packet.setFlags(PacketFlags.NONE);
        packet.setReserved((byte) 0);
        packet.setPayload(new byte[0]);
        packet.calculateCheckSum();
        return packet;
    }
    
    /**
     * 创建心跳包
     * 
     * @return 心跳数据包
     */
    public ProtocolPacket buildHeartbeat() {
        return build(CommandType.HEARTBEAT, new byte[0], QosLevel.AT_MOST_ONCE);
    }
    
    /**
     * 创建心跳响应包
     * 
     * @param seqId 原心跳包的序列号
     * @return 心跳响应数据包
     */
    public ProtocolPacket buildHeartbeatAck(short seqId) {
        ProtocolPacket packet = new ProtocolPacket();
        packet.setMagic(PacketHeader.MAGIC_NUMBER);
        packet.setVersion(PacketHeader.VERSION);
        packet.setCmdType(CommandType.HEARTBEAT_ACK);
        packet.setSeqId(seqId);
        packet.setQos(QosLevel.AT_MOST_ONCE.getValue());
        packet.setFlags(PacketFlags.NONE);
        packet.setReserved((byte) 0);
        packet.setPayload(new byte[0]);
        packet.calculateCheckSum();
        return packet;
    }
    
    /**
     * 获取序列号生成器
     */
    public SeqIdGenerator getSeqIdGenerator() {
        return seqIdGenerator;
    }
}
