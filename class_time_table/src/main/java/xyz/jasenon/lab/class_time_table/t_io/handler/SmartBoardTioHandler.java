package xyz.jasenon.lab.class_time_table.t_io.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.server.intf.TioServerHandler;
import org.tio.core.Tio;
import xyz.jasenon.lab.class_time_table.t_io.protocol.CommandType;
import xyz.jasenon.lab.class_time_table.t_io.protocol.SmartBoardPacket;
import xyz.jasenon.lab.class_time_table.t_io.service.RegisterHandler;
import xyz.jasenon.lab.class_time_table.service.DeviceRegisterService;

import java.nio.ByteBuffer;

/**
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SmartBoardTioHandler implements TioServerHandler {
    
    private final RegisterHandler registerHandler;
    private final DeviceRegisterService deviceRegisterService;

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
        int bodyLength = headerBuffer.getInt();
        
        // 重要：设置length字段，用于后续校验和验证
        packet.setLength(bodyLength);

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
        }
        
        // 验证校验和（必须在设置length和payload之后）
        if (!packet.verifyCheckSum()) {
            log.warn("收到来自channel:{}的数据包校验和验证失败，cmdType={}, seqId={}, length={}, checkSum={}",
                    channelContext.getClientNode(), packet.getCmdType(), packet.getSeqId(), 
                    packet.getLength(), packet.getCheckSum() != null ? packet.getCheckSum() : 0);
            throw new TioDecodeException("数据包校验和验证失败，来自channel:" + channelContext.getClientNode());
        }
        
        log.debug("收到来自channel:{}的消息，cmdType={}, seqId={}, qos={}, flags={}, checkSum={}, length={}",
                channelContext.getClientNode(), packet.getCmdType(), packet.getSeqId(), 
                packet.getQos(), packet.getFlags(), 
                packet.getCheckSum() != null ? packet.getCheckSum() : 0, packet.getLength());

        return packet;
    }

    @Override
    public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
        var sendPacket = (SmartBoardPacket) packet;
        var body = sendPacket.getPayload() == null ? new byte[0] : sendPacket.getPayload();
        var bodyLength = body.length;
        
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
        var acceptPacket = (SmartBoardPacket) packet;
        var cmdType = acceptPacket.getCmdType();
        
        // REGISTER指令不需要验证（正在注册）
        if (cmdType == CommandType.REGISTER) {
            registerHandler.handleRegister(acceptPacket, channelContext);
            return;
        }
        
        // 其他指令需要验证设备合法性（通过ChannelContext的bindId）
        String deviceId = deviceRegisterService.verifyDevice(channelContext);
        if (deviceId == null) {
            log.warn("收到来自未注册设备的请求，cmdType={}, 来自: {}", cmdType, channelContext.getClientNode());
            Tio.close(channelContext, "设备未注册，连接已关闭");
            return;
        }
        
        // 根据指令类型分发到不同的处理器
        log.debug("收到设备{}的指令，cmdType={}", deviceId, cmdType);
        // 其他指令类型的处理将在后续实现
    }
}
