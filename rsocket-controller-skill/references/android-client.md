# Android Kotlin Client 完整实现参考

Android 端采用 **Kotlin 协程 + rsocket-kotlin** 实现，同时作为 Client 发起请求和作为 Server 接收推送。

---

## 1. RSocketClientManager

单例管理长连接、自动重连、状态监听。

```kotlin
class RSocketClientManager private constructor(private val context: Context) {

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

    companion object {
        private const val TAG = "RSocketManager"
        private const val DEFAULT_HOST = "10.0.2.2"
        private const val DEFAULT_PORT = 9000

        @Volatile private var INSTANCE: RSocketClientManager? = null

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
                        host = address.host
                        port = address.port
                        if (isConnected()) {
                            connect()
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMetadataApi::class)
    suspend fun connect(
        customHost: String? = null,
        customPort: Int? = null,
        maxRetries: Long = 10,
        setup: SetUp? = null,
        responseHandler: RSocketResponseHandler? = null
    ) = connectMutex.withLock {
        if (isConnected() || _connectionState.value == RSocketConnectionState.Connecting) {
            return@withLock
        }

        val initialConnectSignal = CompletableDeferred<Unit>()
        cleanupInternal()

        val connectionJob = SupervisorJob()
        connectionScope = CoroutineScope(Dispatchers.IO + connectionJob + exceptionHandler)

        val targetHost = customHost ?: host
        val targetPort = customPort ?: port

        connectionScope?.launch {
            _connectionState.value = RSocketConnectionState.Connecting
            try {
                val transport = KtorTcpClientTransport(coroutineContext) {
                    socketOptions { keepAlive = true }
                }.target(targetHost, targetPort)

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
                                data(gson.toJson(setup ?: SetUp("")))
                            }
                        }
                    }
                    acceptor {
                        val server = requester
                        responseHandler!!   // Android 作为 Server 接收对端请求
                    }
                    reconnectable { cause, attempt ->
                        if (attempt >= maxRetries) {
                            if (!initialConnectSignal.isCompleted) {
                                initialConnectSignal.completeExceptionally(cause)
                            }
                            false
                        } else {
                            _connectionState.value = RSocketConnectionState.Reconnecting(
                                attempt.toInt(), maxRetries.toInt()
                            )
                            delay(5000)
                            true
                        }
                    }
                }

                val rsocketInstance = connector.connect(transport)
                rsocket = rsocketInstance
                requestHandler = RSocketRequestHandler(rsocketInstance)
                _connectionState.value = RSocketConnectionState.Connected
                initialConnectSignal.complete(Unit)

                try {
                    rsocketInstance.coroutineContext[Job]?.join()
                } finally {
                    cleanupInternal()
                }
            } catch (e: Exception) {
                if (!initialConnectSignal.isCompleted) {
                    initialConnectSignal.completeExceptionally(e)
                }
                _connectionState.value = RSocketConnectionState.ConnectionFailed(e)
                cleanupInternal()
            }
        }

        initialConnectSignal.await()
    }

    fun isConnected(): Boolean {
        return _connectionState.value == RSocketConnectionState.Connected && rsocket != null
    }

    fun disconnect() {
        cleanupInternal()
        _connectionState.value = RSocketConnectionState.Disconnected
    }

    private fun cleanupInternal() {
        connectionScope?.cancel()
        connectionScope = null
        rsocket = null
        requestHandler = null
    }

    fun getResponseHandler(): RSocketResponseHandler? = responseHandler

    fun createResponseHandler(): RSocketResponseHandler {
        val handler = RSocketResponseHandler(managerScope.coroutineContext)
        this.responseHandler = handler
        return handler
    }

    suspend fun requestResponse(route: String, jsonPayload: String = "{}"): String? {
        return requestHandler?.requestResponseJson(route, jsonPayload)
    }

    suspend fun fireAndForget(route: String, jsonPayload: String = "{}"): Boolean {
        return requestHandler?.fireAndForgetJson(route, jsonPayload) ?: false
    }

    suspend fun <T> requestResponse(request: RSocketRequest, clazz: Class<T>): T? {
        val response = requestHandler?.requestResponse(request) ?: return null
        return if (response.isSuccess) {
            gson.fromJson(response.dataAsString(), clazz)
        } else null
    }
}
```

---

## 2. RSocketRequestHandler

Client 角色：发送 Request-Response / Fire-and-Forget / Request-Stream。

```kotlin
class RSocketRequestHandler(private val rsocket: RSocket?) {

    @OptIn(ExperimentalMetadataApi::class)
    suspend fun requestResponse(request: RSocketRequest): RSocketResponse {
        if (rsocket == null) return RSocketResponse.failure("RSocket 未连接")

        return try {
            val payload = buildPayload {
                data(request.payload)
                metadata(RoutingMetadata(request.route))
            }
            val responsePayload = rsocket.requestResponse(payload)
            RSocketResponse.success(responsePayload.data.readBytes())
        } catch (e: Exception) {
            RSocketResponse.failure(e.message ?: "请求失败")
        }
    }

    @OptIn(ExperimentalMetadataApi::class)
    suspend fun fireAndForget(request: RSocketRequest): Boolean {
        if (rsocket == null) return false

        return try {
            val payload = buildPayload {
                data(request.payload)
                metadata(RoutingMetadata(request.route))
            }
            rsocket.fireAndForget(payload)
            true
        } catch (e: Exception) {
            false
        }
    }

    @OptIn(ExperimentalMetadataApi::class)
    fun requestStream(request: RSocketRequest): Flow<RSocketResponse> = flow {
        if (rsocket == null) {
            emit(RSocketResponse.failure("RSocket 未连接"))
            return@flow
        }
        try {
            val payload = buildPayload {
                data(request.payload)
                metadata(RoutingMetadata(request.route))
            }
            rsocket.requestStream(payload).collect { responsePayload ->
                emit(RSocketResponse.success(responsePayload.data.readBytes()))
            }
        } catch (e: Exception) {
            emit(RSocketResponse.failure(e.message ?: "流请求失败"))
        }
    }

    suspend fun requestResponseJson(route: String, jsonPayload: String = "{}"): String? {
        val request = RSocketRequest.fromJson(route, jsonPayload)
        val response = requestResponse(request)
        return if (response.isSuccess) response.dataAsString() else null
    }

    suspend fun fireAndForgetJson(route: String, jsonPayload: String = "{}"): Boolean {
        val request = RSocketRequest.fromJson(route, jsonPayload)
        return fireAndForget(request)
    }

    fun requestStreamJson(route: String, jsonPayload: String = "{}"): Flow<RSocketResponse> {
        val request = RSocketRequest.fromJson(route, jsonPayload)
        return requestStream(request)
    }
}
```

---

## 3. RSocketResponseHandler

Server 角色：接收对端请求，按 `route` 分发到 handler。

```kotlin
@OptIn(ExperimentalMetadataApi::class)
class RSocketResponseHandler(
    override val coroutineContext: CoroutineContext
) : RSocket {

    companion object {
        private const val TAG = "RSocketResponseHandler"
    }

    private val handlers = mutableMapOf<String, suspend (ByteArray) -> RSocketResponse>()

    fun registerHandler(route: String, handler: suspend (ByteArray) -> RSocketResponse) {
        handlers[route] = handler
        XLog.tag(TAG).d("注册处理器: $route")
    }

    fun unregisterHandler(route: String) {
        handlers.remove(route)
    }

    override suspend fun requestResponse(payload: Payload): Payload {
        val route = extractRoute(payload)
        val data = payload.data.readBytes()
        val handler = handlers[route]
        return if (handler != null) {
            try {
                buildResponsePayload(handler(data))
            } catch (e: Exception) {
                buildErrorPayload("处理请求失败: ${e.message}")
            }
        } else {
            buildErrorPayload("Unknown route: $route")
        }
    }

    override suspend fun fireAndForget(payload: Payload) {
        val route = extractRoute(payload)
        val data = payload.data.readBytes()
        handlers[route]?.invoke(data)
    }

    override fun requestStream(payload: Payload): Flow<Payload> {
        XLog.tag(TAG).w("Server 端暂不支持 Request-Stream")
        return emptyFlow()
    }

    override fun requestChannel(initPayload: Payload, payloads: Flow<Payload>): Flow<Payload> {
        XLog.tag(TAG).w("Server 端暂不支持 Request-Channel")
        return emptyFlow()
    }

    private fun extractRoute(payload: Payload): String {
        return try {
            payload.metadata?.let { metadata ->
                val routingMetadata = metadata.read(RoutingMetadata)
                routingMetadata.tags.firstOrNull() ?: "unknown"
            } ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun buildResponsePayload(response: RSocketResponse): Payload {
        return buildPayload { data(response.data ?: byteArrayOf()) }
    }

    private fun buildErrorPayload(errorMessage: String): Payload {
        return buildPayload {
            data("""{"success":false,"error":"$errorMessage"}""".toByteArray(Charsets.UTF_8))
        }
    }

    // 便捷注册方法
    fun onConfigUpdate(handler: suspend (ByteArray) -> RSocketResponse) {
        registerHandler(Const.Route.DEVICE_CONFIG_UPDATE, handler)
    }
    fun onFaceUpdate(handler: suspend (ByteArray) -> RSocketResponse) {
        registerHandler(Const.Route.DEVICE_FACE_UPDATE, handler)
    }
    fun onScheduleUpdate(handler: suspend (ByteArray) -> RSocketResponse) {
        registerHandler(Const.Route.DEVICE_SCHEDULE_UPDATE, handler)
    }
    fun onDoorOpen(handler: suspend (ByteArray) -> RSocketResponse) {
        registerHandler(Const.Route.DEVICE_DOOR_OPEN, handler)
    }
    fun onReboot(handler: suspend (ByteArray) -> RSocketResponse) {
        registerHandler(Const.Route.DEVICE_REBOOT, handler)
    }
    fun onCommand(handler: suspend (ByteArray) -> RSocketResponse) {
        registerHandler(Const.Route.DEVICE_COMMAND, handler)
    }
}
```

---

## 4. 数据模型定义

```kotlin
// 请求数据类
data class RSocketRequest(
    val route: String,
    val payload: ByteArray,
    val metadata: Map<String, String>? = null
) {
    companion object {
        fun fromJson(route: String, json: String): RSocketRequest {
            return RSocketRequest(route, json.toByteArray(Charsets.UTF_8))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RSocketRequest
        return route == other.route
                && payload.contentEquals(other.payload)
                && metadata == other.metadata
    }

    override fun hashCode(): Int {
        var result = route.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + (metadata?.hashCode() ?: 0)
        return result
    }
}

// 响应数据类
data class RSocketResponse(
    val data: ByteArray?,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
) {
    fun dataAsString(): String? = data?.toString(Charsets.UTF_8)

    companion object {
        fun success(data: ByteArray?): RSocketResponse {
            return RSocketResponse(data, true)
        }
        fun failure(errorMessage: String): RSocketResponse {
            return RSocketResponse(null, false, errorMessage)
        }
        fun fromJson(json: String): RSocketResponse {
            return RSocketResponse(json.toByteArray(Charsets.UTF_8), true)
        }
    }
}

// 可请求接口（模板方法模式）
interface RSocketRequestable {
    fun getRoute(): String
    fun convert(metadata: Map<String, String> = HashMap()): RSocketRequest {
        return RSocketRequest(getRoute(), GsonUtil.toByteArray(this), metadata)
    }
}

// 业务请求示例
data class RegisterRequest(
    val uuid: String,
    val laboratoryId: Long? = null
) : RSocketRequestable {
    override fun getRoute(): String = Const.Route.DEVICE_REGISTER
}

@Serializable
data class RegisterResponse(
    val uuid: String,
    val config: DeviceRuntimeConfig?,
    val laboraotryId: Long?
)
```

---

## 5. 使用示例

### 5.1 建立连接

```kotlin
lifecycleScope.launch {
    val responseHandler = RSocketResponseHandler(coroutineContext)

    responseHandler.onConfigUpdate { data ->
        val config = GsonUtil.fromJson<UpdateConfigRequest>(data)
        // 更新本地配置...
        RSocketResponse.fromJson("""{"success":true}""")
    }

    responseHandler.onDoorOpen { data ->
        val request = GsonUtil.fromJson<OpenDoorRequest>(data)
        // 执行开门...
        RSocketResponse.fromJson("""{"success":true,"openTime":${System.currentTimeMillis()}}""")
    }

    responseHandler.onFaceUpdate { data ->
        val request = GsonUtil.fromJson<UpdateFaceLibraryRequest>(data)
        // 更新人脸库...
        RSocketResponse.fromJson("""{"success":true,"currentVersion":${request.libraryVersion}}""")
    }

    RSocketClientManager.getInstance(applicationContext).connect(
        customHost = serverAddress?.host,
        customPort = serverAddress?.port,
        setup = SetUp(DeviceProfileObservable.getCurrentUuid()),
        responseHandler = responseHandler
    )
}
```

### 5.2 发送请求

```kotlin
// 注册请求
lifecycleScope.launch {
    val response = RSocketClientManager.getInstance(context).requestResponse(
        request = RegisterRequest(uuid = "device-uuid-123", laboratoryId = 1L).convert(),
        clazz = RegisterResponse::class.java
    )
    response?.let {
        // 处理注册响应
    }
}

// 心跳（Fire-and-Forget）
lifecycleScope.launch {
    RSocketClientManager.getInstance(context).fireAndForget(
        route = Const.Route.DEVICE_HEARTBEAT,
        jsonPayload = """{"uuid":"device-uuid-123","interval":30}"""
    )
}
```

### 5.3 监听连接状态

```kotlin
lifecycleScope.launch {
    RSocketClientManager.getInstance(context).connectionState.collect { state ->
        when (state) {
            is RSocketConnectionState.Connected -> {
                // 连接成功
            }
            is RSocketConnectionState.Reconnecting -> {
                // 重连中：state.attempt / state.maxAttempts
            }
            is RSocketConnectionState.ConnectionFailed -> {
                // 连接失败：state.cause
            }
            is RSocketConnectionState.Disconnected -> {
                // 已断开
            }
            else -> {}
        }
    }
}
```
