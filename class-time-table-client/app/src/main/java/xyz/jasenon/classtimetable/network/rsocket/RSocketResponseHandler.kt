package xyz.jasenon.classtimetable.network.rsocket

import com.elvishew.xlog.XLog
import io.ktor.utils.io.core.readBytes
import io.rsocket.kotlin.ExperimentalMetadataApi
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.metadata.RoutingMetadata
import io.rsocket.kotlin.metadata.read
import io.rsocket.kotlin.payload.Payload
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import xyz.jasenon.classtimetable.network.Const
import xyz.jasenon.classtimetable.network.rsocket.model.RSocketResponse
import kotlin.coroutines.CoroutineContext

/**
 * RSocket 响应处理器（Server 端实现）
 *
 * 在 CS 对等架构中，Android 设备既可以作为 Client 发起请求，也可以作为 Server 接收对端（服务端）主动下发的请求。
 * 本类实现 [RSocket] 接口，处理对端发来的各种请求并分发到对应的处理器。
 *
 * ## 处理的请求类型
 *
 * | 路由 | 说明 | 处理逻辑 |
 * |------|------|----------|
 * | [Const.Route.DEVICE_CONFIG_UPDATE] | 服务器推送设备配置更新 | 更新设备运行时配置 |
 * | [Const.Route.DEVICE_FACE_UPDATE] | 服务器推送人脸信息更新 | 更新本地人脸库 |
 * | [Const.Route.DEVICE_SCHEDULE_UPDATE] | 服务器推送课表更新 | 刷新课程表数据 |
 * | [Const.Route.DEVICE_DOOR_OPEN] | 服务器下发开门指令 | 执行开门操作 |
 * | [Const.Route.DEVICE_REBOOT] | 服务器下发重启指令 | 执行设备重启 |
 * | [Const.Route.DEVICE_COMMAND] | 通用命令下发 | 根据命令类型分发处理 |
 *
 * ## 使用方式
 *
 * 在建立 RSocket 连接时，将此处理器作为 Server 侧的接受器：
 * ```kotlin
 * val responseHandler = RSocketResponseHandler()
 * // 在连接建立时作为接受器传入
 * ```
 *
 * @author Jasenon_ce
 * @since 1.0.0
 */
@OptIn(ExperimentalMetadataApi::class)
class RSocketResponseHandler(
    override val coroutineContext: CoroutineContext) : RSocket {

    companion object {
        private const val TAG = "RSocketResponseHandler"
    }

    /**
     * 请求处理器注册表
     * 路由 -> 处理器的映射
     */
    private val handlers = mutableMapOf<String, suspend (ByteArray) -> RSocketResponse>()

    /**
     * 注册请求处理器
     *
     * @param route 路由路径
     * @param handler 处理函数，接收请求字节数组，返回 [RSocketResponse]
     */
    fun registerHandler(route: String, handler: suspend (ByteArray) -> RSocketResponse) {
        handlers[route] = handler
        XLog.tag(TAG).d("注册处理器: $route")
    }

    /**
     * 注销请求处理器
     *
     * @param route 路由路径
     */
    fun unregisterHandler(route: String) {
        handlers.remove(route)
        XLog.tag(TAG).d("注销处理器: $route")
    }

    /**
     * 处理 Request-Response 模式的请求
     *
     * 这是 Server 端最核心的方法，用于处理对端发来的请求并返回响应。
     * 根据请求的 metadata 中的路由信息，分发到对应的处理器。
     *
     * @param payload 请求负载，包含 data 和 metadata
     * @return Payload 响应负载
     */
    override suspend fun requestResponse(payload: Payload): Payload {
        val route = extractRoute(payload)
        val data = payload.data.readBytes()

        XLog.tag(TAG).d("收到请求 [$route], 数据大小: ${data.size} bytes")

        val handler = handlers[route]
        return if (handler != null) {
            try {
                val response = handler(data)
                buildResponsePayload(response)
            } catch (e: Exception) {
                XLog.tag(TAG).e("处理请求 [$route] 时发生异常", e)
                buildErrorPayload("处理请求失败: ${e.message}")
            }
        } else {
            XLog.tag(TAG).w("未找到 [$route] 的处理器")
            buildErrorPayload("Unknown route: $route")
        }
    }

    /**
     * 处理 Fire-and-Forget 模式的请求
     *
     * 对端发送请求后不期待响应，适用于心跳、日志上报等场景。
     *
     * @param payload 请求负载
     */
    override suspend fun fireAndForget(payload: Payload) {
        val route = extractRoute(payload)
        val data = payload.data.readBytes()

        XLog.tag(TAG).d("收到 Fire-and-Forget 请求 [$route], 数据大小: ${data.size} bytes")

        val handler = handlers[route]
        if (handler != null) {
            try {
                handler(data)
            } catch (e: Exception) {
                XLog.tag(TAG).e("处理 Fire-and-Forget 请求 [$route] 时发生异常", e)
            }
        } else {
            XLog.tag(TAG).w("未找到 [$route] 的处理器，忽略该请求")
        }
    }

    /**
     * 处理 Request-Stream 模式的请求
     *
     * 对端发送一个请求，期待接收多个响应（流）。
     * 适用于实时数据推送、分页加载等场景。
     *
     * @param payload 请求负载
     * @return Flow<Payload> 响应流
     */
    override fun requestStream(payload: Payload): Flow<Payload> {
        val route = extractRoute(payload)
        XLog.tag(TAG).d("收到 Request-Stream 请求 [$route]")

        // 目前项目中暂不支持 Server 端的 Stream 响应
        // 如有需要，可扩展 handlers 返回 Flow 类型
        XLog.tag(TAG).w("Server 端暂不支持 Request-Stream 模式 [$route]")
        return emptyFlow()
    }

    /**
     * 处理 Request-Channel 模式的请求
     *
     * 双向流模式，双方都可以发送和接收数据。
     * 适用于实时双向通信场景。
     *
     * @param payloads 输入流
     * @return Flow<Payload> 输出流
     */
    override fun requestChannel(initPayload: Payload, payloads: Flow<Payload>): Flow<Payload> {
        XLog.tag(TAG).d("收到 Request-Channel 请求")
        // 目前项目中暂不支持 Server 端的 Channel 模式
        XLog.tag(TAG).w("Server 端暂不支持 Request-Channel 模式")
        return emptyFlow()
    }

    /**
     * 提取路由信息
     *
     * 从 Payload 的 metadata 中解析出路由路径。
     *
     * @param payload 请求负载
     * @return String 路由路径，如果未找到则返回 "unknown"
     */
    private fun extractRoute(payload: Payload): String {
        return try {
            payload.metadata?.let { metadata ->
                val routingMetadata = metadata.read(RoutingMetadata)
                routingMetadata.tags.firstOrNull() ?: "unknown"
            } ?: "unknown"
        } catch (e: Exception) {
            XLog.tag(TAG).w("提取路由信息失败: ${e.message}")
            "unknown"
        }
    }

    /**
     * 构建响应 Payload
     *
     * 将 [RSocketResponse] 转换为 RSocket 的 Payload。
     *
     * @param response 响应对象
     * @return Payload RSocket 负载
     */
    private fun buildResponsePayload(response: RSocketResponse): Payload {
        return buildPayload {
            data(response.data ?: byteArrayOf())
        }
    }

    /**
     * 构建错误响应 Payload
     *
     * @param errorMessage 错误信息
     * @return Payload 包含错误信息的响应
     */
    private fun buildErrorPayload(errorMessage: String): Payload {
        return buildPayload {
            val errorJson = """{"success":false,"error":"$errorMessage"}"""
            data(errorJson.toByteArray(Charsets.UTF_8))
        }
    }

    /**
     * 便捷方法：注册配置更新处理器
     *
     * @param handler 处理函数，接收配置 JSON 数据
     */
    fun onConfigUpdate(handler: suspend (ByteArray) -> RSocketResponse) {
        registerHandler(Const.Route.DEVICE_CONFIG_UPDATE, handler)
    }

    /**
     * 便捷方法：注册人脸更新处理器
     *
     * @param handler 处理函数，接收人脸数据
     */
    fun onFaceUpdate(handler: suspend (ByteArray) -> RSocketResponse) {
        registerHandler(Const.Route.DEVICE_FACE_UPDATE, handler)
    }

    /**
     * 便捷方法：注册课表更新处理器
     *
     * @param handler 处理函数，接收课表数据
     */
    fun onScheduleUpdate(handler: suspend (ByteArray) -> RSocketResponse) {
        registerHandler(Const.Route.DEVICE_SCHEDULE_UPDATE, handler)
    }

    /**
     * 便捷方法：注册开门指令处理器
     *
     * @param handler 处理函数
     */
    fun onDoorOpen(handler: suspend (ByteArray) -> RSocketResponse) {
        registerHandler(Const.Route.DEVICE_DOOR_OPEN, handler)
    }

    /**
     * 便捷方法：注册重启指令处理器
     *
     * @param handler 处理函数
     */
    fun onReboot(handler: suspend (ByteArray) -> RSocketResponse) {
        registerHandler(Const.Route.DEVICE_REBOOT, handler)
    }

    /**
     * 便捷方法：注册通用命令处理器
     *
     * @param handler 处理函数
     */
    fun onCommand(handler: suspend (ByteArray) -> RSocketResponse) {
        registerHandler(Const.Route.DEVICE_COMMAND, handler)
    }
}
