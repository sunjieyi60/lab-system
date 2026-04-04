package xyz.jasenon.classtimetable.network

import android.util.Log
import org.tio.client.intf.TioClientHandler
import org.tio.core.ChannelContext
import org.tio.core.TioConfig
import org.tio.core.intf.Packet
import xyz.jasenon.classtimetable.network.handler.DoorStatusPacketHandler
import xyz.jasenon.classtimetable.network.handler.PacketHandlerRegistry
import xyz.jasenon.classtimetable.network.handler.TimetablePacketHandler
import xyz.jasenon.classtimetable.protocol.CommandType
import xyz.jasenon.classtimetable.protocol.PacketFlags
import xyz.jasenon.classtimetable.protocol.QosLevel
import xyz.jasenon.classtimetable.protocol.SeqIdGenerator
import xyz.jasenon.classtimetable.protocol.SmartBoardPacket
import java.nio.ByteBuffer

class SmartBoardTioHandler : TioClientHandler {

    /**
     * 编码：将 SmartBoardPacket 转换为 ByteBuffer
     */
    override fun encode(packet: Packet,tioConfig: TioConfig, context: ChannelContext): ByteBuffer {
        val smartPacket = packet as SmartBoardPacket
        val body = smartPacket.payload ?: ByteArray(0)
        val bodyLength = body.size

        // 创建一个ByteBuffer，大小为头部长度 + 消息体长度
        val buffer = ByteBuffer.allocate(SmartBoardPacket.HEADER_LENGTH + bodyLength)
        buffer.order(tioConfig.byteOrder)

        // 写入头部（可空字段用默认值，避免心跳包等未设全头部时 NPE）
        buffer.putInt(smartPacket.magic ?: SmartBoardPacket.MAGIC_NUMBER)
        buffer.put(smartPacket.version ?: 0x01.toByte())
        buffer.put(smartPacket.cmdType ?: 0.toByte())
        buffer.putShort(smartPacket.seqId ?: 0)
        buffer.put(smartPacket.qos ?: QosLevel.AT_MOST_ONCE.value)
        buffer.put(smartPacket.flags ?: PacketFlags.NONE)
        buffer.put(smartPacket.reserved ?: 0.toByte())
        buffer.put(smartPacket.checkSum ?: 0.toByte())
        buffer.putInt(bodyLength) // 写入真实的消息体长度

        // 写入消息体
        if (bodyLength > 0) {
            buffer.put(body)
        }

        buffer.flip()
        return buffer
    }

    /**
     * 解码：从 ByteBuffer 解析出 SmartBoardPacket
     */
    override fun decode(buffer: ByteBuffer, limit: Int, position: Int, readableLength: Int, context: ChannelContext): SmartBoardPacket? {
        if (readableLength < SmartBoardPacket.HEADER_LENGTH) {
            return null // 数据包长度不足，等待更多数据
        }

        val magic = buffer.getInt(position)
        if (magic != SmartBoardPacket.MAGIC_NUMBER) {
            throw Exception("Invalid magic number")
        }

        // 读取头部
        buffer.position(position)
        val header = ByteArray(SmartBoardPacket.HEADER_LENGTH)
        buffer.get(header)

        val resultPacket = SmartBoardPacket()
        val headerBuffer = ByteBuffer.wrap(header).order(buffer.order())
        resultPacket.magic = headerBuffer.getInt()
        resultPacket.version = headerBuffer.get()
        resultPacket.cmdType = headerBuffer.get()
        resultPacket.seqId = headerBuffer.getShort()
        resultPacket.qos = headerBuffer.get()
        resultPacket.flags = headerBuffer.get()
        resultPacket.reserved = headerBuffer.get()
        resultPacket.checkSum = headerBuffer.get()
        val bodyLength = headerBuffer.getInt()

        // 检查消息体是否完整
        if (readableLength < SmartBoardPacket.HEADER_LENGTH + bodyLength) {
            return null // 消息体不完整，等待更多数据
        }

        // 读取消息体
        if (bodyLength > 0) {
            val body = ByteArray(bodyLength)
            buffer.get(body)
            resultPacket.payload = body
        }

        return resultPacket
    }

    init {
        PacketHandlerRegistry.register(CommandType.TIMETABLE_RESP, TimetablePacketHandler())
        PacketHandlerRegistry.register(CommandType.DOOR_STATUS, DoorStatusPacketHandler())
    }

    /**
     * 消息处理：按 cmdType 分发到对应 Handler，Handler 通过 RemoteDataObservable 通知观察者更新
     */
    override fun handler(packet: Packet, context: ChannelContext) {
        val smartPacket = packet as SmartBoardPacket
        val handled = PacketHandlerRegistry.dispatch(smartPacket)
        if (!handled) {
            Log.d("SmartBoardTioHandler", "No handler for cmdType=${smartPacket.cmdType}")
        }
    }

    override fun heartbeatPacket(context: ChannelContext): Packet {
        Log.d("SmartBoardTioHandler", "发送心跳包")
        val heartbeatPacket = SmartBoardPacket()
        heartbeatPacket.setCmdType(CommandType.HEARTBEAT)
        heartbeatPacket.setVersion(0x01.toByte())
        heartbeatPacket.setSeqId(SeqIdGenerator().nextSeqId())
        heartbeatPacket.setQos(QosLevel.AT_MOST_ONCE.value)
        heartbeatPacket.setFlags(PacketFlags.NONE)
        heartbeatPacket.setReserved(0.toByte())
        heartbeatPacket.setPayload(null)
        heartbeatPacket.setLength(0)
        heartbeatPacket.calculateCheckSum()
        return heartbeatPacket
    }
}