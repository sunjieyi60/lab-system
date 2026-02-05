package xyz.jasenon.lab.tio.client;

import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.client.intf.TioClientHandler;
import xyz.jasenon.lab.tio.client.protocol.CommandType;
import xyz.jasenon.lab.tio.client.protocol.SmartBoardPacket;

import java.nio.ByteBuffer;

/**
 * 与 class_time_table 服务端编解码一致，用于测试。
 */
public class SmartBoardClientHandler implements TioClientHandler {

    @Override
    public Packet decode(ByteBuffer byteBuffer, int limit, int position, int readableLength, ChannelContext channelContext) throws TioDecodeException {
        if (readableLength < SmartBoardPacket.HEADER_LENGTH) {
            return null;
        }
        byteBuffer.position(position);

        byte[] header = new byte[SmartBoardPacket.HEADER_LENGTH];
        byteBuffer.get(header);

        SmartBoardPacket packet = new SmartBoardPacket();
        ByteBuffer headerBuffer = ByteBuffer.wrap(header).order(byteBuffer.order());
        int magicNumber = headerBuffer.getInt();
        if (magicNumber != SmartBoardPacket.MAGIC_NUMBER) {
            throw new TioDecodeException("magic number 不匹配");
        }
        packet.setMagic(magicNumber);
        packet.setVersion(headerBuffer.get());
        packet.setCmdType(headerBuffer.get());
        packet.setSeqId(headerBuffer.getShort());
        packet.setQos(headerBuffer.get());
        packet.setFlags(headerBuffer.get());
        packet.setReserved(headerBuffer.get());
        packet.setCheckSum(headerBuffer.get());
        int bodyLength = headerBuffer.getInt();
        
        // 重要：设置length字段，用于后续校验和验证
        packet.setLength(bodyLength);
        
        if (bodyLength < 0) {
            throw new TioDecodeException("消息体长度小于0");
        }
        int neededLength = SmartBoardPacket.HEADER_LENGTH + bodyLength;
        if (readableLength < neededLength) {
            return null;
        }
        if (bodyLength > 0) {
            byte[] dst = new byte[bodyLength];
            byteBuffer.get(dst);
            packet.setPayload(dst);
        }
        
        // 验证校验和（必须在设置length和payload之后）
        if (!packet.verifyCheckSum()) {
            throw new TioDecodeException("数据包校验和验证失败");
        }
        
        return packet;
    }

    @Override
    public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
        SmartBoardPacket sendPacket = (SmartBoardPacket) packet;
        byte[] body = sendPacket.getPayload() == null ? new byte[0] : sendPacket.getPayload();
        int bodyLength = body.length;
        
        // 确保length字段已设置（必须在计算校验和之前）
        if (sendPacket.getLength() == null) {
            sendPacket.setLength(bodyLength);
        } else if (sendPacket.getLength() != bodyLength) {
            // 如果length与payload长度不一致，以payload为准
            sendPacket.setLength(bodyLength);
        }
        
        // 确保校验和已计算（在设置length之后）
        if (sendPacket.getCheckSum() == null) {
            sendPacket.calculateCheckSum();
        }

        ByteBuffer buffer = ByteBuffer.allocate(SmartBoardPacket.HEADER_LENGTH + bodyLength);
        buffer.order(tioConfig.getByteOrder());

        buffer.putInt(sendPacket.getMagic());
        buffer.put(sendPacket.getVersion());
        buffer.put(sendPacket.getCmdType());
        buffer.putShort(sendPacket.getSeqId());
        buffer.put(sendPacket.getQos());
        buffer.put(sendPacket.getFlags());
        buffer.put(sendPacket.getReserved());
        buffer.put(sendPacket.getCheckSum());
        buffer.putInt(bodyLength); // 使用已设置的length值

        if (bodyLength > 0) {
            buffer.put(body);
        }
        
        // 只调用一次flip，准备读取
        buffer.flip();
        return buffer;
    }

    @Override
    public void handler(Packet packet, ChannelContext channelContext) throws Exception {
        SmartBoardPacket p = (SmartBoardPacket) packet;
        byte cmd = p.getCmdType();
        String cmdName = cmdName(cmd);
        String logMsg = String.format("[收到] cmdType=%s (0x%02X) seqId=%d qos=%s flags=0x%02X payloadLen=%d checkSum=0x%02X",
                cmdName, cmd & 0xff, p.getSeqId(), p.getQosLevel(), 
                p.getFlags() != null ? p.getFlags() : 0, 
                p.getPayload() != null ? p.getPayload().length : 0,
                p.getCheckSum() != null ? p.getCheckSum() : 0);
        System.out.println(logMsg);
    }

    private static String cmdName(byte cmd) {
        return switch (cmd) {
            case CommandType.REGISTER -> "REGISTER";
            case CommandType.REGISTER_ACK -> "REGISTER_ACK";
            case CommandType.HEARTBEAT -> "HEARTBEAT";
            case CommandType.HEARTBEAT_ACK -> "HEARTBEAT_ACK";
            case CommandType.FACE_ENROLL -> "FACE_ENROLL";
            case CommandType.FACE_ENROLL_ACK -> "FACE_ENROLL_ACK";
            case CommandType.FEATURE_UPLOAD -> "FEATURE_UPLOAD";
            case CommandType.DOOR_OPEN -> "DOOR_OPEN";
            case CommandType.DOOR_STATUS -> "DOOR_STATUS";
            case CommandType.TIMETABLE_REQ -> "TIMETABLE_REQ";
            case CommandType.TIMETABLE_RESP -> "TIMETABLE_RESP";
            case CommandType.OTA_NOTIFY -> "OTA_NOTIFY";
            case CommandType.OTA_DOWNLOAD -> "OTA_DOWNLOAD";
            case CommandType.OTA_CHUNK -> "OTA_CHUNK";
            case CommandType.OTA_PROGRESS -> "OTA_PROGRESS";
            default -> "0x" + Integer.toHexString(cmd & 0xff);
        };
    }

    @Override
    public Packet heartbeatPacket(ChannelContext channelContext) {
        var packet = new SmartBoardPacket();
        packet.setCmdType(CommandType.HEARTBEAT);
        return packet;
    }
}
