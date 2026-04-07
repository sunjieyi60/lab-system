package xyz.jasenon.classtimetable.network.rsocket

import android.content.Context
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.core.RSocketConnector
import io.rsocket.kotlin.keepalive.KeepAlive
import io.rsocket.kotlin.payload.PayloadMimeType
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import io.rsocket.kotlin.transport.ktor.tcp.KtorTcpClientTransport
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.jasenon.classtimetable.config.DeviceProfileObservable
import xyz.jasenon.classtimetable.network.rsocket.model.SetUp
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

/**
 * RSocket 客户端管理器
 *
 * 负责管理 RSocket 长连接的建立、心跳保活、自动重连以及提供请求接口。
 * 服务器地址从 [DeviceProfileObservable] 获取，当地址变化时自动更新。
 *
 * @author Jasenon_ce
 * @see DeviceProfileObservable
 * @since 1.0.0
 */
class RSocketClientManager private constructor(private val context: Context) {

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectionScope: CoroutineScope? = null
    private var rsocket: RSocket? = null
    private var requestHandler: RSocketRequestHandler? = null
    private val gson = Gson()
    private val connectMutex = Mutex()

    private val _connectionState = MutableStateFlow<RSocketConnectionState>(
        RSocketConnectionState.Disconnected
    )
    val connectionState: StateFlow<RSocketConnectionState> = _connectionState.asStateFlow()

    private var host: String = DEFAULT_HOST
    private var port: Int = DEFAULT_PORT
    private val maxReconnectAttempts = 10L
    private val reconnectDelayMs = 5000L

    companion object {
        private const val TAG = "RSocketManager"
        private const val DEFAULT_HOST = "10.0.2.2"
        private const val DEFAULT_PORT = 9000

        @Volatile
        private var INSTANCE: RSocketClientManager? = null

        @JvmStatic
        fun getInstance(context: Context): RSocketClientManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RSocketClientManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        @JvmStatic
        fun destroyInstance() {
            INSTANCE?.disconnect()
            INSTANCE = null
        }
    }

    init {
        // 监听设备档案变化，自动更新服务器地址
        managerScope.launch {
            DeviceProfileObservable.profile.collect { profile ->
                profile?.serverAddress?.let { address ->
                    if (address.isValid()) {
                        val oldHost = host
                        val oldPort = port
                        host = address.host
                        port = address.port
                        XLog.tag(TAG).d("服务器地址自动更新: $host:$port")
                        
                        // 如果地址真的变了且已连接，建议重新连接以应用新地址
                        if (isConnected() && (oldHost != host || oldPort != port)) {
                            XLog.tag(TAG).i("地址变更，尝试重新连接到新服务器...")
                            connect()
                        }
                    }
                }
            }
        }
    }

    /**
     * 重载的连接方法，方便直接传入 SetUp 对象
     */
    suspend fun connect(
        setup: SetUp,
        customHost: String? = null,
        customPort: Int? = null,
        maxRetries: Long = maxReconnectAttempts
    ) = connect(customHost, customPort, maxRetries, setup)

    /**
     * 连接到 RSocket 服务器
     *
     * 该函数会挂起，直到【首次连接成功】或【达到重试上限】。
     * 连接成功后，该函数立即返回，保活任务将在后台持续运行。
     *
     * @param customHost 自定义服务器地址，如果为 null 则使用档案中配置的地址
     * @param customPort 自定义服务器端口，如果为 null 则使用档案中配置的端口
     * @param maxRetries 最大重连次数
     * @param setup 连接时的 Setup Payload 数据
     */
    suspend fun connect(
        customHost: String? = null,
        customPort: Int? = null,
        maxRetries: Long = maxReconnectAttempts,
        setup: SetUp? = null
    ) = connectMutex.withLock {
        if (isConnected()) {
            XLog.tag(TAG).d("当前已处于连接状态，无需重复连接")
            return@withLock
        }

        if (_connectionState.value == RSocketConnectionState.Connecting) {
            XLog.tag(TAG).d("正在连接中，请稍候...")
            return@withLock
        }

        val initialConnectSignal = CompletableDeferred<Unit>()

        cleanupInternal()
        val connectionJob = SupervisorJob()
        connectionScope = CoroutineScope(Dispatchers.IO + connectionJob)

        val targetHost = customHost ?: host
        val targetPort = customPort ?: port

        connectionScope?.launch {
            _connectionState.value = RSocketConnectionState.Connecting
            try {
                XLog.tag(TAG).d(">>> [CONNECT] 尝试建立连接: $targetHost:$targetPort")

                val transport = KtorTcpClientTransport(coroutineContext) {
                    socketOptions { keepAlive = true }
                }.target(targetHost, targetPort)

                val connector = RSocketConnector {
                    connectionConfig {
                        keepAlive = KeepAlive(interval = 20.seconds, maxLifetime = 90.seconds)
                        payloadMimeType = PayloadMimeType(
                            data = "application/json",
                            metadata = "message/x.rsocket.routing.v0"
                        )
                        // 使用传入的 setup 对象，如果为空则发送默认的 "hello"
                        setupPayload {
                            buildPayload {
                                val setupData = setup?.let { gson.toJson(it) } ?: "hello"
                                data(setupData)
                            }
                        }
                    }

                    reconnectable { cause, attempt ->
                        if (attempt >= maxRetries) {
                            XLog.tag(TAG).e(">>> [RECONNECT] 达到重试上限 ($maxRetries)")
                            if (!initialConnectSignal.isCompleted) {
                                initialConnectSignal.completeExceptionally(cause)
                            }
                            false
                        } else {
                            XLog.tag(TAG).w(">>> [RECONNECT] 连接丢失，第 $attempt 次重连...")
                            _connectionState.value = RSocketConnectionState.Reconnecting(
                                attempt.toInt(), maxRetries.toInt()
                            )
                            delay(reconnectDelayMs)
                            true
                        }
                    }
                }

                val rsocketInstance = connector.connect(transport)
                rsocket = rsocketInstance
                requestHandler = RSocketRequestHandler(rsocketInstance)
                _connectionState.value = RSocketConnectionState.Connected

                XLog.tag(TAG).i(">>> [SUCCESS] RSocket 连接已建立，Setup 数据已发送")
                initialConnectSignal.complete(Unit)

                try {
                    rsocketInstance.coroutineContext[Job]?.join()
                } catch (e: Exception) {
                    XLog.tag(TAG).e(">>> [MONITOR] 连接监控异常", e)
                } finally {
                    cleanupInternal()
                }

            } catch (e: Exception) {
                XLog.tag(TAG).e(">>> [FATAL] 连接建立失败", e)
                if (!initialConnectSignal.isCompleted) {
                    initialConnectSignal.completeExceptionally(e)
                }
                _connectionState.value = RSocketConnectionState.ConnectionFailed(e)
                cleanupInternal()
            }
        }

        try {
            initialConnectSignal.await()
        } catch (e: Exception) {
            XLog.tag(TAG).e("connect() 任务由于错误返回: ${e.message}")
            throw e
        }
    }

    fun isConnected(): Boolean {
        return _connectionState.value == RSocketConnectionState.Connected && rsocket != null
    }

    fun disconnect() {
        cleanupInternal()
        _connectionState.value = RSocketConnectionState.Disconnected
        XLog.tag(TAG).d("主动断开 RSocket 连接")
    }

    private fun cleanupInternal() {
        connectionScope?.cancel()
        connectionScope = null
        rsocket = null
        requestHandler = null
    }

    /**
     * 发送请求-响应（供外部业务调用）
     *
     * @param route 路由地址
     * @param jsonPayload JSON 格式的请求体
     * @return 服务器响应的 JSON 字符串，失败返回 null
     */
    suspend fun requestResponse(route: String, jsonPayload: String = "{}"): String? {
        val handler = requestHandler
        if (handler == null) {
            XLog.tag(TAG).w("请求失败: 当前未连接 RSocket ($route)")
            return null
        }
        XLog.tag(TAG).d("发送请求 [$route]: $jsonPayload")
        return handler.requestResponseJson(route, jsonPayload)
    }

    /**
     * 发送即忘
     *
     * @param route 路由地址
     * @param jsonPayload JSON 格式的请求体
     * @return 是否发送成功
     */
    suspend fun fireAndForget(route: String, jsonPayload: String = "{}"): Boolean {
        val handler = requestHandler
        if (handler == null) {
            XLog.tag(TAG).w("发送失败: 当前未连接 RSocket ($route)")
            return false
        }
        return handler.fireAndForgetJson(route, jsonPayload)
    }
}
