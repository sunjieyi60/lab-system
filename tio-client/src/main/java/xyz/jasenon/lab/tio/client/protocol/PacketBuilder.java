package xyz.jasenon.lab.tio.client.protocol;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据包构建器
 * 提供便捷的方法来构建SmartBoardPacket
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Slf4j
public class PacketBuilder {
    
    private final SeqIdGenerator seqIdGenerator;
    
    public PacketBuilder() {
        this.seqIdGenerator = new SeqIdGenerator();
    }
    
    public PacketBuilder(SeqIdGenerator seqIdGenerator) {
        this.seqIdGenerator = seqIdGenerator;
    }
    
    /**
     * 创建基础数据包
     * 
     * @param cmdType 指令类型
     * @param payload 负载数据
     * @return 数据包
     */
    public SmartBoardPacket build(Byte cmdType, byte[] payload) {
        return build(cmdType, payload, QosLevel.AT_MOST_ONCE, PacketFlags.NONE);
    }
    
    /**
     * 创建带QoS的数据包
     * 
     * @param cmdType 指令类型
     * @param payload 负载数据
     * @param qosLevel QoS级别
     * @return 数据包
     */
    public SmartBoardPacket build(Byte cmdType, byte[] payload, QosLevel qosLevel) {
        return build(cmdType, payload, qosLevel, PacketFlags.NONE);
    }
    
    /**
     * 创建完整的数据包
     * 
     * @param cmdType 指令类型
     * @param payload 负载数据
     * @param qosLevel QoS级别
     * @param flags 标志位
     * @return 数据包
     */
    public SmartBoardPacket build(Byte cmdType, byte[] payload, QosLevel qosLevel, byte flags) {
        SmartBoardPacket packet = new SmartBoardPacket();
        packet.setVersion((byte) 0x01);
        packet.setCmdType(cmdType);
        packet.setSeqId(seqIdGenerator.nextSeqId());
        packet.setQos(qosLevel.getValue());
        packet.setFlags(flags);
        packet.setReserved((byte) 0);
        packet.setPayload(payload);
        packet.setLength(payload != null ? payload.length : 0);
        
        // 如果需要确认，设置ACK_REQUIRED标志
        if (qosLevel.requiresAck()) {
            packet.setFlags(PacketFlags.setFlag(packet.getFlags(), PacketFlags.ACK_REQUIRED));
        }
        
        // 计算并设置校验和
        CheckSumCalculator.setCheckSum(packet);
        
        return packet;
    }
    
    /**
     * 创建注册响应包
     * 
     * @param token 设备token
     * @param configJson 配置JSON
     * @return 数据包
     */
    public SmartBoardPacket buildRegisterAck(String token, String configJson) {
        // TODO: 实现具体的注册响应数据格式
        byte[] payload = (token + "|" + configJson).getBytes();
        return build(CommandType.REGISTER_ACK, payload, QosLevel.AT_LEAST_ONCE);
    }
    
    /**
     * 创建心跳响应包
     * 
     * @param seqId 原心跳包的序列号
     * @param urgentCommand 紧急指令（可选）
     * @return 数据包
     */
    public SmartBoardPacket buildHeartbeatAck(Short seqId, byte[] urgentCommand) {
        SmartBoardPacket packet = new SmartBoardPacket();
        packet.setVersion((byte) 0x01);
        packet.setCmdType(CommandType.HEARTBEAT_ACK);
        packet.setSeqId(seqId); // 使用原心跳包的序列号
        packet.setQos(QosLevel.AT_MOST_ONCE.getValue());
        packet.setFlags(PacketFlags.NONE);
        packet.setReserved((byte) 0);
        packet.setPayload(urgentCommand);
        packet.setLength(urgentCommand != null ? urgentCommand.length : 0);
        
        CheckSumCalculator.setCheckSum(packet);
        
        return packet;
    }
    
    /**
     * 创建分片数据包
     * 
     * @param cmdType 指令类型
     * @param payload 负载数据
     * @param chunkIndex 分片索引（从0开始）
     * @param totalChunks 总分片数
     * @param qosLevel QoS级别
     * @return 数据包
     */
    public SmartBoardPacket buildFragment(Byte cmdType, byte[] payload, int chunkIndex, int totalChunks, QosLevel qosLevel) {
        byte flags = PacketFlags.NONE;
        
        // 设置分片标志
        if (chunkIndex == 0) {
            flags = PacketFlags.setFlag(flags, PacketFlags.START);
        } else if (chunkIndex == totalChunks - 1) {
            flags = PacketFlags.setFlag(flags, PacketFlags.END);
        } else {
            flags = PacketFlags.setFlag(flags, PacketFlags.MID);
        }
        
        return build(cmdType, payload, qosLevel, flags);
    }
    
    /**
     * 创建ACK确认包
     * 
     * @param ackSeqId 要确认的序列号
     * @return 数据包
     */
    public SmartBoardPacket buildAck(Short ackSeqId) {
        SmartBoardPacket packet = new SmartBoardPacket();
        packet.setVersion((byte) 0x01);
        packet.setCmdType(CommandType.HEARTBEAT_ACK); // 使用HEARTBEAT_ACK作为通用ACK
        packet.setSeqId(ackSeqId);
        packet.setQos(QosLevel.AT_MOST_ONCE.getValue());
        packet.setFlags(PacketFlags.ACK_REQUIRED);
        packet.setReserved((byte) 0);
        packet.setPayload(new byte[0]);
        packet.setLength(0);
        
        CheckSumCalculator.setCheckSum(packet);
        
        return packet;
    }
}


