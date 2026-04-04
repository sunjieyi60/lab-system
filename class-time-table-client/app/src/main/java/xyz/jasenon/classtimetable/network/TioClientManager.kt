package xyz.jasenon.classtimetable.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tio.client.ClientChannelContext
import org.tio.client.ReconnConf
import org.tio.client.TioClient
import org.tio.client.TioClientConfig
import org.tio.core.Node
import org.tio.core.Tio
import xyz.jasenon.classtimetable.config.ConfigObservable
import xyz.jasenon.classtimetable.network.handler.FaceEnrollPacketHandler
import xyz.jasenon.classtimetable.network.handler.PacketHandlerRegistry
import xyz.jasenon.classtimetable.network.handler.RegisterAckPacketHandler
import xyz.jasenon.classtimetable.network.register.RegisterPayloadBuilder
import xyz.jasenon.classtimetable.protocol.CheckSumCalculator
import xyz.jasenon.classtimetable.protocol.CommandType
import xyz.jasenon.classtimetable.protocol.PacketBuilder
import xyz.jasenon.classtimetable.protocol.PacketFlags
import xyz.jasenon.classtimetable.protocol.QosLevel
import xyz.jasenon.classtimetable.protocol.SeqIdGenerator
import xyz.jasenon.classtimetable.protocol.SmartBoardPacket

/**
 * T-io 客户端管理
 *
 * 约定：在兜底配置载入并推送 Observer 之后调用 [getInstance]；
 * 使用 [ConfigObservable.getCurrent] 获取配置（host/port/timeout/heartPeriod），连接后发送 REGISTER 协商 AES 密钥；
 * 收到 REGISTER_ACK 且 payload 非空时由 [RegisterAckPacketHandler] 解密并写入配置、更新 Observer。
 */
class TioClientManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val handler = SmartBoardTioHandler()
    private val listener = SmartBoardTioListener()
    private val appConfig = ConfigObservable.getCurrent()
        ?: throw IllegalStateException("请先调用 AppConfigManager.loadFallbackAndPush() 完成兜底配置")
    private val tioClientConfig: TioClientConfig
    private var tioClient: TioClient? = null
    private var channelContext: ClientChannelContext? = null

    init {
        PacketHandlerRegistry.register(CommandType.REGISTER_ACK, RegisterAckPacketHandler(appContext))
        PacketHandlerRegistry.register(CommandType.FACE_SEND, FaceEnrollPacketHandler(appContext))
        val reconnConf = ReconnConf(appConfig.backendServer.timeout!!)
        this.tioClientConfig = TioClientConfig(handler, listener, reconnConf)
        tioClientConfig.setHeartbeatTimeout(appConfig.backendServer.heartPeriod!!)
        this.tioClient = TioClient(tioClientConfig)
        connect()
    }

    private fun connect() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val node = Node(appConfig.backendServer.host, appConfig.backendServer.port!!)
                channelContext = tioClient?.connect(node)
                register()
                Log.d("TioClientManager", "Tio client connected and registered.")
            } catch (e: Exception) {
                Log.e("TioClientManager", "Tio client connection failed.", e)
            }
        }
    }

    @Throws(Exception::class)
    private fun register() {
        val payload = RegisterPayloadBuilder.buildPayload(appContext, appConfig)
        if (payload == null) {
            Log.e("TioClientManager", "Build register payload failed (check server_public_key in assets)")
            return
        }
//        val packet = SmartBoardPacket()
//        packet.setCmdType(CommandType.REGISTER)
//        packet.setVersion(0x01.toByte())
//        packet.setSeqId(SeqIdGenerator().nextSeqId())
//        packet.setQos(QosLevel.AT_LEAST_ONCE.value)
//        packet.setFlags(PacketFlags.NONE)
//        packet.setReserved(0.toByte())
//        packet.setPayload(payload)
//        packet.setLength(payload.size)

        val packet = PacketBuilder().build(CommandType.REGISTER, payload)

        CheckSumCalculator.setCheckSum(packet)
        Tio.send(channelContext, packet)
        Log.d("TioClientManager", "REGISTER sent, payload length=${payload.size}")
    }

    /**
     * 发送数据包到 Server（供 Handler 回复 FACE_SEND_ACK 等）
     */
    fun sendPacket(packet: SmartBoardPacket) {
        val ctx = channelContext ?: run {
            Log.w("TioClientManager", "sendPacket: channelContext 为空，未连接")
            return
        }
        try {
            CheckSumCalculator.setCheckSum(packet)
            Tio.send(ctx, packet)
        } catch (e: Exception) {
            Log.e("TioClientManager", "sendPacket 失败", e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: TioClientManager? = null

        @JvmStatic
        fun getInstance(context: Context): TioClientManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TioClientManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
