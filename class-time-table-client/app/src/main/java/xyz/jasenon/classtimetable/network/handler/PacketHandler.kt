package xyz.jasenon.classtimetable.network.handler

import xyz.jasenon.classtimetable.protocol.SmartBoardPacket

/**
 * 数据包业务处理器接口
 *
 * TioClientHandler 根据 cmdType 将 SmartBoardPacket 分发到对应 Handler，
 * Handler 解析 payload 后通过 RemoteDataObservable 更新数据，实现观察者模式下的页面刷新。
 */
fun interface PacketHandler {
    /**
     * 处理数据包
     * @return true 表示已处理，false 表示未处理（可交给下一个 handler）
     */
    fun handle(packet: SmartBoardPacket): Boolean
}
