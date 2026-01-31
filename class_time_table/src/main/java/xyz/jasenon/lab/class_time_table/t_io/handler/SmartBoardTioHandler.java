package xyz.jasenon.lab.class_time_table.t_io.handler;

import org.springframework.stereotype.Component;
import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.server.intf.TioServerHandler;
import xyz.jasenon.lab.class_time_table.t_io.protocol.SmartBoardPacket;

import java.nio.ByteBuffer;

/**
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Component
public class SmartBoardTioHandler implements TioServerHandler {

    @Override
    public Packet decode(ByteBuffer byteBuffer, int limit, int position, int readableLength, ChannelContext channelContext) throws TioDecodeException {
        if (readableLength < SmartBoardPacket.HEADER_LENGTH) {
            // 收到不完整的包，直接返回null 交由框架处理
            return null;
        }

        var magicNumber = byteBuffer.getInt(position);
        if (magicNumber != SmartBoardPacket.MAGIC_NUMBER) {
            throw new TioDecodeException("magic number 不匹配" + "来自channel:" + channelContext.getClientNode());
        }

        var header = new byte[SmartBoardPacket.HEADER_LENGTH];
        byteBuffer.get(header);

        // 接收到的消息体长度
        Integer bodyLength = byteBuffer.getInt();

        if (bodyLength < 0){
            throw new TioDecodeException("消息体长度小于0" + "来自channel:" + channelContext.getClientNode());
        }



        // 校验消息体长度
        Integer neededLength = SmartBoardPacket.HEADER_LENGTH + bodyLength;
        Integer isEnough = readableLength - neededLength;
        if (isEnough < 0){
            // 消息体不完整，直接返回null
            return null;
        }

        SmartBoardPacket packet = new SmartBoardPacket();
        if (bodyLength > 0){
            byte[] dst = new byte[bodyLength];
            byteBuffer.get(dst);
            packet.setPayload(dst);
        }
        return packet;
    }

    @Override
    public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
        var sendPacket = (SmartBoardPacket) packet;
        var body = sendPacket.getPayload() == null ? new byte[0] : sendPacket.getPayload();
        var bodyLength = body.length;

        var buffer = ByteBuffer.allocate(SmartBoardPacket.HEADER_LENGTH + bodyLength);
        buffer.order(tioConfig.getByteOrder());

        buffer.putInt(sendPacket.getMagic());
        buffer.put(sendPacket.getVersion());
        buffer.put(sendPacket.getCmdType());
        buffer.putShort(sendPacket.getSeqId());
        buffer.put(sendPacket.getQos());
        buffer.put(sendPacket.getFlags());
        buffer.put(sendPacket.getReserved());
        buffer.put(sendPacket.getCheckSum());
        buffer.putInt(bodyLength);

        if (bodyLength > 0) {
            buffer.put(body);
        }

        return buffer;
    }

    @Override
    public void handler(Packet packet, ChannelContext channelContext) throws Exception {

    }
}
