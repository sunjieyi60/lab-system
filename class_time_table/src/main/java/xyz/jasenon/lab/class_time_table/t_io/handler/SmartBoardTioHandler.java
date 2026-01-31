package xyz.jasenon.lab.class_time_table.t_io.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.server.intf.TioServerHandler;
import xyz.jasenon.lab.class_time_table.t_io.protocol.SmartBoardPacket;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Component
@Slf4j
public class SmartBoardTioHandler implements TioServerHandler {

    @Override
    public Packet decode(ByteBuffer byteBuffer, int limit, int position, int readableLength, ChannelContext channelContext) throws TioDecodeException {
        if (readableLength < SmartBoardPacket.HEADER_LENGTH) {
            // 收到不完整的包，直接返回null 交由框架处理
            return null;
        }

        // 必须从 position 开始读，否则可能读到错误偏移（框架可能未设置 buffer.position）
        byteBuffer.position(position);

        var header = new byte[SmartBoardPacket.HEADER_LENGTH];
        byteBuffer.get(header);

        SmartBoardPacket packet = new SmartBoardPacket();
        var headerBuffer = ByteBuffer.wrap(header).order(byteBuffer.order());
        int magicNumber = headerBuffer.getInt();
        if (magicNumber != SmartBoardPacket.MAGIC_NUMBER) {
            throw new TioDecodeException("magic number 不匹配" + "来自channel:" + channelContext.getClientNode());
        }
        packet.setMagic(magicNumber);
        packet.setVersion(headerBuffer.get());
        packet.setCmdType(headerBuffer.get());
        packet.setSeqId(headerBuffer.getShort());
        packet.setQos(headerBuffer.get());
        packet.setFlags(headerBuffer.get());
        packet.setReserved(headerBuffer.get());
        packet.setCheckSum(headerBuffer.get());
        // 消息体长度在 16 字节头内最后 4 字节，从 header 解析，不要再用 byteBuffer.getInt()（会读到 body 前 4 字节）
        Integer bodyLength = headerBuffer.getInt();

        if (bodyLength < 0) {
            throw new TioDecodeException("消息体长度小于0" + "来自channel:" + channelContext.getClientNode());
        }

        // 校验消息体是否完整
        int neededLength = SmartBoardPacket.HEADER_LENGTH + bodyLength;
        if (readableLength < neededLength) {
            return null;
        }

        if (bodyLength > 0) {
            byte[] dst = new byte[bodyLength];
            byteBuffer.get(dst);
            packet.setPayload(dst);
            log.info("收到来自channel:{}的消息，cmdType={}, seqId={}, payload={}", channelContext.getClientNode(), packet.getCmdType(), packet.getSeqId(),
                    new String(dst, StandardCharsets.UTF_8));
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
        buffer.flip();
        return buffer;
    }

    @Override
    public void handler(Packet packet, ChannelContext channelContext) throws Exception {
        var acceptPacket = (SmartBoardPacket) packet;
        var cmdType = acceptPacket.getCmdType();
        // TODO: 处理业务逻辑
    }
}
