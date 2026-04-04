package xyz.jasenon.classtimetable.network.handler

import android.util.Log
import xyz.jasenon.classtimetable.network.observe.DoorStatusData
import xyz.jasenon.classtimetable.network.observe.RemoteDataObservable
import xyz.jasenon.classtimetable.protocol.CommandType
import xyz.jasenon.classtimetable.protocol.SmartBoardPacket

/**
 * 门禁状态包处理器
 * 解析 DOOR_STATUS 等 payload，通过 RemoteDataObservable 更新门状态，观察者自动刷新。
 */
class DoorStatusPacketHandler : PacketHandler {

    override fun handle(packet: SmartBoardPacket): Boolean {
        if (packet.cmdType != CommandType.DOOR_STATUS) return false
        val payload = packet.payload
        val data = if (payload != null && payload.isNotEmpty()) {
            try {
                val json = String(payload, Charsets.UTF_8)
                org.json.JSONObject(json).let { obj ->
                    DoorStatusData(
                        open = obj.optBoolean("open", false),
                        message = obj.optString("message").takeIf { it.isNotEmpty() }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Parse door status failed", e)
                null
            }
        } else {
            DoorStatusData(open = false, message = null)
        }
        RemoteDataObservable.updateDoorStatus(data)
        Log.d(TAG, "Door status updated: $data")
        return true
    }

    companion object {
        private const val TAG = "DoorStatusPacketHandler"
    }
}
