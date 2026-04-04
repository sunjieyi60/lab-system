package xyz.jasenon.classtimetable.network.rsocket

import android.content.Context
import android.util.Log
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.core.RSocketConnector
import io.rsocket.kotlin.keepalive.KeepAlive
import io.rsocket.kotlin.payload.PayloadMimeType
import io.rsocket.kotlin.transport.ktor.tcp.KtorTcpClientTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.jasenon.classtimetable.config.ConfigObservable
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

/**
 * RSocket 客户端管理器
 *
 * 基于 Ktor TCP Transport 实现的 RSocket 客户端单例管理器，负责：
 * - 管理 TCP 连接生命周期（建立、断开、重连）
 * - 使用 RSocket 内置重连机制实现自动重连
 * - 提供连接状态监控（[connectionState] StateFlow）
 * - 提供请求/响应（Request-Response）、发送即忘（Fire-and-Forget）等交互模式
 *
 * ## 协程使用说明
 *
 * 本类使用两个 CoroutineScope 管理协程生命周期：
 * - [managerScope]: 管理器级别 Scope，用于配置监听，生命周期与单例相同
 * - [connectionScope]: 连接级别 Scope，每次 [connect] 创建，[disconnect] 取消
 *
 * **重要**：RSocket 连接绑定到 [connectionScope]，当 Scope 取消时，连接自动断开。
 *
 * ## 内置重连机制
 *
 * 使用 [RSocketConnector.reconnectable] 实现自动重连：
 * - 连接失败后自动重试
 * - 可通过 [connect] 的 maxRetries 参数设置最大重试次数
 * - 每次重试间隔 [reconnectDelayMs]（默认 5 秒）
 *
 * **注意**：内置重连仅重新建立连接，已存在的流（Stream）会失败。
 * 如需流恢复，需要服务器支持 RSocket Resumption 协议。
 *
 * ## 使用示例
 *
 * ### 1. 初始化并连接
 * ```kotlin
 * class MainActivity : ComponentActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         // 获取单例实例
 *         val rsocketManager = RSocketClientManager.getInstance(this)
 *
 *         // 启动连接（协程中调用）
 *         lifecycleScope.launch {
 *             rsocketManager.connect() // 使用配置中的 host/port
 *             // 或指定参数
 *             // rsocketManager.connect("192.168.1.100", 9000, maxRetries = 5)
 *         }
 *     }
 * }
 * ```
 *
 * ### 2. 观察连接状态
 * ```kotlin
 * lifecycleScope.launch {
 *     rsocketManager.connectionState.collect { state ->
 *         when (state) {
 *             is RSocketConnectionState.Connected -> {
 *                 Log.d("RSocket", "连接成功")
 *             }
 *             is RSocketConnectionState.Reconnecting -> {
 *                 Log.d("RSocket", "重连中: ${state.attempt}/${state.maxAttempts}")
 *             }
 *             is RSocketConnectionState.ConnectionFailed -> {
 *                 Log.e("RSocket", "连接失败", state.error)
 *             }
 *             is RSocketConnectionState.ConnectionClosed -> {
 *                 Log.d("RSocket", "连接关闭: ${state.reason}")
 *             }
 *             is RSocketConnectionState.Disconnected -> {
 *                 Log.d("RSocket", "已断开")
 *             }
 *             else -> { }
 *         }
 *     }
 * }
 * ```
 *
 * ### 3. 发送请求
 * ```kotlin
 * // 请求-响应模式
 * lifecycleScope.launch {
 *     val response = rsocketManager.requestResponse("api/timetable", "{}")
 *     if (response != null) {
 *         // 处理响应
 *     }
 * }
 *
 * // 发送即忘模式
 * lifecycleScope.launch {
 *     val success = rsocketManager.fireAndForget("api/heartbeat", "{}")
 * }
 * ```
 *
 * ### 4. 断开连接
 * ```kotlin
 * override fun onDestroy() {
 *     super.onDestroy()
 *     // 断开当前连接（保留单例，可重新连接）
 *     rsocketManager.disconnect()
 * }
 *
 * // 或彻底销毁（应用退出时）
 * rsocketManager.destroy()
 * ```
 *
 * @constructor 私有构造器，使用 [getInstance] 获取单例
 * @param context Android Context，用于获取 ApplicationContext
 *
 * @see RSocketRequestHandler 请求处理器
 * @see RSocketConnectionState 连接状态
 * @see ConfigObservable 配置监听
 */
class RSocketClientManager private constructor(private val context: Context) {

    /**
     * ApplicationContext，避免内存泄漏
     */
    private val appContext = context.applicationContext

    /**
     * 管理器级别 CoroutineScope
     *
     * 用于：
     * - 监听配置变化（[ConfigObservable]）
     * - 管理连接生命周期
     *
     * 生命周期与单例实例相同，调用 [destroy] 时取消
     */
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 连接级别 CoroutineScope
     *
     * 每次调用 [connect] 时创建，用于：
     * - 维护 RSocket TCP 连接
     * - 监听连接关闭事件
     * - 数据收发
     *
     * 调用 [disconnect] 或连接关闭时取消，RSocket 连接随之断开
     */
    private var connectionScope: CoroutineScope? = null

    /**
     * RSocket 实例（可能包含重连包装）
     *
     * 非空时表示已建立连接，可通过 [isConnected] 检查连接状态
     */
    private var rsocket: RSocket? = null

    /**
     * 连接状态 StateFlow
     *
     * 状态流转：
     * - Disconnected → Connecting → Connected
     * - Connected → ConnectionClosed/ConnectionFailed → Reconnecting → Connected
     * - 任意状态 → Disconnected（调用 [disconnect]）
     *
     * UI 组件可收集此 Flow 实时更新连接状态指示器
     */
    private val _connectionState = MutableStateFlow<RSocketConnectionState>(
        RSocketConnectionState.Disconnected
    )

    /**
     * 对外暴露的连接状态 StateFlow（只读）
     *
     * @return [StateFlow]<[RSocketConnectionState]> 连接状态流
     */
    val connectionState: StateFlow<RSocketConnectionState> = _connectionState.asStateFlow()

    /**
     * 请求处理器
     *
     * 封装在 RSocket 之上的请求处理方法，提供便捷的发送接口
     * 在 [connect] 成功后初始化，[disconnect] 后清空
     */
    private var requestHandler: RSocketRequestHandler? = null

    /**
     * 服务器主机地址
     *
     * 从 [ConfigObservable.config] 中的 [AppConfig.backendServer.host] 读取
     * 默认值为 [DEFAULT_HOST]
     */
    private var host: String = DEFAULT_HOST

    /**
     * 服务器端口
     *
     * 从 [ConfigObservable.config] 中的 [AppConfig.backendServer.port] 读取
     * 默认值为 [DEFAULT_PORT]
     */
    private var port: Int = DEFAULT_PORT

    /**
     * 最大重连次数
     *
     * 可通过 [connect] 的 maxRetries 参数覆盖
     */
    private val maxReconnectAttempts = 10L

    /**
     * 重连间隔（毫秒）
     *
     * 每次连接失败后等待此时间再重试，默认 5000ms（5秒）
     */
    private val reconnectDelayMs = 5000L

    companion object {
        private const val TAG = "RSocketClientManager"
        private const val DEFAULT_HOST = "localhost"
        private const val DEFAULT_PORT = 9000

        @Volatile
        private var INSTANCE: RSocketClientManager? = null

        /**
         * 获取 RSocketClientManager 单例实例
         *
         * 线程安全，首次调用时创建实例
         *
         * @param context Android Context，用于获取 ApplicationContext
         * @return [RSocketClientManager] 单例实例
         */
        @JvmStatic
        fun getInstance(context: Context): RSocketClientManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RSocketClientManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        /**
         * 销毁单例实例
         *
         * 调用 [disconnect] 断开连接并清空单例引用
         * 用于测试或需要完全重置的场景
         */
        @JvmStatic
        fun destroyInstance() {
            INSTANCE?.disconnect()
            INSTANCE = null
        }
    }

    init {
        // 监听配置变化，动态更新连接参数
        managerScope.launch {
            ConfigObservable.config.collect { config ->
                config?.let {
                    host = it.backendServer.host ?: DEFAULT_HOST
                    port = it.backendServer.port ?: DEFAULT_PORT
                    Log.d(TAG, "配置更新: host=$host, port=$port")
                }
            }
        }
    }

    /**
     * 连接到 RSocket 服务器
     *
     * 这是一个挂起函数，需要在协程中调用。
     * 连接成功后会更新 [connectionState] 为 [RSocketConnectionState.Connected]
     *
     * ## 协程上下文
     * - 在 [Dispatchers.IO] 上执行网络操作
     * - 不会阻塞调用线程
     * - 可通过返回的 Job 取消连接过程
     *
     * ## 重连逻辑
     * 使用 RSocket 内置重连机制：
     * 1. 连接失败时自动重试
     * 2. 每次重试前等待 [reconnectDelayMs]
     * 3. 达到 [maxRetries] 后停止重试并抛出异常
     *
     * @param customHost 可选的自定义主机地址，为 null 时使用配置中的 host
     * @param customPort 可选的自定义端口，为 null 时使用配置中的 port
     * @param maxRetries 最大重试次数，默认 10 次
     *
     * @throws Exception 当所有重试都失败时抛出
     *
     * @sample
     * ```kotlin
     * lifecycleScope.launch {
     *     try {
     *         rsocketManager.connect("192.168.1.100", 9000, maxRetries = 5)
     *     } catch (e: Exception) {
     *         Log.e("RSocket", "连接失败", e)
     *     }
     * }
     * ```
     */
    suspend fun connect(
        customHost: String? = null,
        customPort: Int? = null,
        maxRetries: Long = maxReconnectAttempts
    ) {
        if (_connectionState.value == RSocketConnectionState.Connected) {
            Log.d(TAG, "已经处于连接状态")
            return
        }

        if (_connectionState.value == RSocketConnectionState.Connecting) {
            Log.d(TAG, "正在连接中...")
            return
        }

        _connectionState.value = RSocketConnectionState.Connecting

        // 清理之前的连接资源
        cleanupConnection()

        val targetHost = customHost ?: host
        val targetPort = customPort ?: port

        try {
            // 创建独立的连接 Scope
            val connectionJob = SupervisorJob()
            connectionScope = CoroutineScope(Dispatchers.IO + connectionJob)

            connectionScope?.launch {
                Log.d(TAG, "正在连接到 $targetHost:$targetPort (最大重试: $maxRetries)")

                // 创建 TCP Transport
                // 使用当前协程的 coroutineContext，绑定到 connectionScope
                val transport = KtorTcpClientTransport(coroutineContext) {
                    socketOptions {
                        keepAlive = true
                    }
                }.target(targetHost, targetPort)

                // 创建 RSocket Connector
                val connector = RSocketConnector {
                    connectionConfig {
                        // 配置 KeepAlive
                        // 每 20 秒发送一次心跳，90 秒内未收到响应视为断开
                        keepAlive = KeepAlive(
                            interval = 20.seconds,
                            maxLifetime = 90.seconds
                        )
                        // 配置 MIME 类型
                        payloadMimeType = PayloadMimeType(
                            data = "application/json",
                            metadata = "message/x.rsocket.routing.v0"
                        )
                    }

                    // 配置内置重连机制
                    // predicate 返回 true 继续重试，false 停止重试
                    reconnectable { cause: Throwable, attempt: Long ->
                        if (attempt >= maxRetries) {
                            Log.e(TAG, "达到最大重试次数 ($maxRetries)，停止重连: ${cause.message}")
                            false // 停止重试
                        } else {
                            Log.w(TAG, "连接失败，第 $attempt 次重试: ${cause.message}")
                            _connectionState.value = RSocketConnectionState.Reconnecting(
                                attempt = attempt.toInt(),
                                maxAttempts = maxRetries.toInt()
                            )
                            // 延迟后再重试
                            delay(reconnectDelayMs)
                            true // 继续重试
                        }
                    }
                }

                // 建立连接（包含重连包装）
                val rsocketInstance = connector.connect(transport)
                rsocket = rsocketInstance
                requestHandler = RSocketRequestHandler(rsocketInstance)

                _connectionState.value = RSocketConnectionState.Connected

                Log.d(TAG, "RSocket 连接成功")

                // 监听连接关闭
                launch {
                    try {
                        // RSocket 继承 CoroutineScope，通过 coroutineContext 获取 Job
                        rsocketInstance.coroutineContext[Job]?.join()
                        // 连接关闭（可能是正常关闭或重试用尽）
                        Log.d(TAG, "RSocket 连接已关闭")
                        if (_connectionState.value == RSocketConnectionState.Connected) {
                            _connectionState.value = RSocketConnectionState.ConnectionClosed("连接已关闭")
                        }
                    } catch (e: Exception) {
                        // 连接异常
                        Log.e(TAG, "连接异常", e)
                        _connectionState.value = RSocketConnectionState.ConnectionFailed(e)
                    } finally {
                        cleanupConnection()
                    }
                }
            }?.join() // 等待连接完成

        } catch (e: Exception) {
            Log.e(TAG, "连接失败", e)
            _connectionState.value = RSocketConnectionState.ConnectionFailed(e)
            cleanupConnection()
        }
    }

    /**
     * 断开连接并清理资源
     *
     * 此方法会：
     * 1. 取消 [connectionScope]，关闭 TCP 连接
     * 2. 清空 [rsocket] 和 [requestHandler] 引用
     * 3. 更新 [connectionState] 为 [RSocketConnectionState.Disconnected]
     *
     * **注意**：断开连接后可以通过 [connect] 重新建立连接
     *
     * 应在以下场景调用：
     * - Activity/Fragment 销毁时
     * - 需要切换服务器地址时
     * - 应用进入后台时
     */
    fun disconnect() {
        cleanupConnection()
        _connectionState.value = RSocketConnectionState.Disconnected
        Log.d(TAG, "RSocket 已断开连接")
    }

    /**
     * 销毁管理器，释放所有资源
     *
     * 此方法会：
     * 1. 调用 [disconnect] 断开连接
     * 2. 取消 [managerScope]，停止配置监听
     *
     * **警告**：销毁后单例实例仍可继续使用（通过 [getInstance] 重新获取），
     * 但会创建新的 Scope。通常只在应用退出时调用。
     */
    fun destroy() {
        disconnect()
        managerScope.cancel()
        Log.d(TAG, "RSocketClientManager 已销毁")
    }

    /**
     * 清理连接相关资源
     *
     * 私有方法，用于内部资源清理：
     * - 取消 [connectionScope]（这会同时关闭 RSocket 连接）
     * - 清空 [rsocket] 引用
     * - 清空 [requestHandler] 引用
     */
    private fun cleanupConnection() {
        connectionScope?.cancel()
        connectionScope = null
        rsocket = null
        requestHandler = null
    }

    /**
     * 获取请求处理器
     *
     * @return [RSocketRequestHandler] 实例，如果未连接则返回 null
     *
     * @see RSocketRequestHandler.requestResponseJson
     * @see RSocketRequestHandler.fireAndForgetJson
     */
    fun getRequestHandler(): RSocketRequestHandler? {
        return requestHandler
    }

    /**
     * 检查是否已连接
     *
     * @return Boolean true 表示已连接，false 表示未连接
     */
    fun isConnected(): Boolean {
        return _connectionState.value == RSocketConnectionState.Connected && rsocket != null
    }

    /**
     * 发送请求-响应（便捷方法）
     *
     * 封装 [RSocketRequestHandler.requestResponseJson]，简化调用
     *
     * ## 协程上下文
     * 这是一个挂起函数，在 [Dispatchers.IO] 上执行网络操作
     *
     * @param route 路由路径，用于标识请求类型，如 "api/timetable"
     * @param jsonPayload JSON 格式的请求体，默认为 "{}"
     * @return String? JSON 格式的响应字符串，失败返回 null
     *
     * @sample
     * ```kotlin
     * lifecycleScope.launch {
     *     val response = rsocketManager.requestResponse("api/timetable", "{}")
     *     response?.let {
     *         // 解析 JSON 响应
     *     }
     * }
     * ```
     */
    suspend fun requestResponse(route: String, jsonPayload: String = "{}"): String? {
        return getRequestHandler()?.requestResponseJson(route, jsonPayload)
    }

    /**
     * 发送即忘（便捷方法）
     *
     * 封装 [RSocketRequestHandler.fireAndForgetJson]，简化调用
     *
     * ## 协程上下文
     * 这是一个挂起函数，在 [Dispatchers.IO] 上执行网络操作
     *
     * Fire-and-Forget 模式不等待服务器响应，适用于：
     * - 心跳包
     * - 日志上报
     * - 状态通知
     *
     * @param route 路由路径，如 "api/heartbeat"
     * @param jsonPayload JSON 格式的请求体，默认为 "{}"
     * @return Boolean true 表示发送成功，false 表示发送失败（未连接或异常）
     *
     * @sample
     * ```kotlin
     * lifecycleScope.launch {
     *     val success = rsocketManager.fireAndForget("api/heartbeat", "{}")
     *     if (!success) {
     *         Log.w("RSocket", "心跳发送失败")
     *     }
     * }
     * ```
     */
    suspend fun fireAndForget(route: String, jsonPayload: String = "{}"): Boolean {
        return getRequestHandler()?.fireAndForgetJson(route, jsonPayload) ?: false
    }
}
