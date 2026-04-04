package xyz.jasenon.classtimetable.network.handler

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import xyz.jasenon.classtimetable.config.AesConfigHelper
import xyz.jasenon.classtimetable.config.AppConfig
import xyz.jasenon.classtimetable.config.AppConfigManager
import xyz.jasenon.classtimetable.protocol.CommandType
import xyz.jasenon.classtimetable.protocol.SmartBoardPacket

/**
 * REGISTER_ACK 包处理器
 *
 * 服务器验证 REGISTER 后回复 REGISTER_ACK；
 * payload = IV(12) + AES-GCM(配置 JSON)。解密后按 [AppConfig] 结构反序列化，
 * 直接写入兜底本地 JSON 并更新 [ConfigObservable]（与后端 DeviceConfigDTO 结构一致）。
 */
class RegisterAckPacketHandler(private val context: Context) : PacketHandler {

    private val gson = Gson()
    private val configManager by lazy { AppConfigManager(context.applicationContext) }

    override fun handle(packet: SmartBoardPacket): Boolean {
        if (packet.cmdType != CommandType.REGISTER_ACK) return false
        val payload = packet.payload ?: return true
        if (payload.size <= 12) return true

        val json = AesConfigHelper.decryptToJson(payload)
            ?: run {
                Log.e(TAG, "REGISTER_ACK payload 解密失败")
                return true
            }
        return try {
            val config = gson.fromJson(json, AppConfig::class.java)
            configManager.saveConfigFromServer(config)
            Log.d(TAG, "REGISTER_ACK 配置已解密并写入兜底 JSON，已更新 Observer，deviceId=${config.deviceId}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "REGISTER_ACK 反序列化配置失败", e)
            true
        }
    }

    companion object {
        private const val TAG = "RegisterAckPacketHandler"
    }
}
