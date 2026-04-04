package xyz.jasenon.classtimetable.network.handler

import xyz.jasenon.classtimetable.protocol.CommandType
import xyz.jasenon.classtimetable.protocol.SmartBoardPacket

/**
 * 按 cmdType 注册并分发 PacketHandler
 */
object PacketHandlerRegistry {

    private val handlers = mutableMapOf<Byte, PacketHandler>()

    fun register(cmdType: Byte, handler: PacketHandler) {
        handlers[cmdType] = handler
    }

    fun unregister(cmdType: Byte) {
        handlers.remove(cmdType)
    }

    /**
     * 根据包类型分发到对应 Handler，Handler 内部通过 RemoteDataObservable 通知观察者
     */
    fun dispatch(packet: SmartBoardPacket): Boolean {
        val cmdType = packet.cmdType ?: return false
        val handler = handlers[cmdType] ?: return false
        return handler.handle(packet)
    }
}
