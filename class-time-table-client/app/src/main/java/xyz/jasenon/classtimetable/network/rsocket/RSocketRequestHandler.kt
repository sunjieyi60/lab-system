package xyz.jasenon.classtimetable.network.rsocket

import io.ktor.utils.io.core.readBytes
import io.rsocket.kotlin.ExperimentalMetadataApi
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.metadata.RoutingMetadata
import io.rsocket.kotlin.metadata.metadata
import io.rsocket.kotlin.payload.Payload
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import xyz.jasenon.classtimetable.network.rsocket.model.RSocketRequest
import xyz.jasenon.classtimetable.network.rsocket.model.RSocketResponse

/**
 * RSocket 请求处理器
 *
 * 封装 RSocket 的各种交互模式，提供类型安全的请求/响应处理。
 * 基于 [RSocket] 实例构建，提供高层次的业务接口。
 *
 * ## 支持的交互模式
 *
 * | 模式 | 方法 | 说明 |
 * |------|------|------|
 * | Request-Response | [requestResponse] | 发送请求，等待单个响应 |
 * | Fire-and-Forget | [fireAndForget] | 发送请求，不等待响应 |
 * | Request-Stream | [requestStream] | 发送请求，接收响应流 |
 *
 * ## 协程使用说明
 *
 * 所有请求方法都是挂起函数，需要在协程中调用：
 * - [requestResponse] - 挂起直到收到响应或超时
 * - [fireAndForget] - 挂起直到请求发出（非阻塞）
 * - [requestStream] - 返回 Flow，在收集时挂起
 *
 * ## 使用示例
 *
 * ### 请求-响应模式
 * ```kotlin
 * val handler = rsocketManager.getRequestHandler() ?: return
 *
 * lifecycleScope.launch {
 *     val response = handler.requestResponseJson("api/data", "{}")
 *     response?.let { json ->
 *         // 处理响应
 *     }
 * }
 * ```
 *
 * ### 发送即忘模式
 * ```kotlin
 * lifecycleScope.launch {
 *     val success = handler.fireAndForgetJson("api/log", "{}")
 *     if (success) {
 *         // 发送成功
 *     }
 * }
 * ```
 *
 * ### 请求-流模式
 * ```kotlin
 * lifecycleScope.launch {
 *     handler.requestStreamJson("api/events", "{}")
 *         .collect { response ->
 *             // 处理每个响应
 *         }
 * }
 * ```
 *
 * @property rsocket 底层的 RSocket 实例，为 null 时表示未连接
 * @constructor 使用 RSocket 实例创建处理器
 */
class RSocketRequestHandler(private val rsocket: RSocket?) {

    /**
     * 检查连接是否可用
     *
     * @return Boolean true 表示已连接，false 表示未连接
     */
    private fun checkConnection(): Boolean {
        return rsocket != null
    }

    /**
     * 请求-响应模式（Request-Response）
     *
     * 发送一个请求并等待单个响应。这是最常用的交互模式，
     * 适用于需要获取数据的场景，如查询课表、获取配置等。
     *
     * ## 协程行为
     * - 挂起直到收到响应
     * - 在 [Dispatchers.IO] 上执行网络操作
     * - 超时由 RSocket 的 keepAlive 配置控制
     *
     * ## 错误处理
     * - 连接断开时返回 [RSocketResponse.failure]
     * - 请求超时时抛出异常并返回失败响应
     * - 服务器错误通过响应体返回
     *
     * @param request 请求数据，包含路由和 payload
     * @return [RSocketResponse] 响应对象，包含数据或错误信息
     *
     * @sample
     * ```kotlin
     * val request = RSocketRequest(
     *     route = "api/timetable",
     *     payload = "{\"week\":1}".toByteArray()
     * )
     * val response = handler.requestResponse(request)
     * if (response.isSuccess) {
     *     val data = response.dataAsString()
     * }
     * ```
     */
    @OptIn(ExperimentalMetadataApi::class)
    suspend fun requestResponse(request: RSocketRequest): RSocketResponse {
        if (!checkConnection()) {
            return RSocketResponse.failure("RSocket 未连接")
        }

        return try {
            val payload = buildPayload {
                data(request.payload)
                metadata(RoutingMetadata(request.route))
            }

            val responsePayload = rsocket!!.requestResponse(payload)
            val responseData = responsePayload.data.readBytes()

            RSocketResponse.success(responseData)
        } catch (e: Exception) {
            RSocketResponse.failure(e.message ?: "请求失败")
        }
    }

    /**
     * 发送即忘模式（Fire-and-Forget）
     *
     * 发送请求后不等待响应，适用于不需要确认的场景：
     * - 心跳包
     * - 日志上报
     * - 状态通知
     * - 传感器数据上报
     *
     * ## 协程行为
     * - 挂起直到请求成功发出
     * - 不等待服务器处理结果
     * - 如果连接断开，立即返回 false
     *
     * ## 可靠性
     * - 网络层保证数据包发出
     * - 不保证服务器收到和处理
     * - 适合可丢失的数据
     *
     * @param request 请求数据
     * @return Boolean true 表示发送成功，false 表示发送失败
     *
     * @sample
     * ```kotlin
     * val request = RSocketRequest(
     *     route = "api/heartbeat",
     *     payload = "{}".toByteArray()
     * )
     * val success = handler.fireAndForget(request)
     * ```
     */
    @OptIn(ExperimentalMetadataApi::class)
    suspend fun fireAndForget(request: RSocketRequest): Boolean {
        if (!checkConnection()) {
            return false
        }

        return try {
            val payload = buildPayload {
                data(request.payload)
                metadata(RoutingMetadata(request.route))
            }

            rsocket!!.fireAndForget(payload)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 请求-流模式（Request-Stream）
     *
     * 发送一个请求并接收多个响应（响应流）。
     * 适用于需要持续接收数据的场景：
     * - 实时数据推送
     * - 分页数据加载
     * - 事件订阅
     *
     * ## 协程行为
     * - 返回 [Flow] 对象，不会立即挂起
     * - 在 [Flow.collect] 时挂起并接收数据
     * - 流取消时会自动关闭连接
     *
     * ## 生命周期
     * - 流的生命周期与收集协程绑定
     * - 收集协程取消时，流自动关闭
     * - RSocket 连接断开时，流以异常结束
     *
     * @param request 请求数据
     * @return [Flow]<[RSocketResponse]> 响应流，每个元素是一个响应
     *
     * @sample
     * ```kotlin
     * val request = RSocketRequest(
     *     route = "api/events",
     *     payload = "{}".toByteArray()
     * )
     * handler.requestStream(request).collect { response ->
     *     if (response.isSuccess) {
     *         // 处理每个响应
     *     }
     * }
     * ```
     */
    @OptIn(ExperimentalMetadataApi::class)
    fun requestStream(request: RSocketRequest): Flow<RSocketResponse> = flow {
        if (!checkConnection()) {
            emit(RSocketResponse.failure("RSocket 未连接"))
            return@flow
        }

        try {
            val payload = buildPayload {
                data(request.payload)
                metadata(RoutingMetadata(request.route))
            }

            rsocket!!.requestStream(payload).collect { responsePayload ->
                val responseData = responsePayload.data.readBytes()
                emit(RSocketResponse.success(responseData))
            }
        } catch (e: Exception) {
            emit(RSocketResponse.failure(e.message ?: "流请求失败"))
        }
    }

    /**
     * 请求-响应模式（JSON 便捷方法）
     *
     * 简化版本的 [requestResponse]，直接处理 JSON 字符串。
     * 自动将 JSON 字符串转换为 [RSocketRequest]。
     *
     * @param route 路由路径，如 "api/timetable"
     * @param jsonPayload JSON 格式的请求体，默认为 "{}"
     * @return String? JSON 格式的响应字符串，失败返回 null
     *
     * @sample
     * ```kotlin
     * lifecycleScope.launch {
     *     val json = handler.requestResponseJson("api/timetable", "{\"week\":1}")
     *     json?.let {
     *         val courses = parseCourses(it)
     *         // 更新 UI
     *     }
     * }
     * ```
     */
    suspend fun requestResponseJson(route: String, jsonPayload: String = "{}"): String? {
        val request = RSocketRequest.fromJson(route, jsonPayload)
        val response = requestResponse(request)
        return if (response.isSuccess) {
            response.dataAsString()
        } else {
            null
        }
    }

    /**
     * 发送即忘模式（JSON 便捷方法）
     *
     * 简化版本的 [fireAndForget]，直接处理 JSON 字符串。
     * 自动将 JSON 字符串转换为 [RSocketRequest]。
     *
     * @param route 路由路径，如 "api/heartbeat"
     * @param jsonPayload JSON 格式的请求体，默认为 "{}"
     * @return Boolean true 表示发送成功
     *
     * @sample
     * ```kotlin
     * lifecycleScope.launch {
     *     val success = handler.fireAndForgetJson("api/log", "{\"level\":\"INFO\"}")
     * }
     * ```
     */
    suspend fun fireAndForgetJson(route: String, jsonPayload: String = "{}"): Boolean {
        val request = RSocketRequest.fromJson(route, jsonPayload)
        return fireAndForget(request)
    }

    /**
     * 请求-流模式（JSON 便捷方法）
     *
     * 简化版本的 [requestStream]，直接处理 JSON 字符串。
     * 自动将 JSON 字符串转换为 [RSocketRequest]。
     *
     * @param route 路由路径，如 "api/events"
     * @param jsonPayload JSON 格式的请求体，默认为 "{}"
     * @return [Flow]<[RSocketResponse]> 响应流
     *
     * @sample
     * ```kotlin
     * lifecycleScope.launch {
     *     handler.requestStreamJson("api/notifications", "{}")
     *         .collect { response ->
     *             response.dataAsString()?.let { json ->
     *                 // 处理每个通知
     *             }
     *         }
     * }
     * ```
     */
    fun requestStreamJson(route: String, jsonPayload: String = "{}"): Flow<RSocketResponse> {
        val request = RSocketRequest.fromJson(route, jsonPayload)
        return requestStream(request)
    }
}
