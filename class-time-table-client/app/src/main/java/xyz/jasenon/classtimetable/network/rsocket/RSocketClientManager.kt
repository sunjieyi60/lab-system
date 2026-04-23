package xyz.jasenon.classtimetable.network.rsocket

import android.content.Context
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import io.rsocket.kotlin.ConnectionAcceptorContext
import io.rsocket.kotlin.ExperimentalMetadataApi
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.core.RSocketConnector
import io.rsocket.kotlin.core.WellKnownMimeType
import io.rsocket.kotlin.keepalive.KeepAlive
import io.rsocket.kotlin.payload.Payload
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
import xyz.jasenon.classtimetable.network.rsocket.model.RSocketRequest
import xyz.jasenon.classtimetable.network.rsocket.model.SetUp
import kotlin.coroutines.CoroutineContext
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

    /**
     * 协程异常处理器，防止 RSocket 内部异常（如 ClosedSendChannelException）导致应用崩溃
     */
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        XLog.tag(TAG).e("RSocket 协程中发生未捕获异常: ${throwable.message}", throwable)
    }

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)
    private var connectionScope: CoroutineScope? = null
    private var rsocket: RSocket? = null
    private var requestHandler: RSocketRequestHandler? = null
    private var responseHandler: RSocketResponseHandler? = null
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
                        try {
                            if (isConnected() && (oldHost != host || oldPort != port)) {
                                XLog.tag(TAG).i("地址变更，尝试重新连接到新服务器...")
                                connect()
                            }
                        } catch (e: Exception) {
                            XLog.tag(TAG).e("自动重连失败", e)
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
        maxRetries: Long = maxReconnectAttempts,
        responseHandler: RSocketResponseHandler? = null
    ) = connect(customHost, customPort, maxRetries, setup, responseHandler)

    /**
     * 连接到 RSocket 服务器
     *
     * 该函数会挂起，直到【首次连接成功】或【达到重试上限】。
     * 连接成功后，该函数立即返回，保活任务将在后台持续运行。
     *
     * 在 CS 对等架构中，连接时可通过 [responseHandler] 设置 Server 端处理器，
     * 用于接收对端（服务端）主动下发的请求。
     *
     * @param customHost 自定义服务器地址，如果为 null 则使用档案中配置的地址
     * @param customPort 自定义服务器端口，如果为 null 则使用档案中配置的端口
     * @param maxRetries 最大重连次数
     * @param setup 连接时的 Setup Payload 数据
     * @param responseHandler Server 端响应处理器，用于处理对端主动下发的请求
     */
    @OptIn(ExperimentalMetadataApi::class)
    suspend fun connect(
        customHost: String? = null,
        customPort: Int? = null,
        maxRetries: Long = maxReconnectAttempts,
        setup: SetUp? = null,
        responseHandler: RSocketResponseHandler? = null
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
        // 将 exceptionHandler 加入连接作用域
        connectionScope = CoroutineScope(Dispatchers.IO + connectionJob + exceptionHandler)

        val targetHost = customHost ?: host
        val targetPort = customPort ?: port

        connectionScope?.launch {
            _connectionState.value = RSocketConnectionState.Connecting
            try {
                XLog.tag(TAG).d(">>> [CONNECT] 尝试建立连接: $targetHost:$targetPort")

                val transport = KtorTcpClientTransport(coroutineContext) {
                    socketOptions { keepAlive = true }
                }.target(targetHost, targetPort)

                // 保存响应处理器引用
                this@RSocketClientManager.responseHandler = responseHandler

                val connector = RSocketConnector {
                    connectionConfig {
                        keepAlive = KeepAlive(interval = 20.seconds, maxLifetime = 90.seconds)
                        payloadMimeType = PayloadMimeType(
                            data = WellKnownMimeType.ApplicationJson,
                            metadata = WellKnownMimeType.MessageRSocketRouting
                        )
                        setupPayload {
                            buildPayload {
                                val setupData = setup?.let { gson.toJson(it) } ?: ""
                                data(setupData)
                            }
                        }
                    }
                    acceptor {
                        object : RSocket {
                            override val coroutineContext: CoroutineContext
                                get() = connectionScope!!.coroutineContext

                            override suspend fun requestResponse(payload: Payload): Payload {
                                XLog.tag(TAG).i("接收到请求")
                                return super.requestResponse(payload)
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
        // responseHandler 由外部管理生命周期，这里不清理
    }

    /**
     * 获取 ResponseHandler，用于注册服务端主动下发请求的处理逻辑
     *
     * @return RSocketResponseHandler? 如果连接时设置了 responseHandler 则返回，否则返回 null
     */
    fun getResponseHandler(): RSocketResponseHandler? {
        return responseHandler
    }

    /**
     * 创建并设置新的 ResponseHandler
     *
     * 在连接建立后，可通过此方法创建并配置 ResponseHandler。
     * 注意：如果连接尚未建立，此方法返回的 handler 不会被使用。
     *
     * @return RSocketResponseHandler 新的响应处理器实例
     */
    fun createResponseHandler(): RSocketResponseHandler {
        val handler = RSocketResponseHandler(managerScope.coroutineContext)
        this.responseHandler = handler
        return handler
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
        return try {
            handler.requestResponseJson(route, jsonPayload)
        } catch (e: Exception) {
            XLog.tag(TAG).e("请求过程中发生异常", e)
            null
        }
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
        return try {
            handler.fireAndForgetJson(route, jsonPayload)
        } catch (e: Exception) {
            XLog.tag(TAG).e("发送过程中发生异常", e)
            false
        }
    }

    /**
     * 带范型转化的requstResponse
     * todo 修改为使用  GsonUtil反序列化
     * {@see xyz.jasenon.classtimetable.util.GsonUtil}
     */
    suspend fun <T> requestResponse(request: RSocketRequest, clazz: Class<T>): T? {
        val handler = requestHandler
        if (handler == null){
            XLog.tag(TAG).w("发送失败: 当前未连接 RSocket (${request.route})")
            return null
        }
        return try {
            val response = handler.requestResponse(request)
            if (response.isSuccess){
                gson.fromJson(response.dataAsString(), clazz)
            } else {
                XLog.tag(TAG).w("请求失败: ${response}")
                null
            }
        } catch (e: Exception) {
            XLog.tag(TAG).e("请求并解析过程中发生异常", e)
            null
        }
    }

}
