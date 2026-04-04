package xyz.jasenon.classtimetable.network.rsocket.model

/**
 * RSocket 请求数据类
 *
 * 封装 RSocket 请求的必要信息，包括路由、请求体和元数据。
 * 这是 [RSocketRequestHandler] 各方法的标准输入类型。
 *
 * ## 字段说明
 *
 * - [route]: 路由路径，用于服务器端路由分发，如 "api/timetable"
 * - [payload]: 请求体数据，字节数组格式，通常是 JSON 序列化后的数据
 * - [metadata]: 可选的元数据，可用于传递额外的请求头信息
 *
 * ## 创建方式
 *
 * ### 1. 直接构造
 * ```kotlin
 * val request = RSocketRequest(
 *     route = "api/timetable",
 *     payload = "{\"week\":1}".toByteArray(Charsets.UTF_8),
 *     metadata = mapOf("auth" to "token123")
 * )
 * ```
 *
 * ### 2. 使用工厂方法（推荐）
 * ```kotlin
 * val request = RSocketRequest.fromJson("api/timetable", "{\"week\":1}")
 * ```
 *
 * @property route 路由路径，用于标识请求类型，服务器根据此字段路由到对应处理器
 * @property payload 请求体数据，字节数组格式
 * @property metadata 可选的元数据映射，用于传递额外请求头，默认为 null
 *
 * @see RSocketRequestHandler.requestResponse
 * @see RSocketRequestHandler.fireAndForget
 * @see RSocketRequestHandler.requestStream
 */
data class RSocketRequest(
    val route: String,
    val payload: ByteArray,
    val metadata: Map<String, String>? = null
) {

    /**
     * 比较相等性
     *
     * 比较 route、payload 和 metadata 是否都相等。
     * payload 使用 [ByteArray.contentEquals] 进行内容比较。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RSocketRequest

        if (route != other.route) return false
        if (!payload.contentEquals(other.payload)) return false
        if (metadata != other.metadata) return false

        return true
    }

    /**
     * 计算哈希码
     *
     * 基于 route、payload 和 metadata 计算，
     * payload 使用 [ByteArray.contentHashCode]。
     */
    override fun hashCode(): Int {
        var result = route.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + (metadata?.hashCode() ?: 0)
        return result
    }

    /**
     * 转换为字符串表示
     *
     * 用于调试，显示 route 和 payload 长度，不包含具体数据内容。
     */
    override fun toString(): String {
        return "RSocketRequest(route='$route', payloadSize=${payload.size}, metadata=$metadata)"
    }

    companion object {
        /**
         * 从 JSON 字符串创建请求
         *
         * 便捷工厂方法，将 JSON 字符串自动转换为字节数组。
         *
         * @param route 路由路径，如 "api/timetable"
         * @param json JSON 格式的请求体字符串
         * @return [RSocketRequest] 请求对象
         *
         * @sample
         * ```kotlin
         * val request = RSocketRequest.fromJson("api/timetable", "{\"week\":1,\"day\":2}")
         * ```
         */
        fun fromJson(route: String, json: String): RSocketRequest {
            return RSocketRequest(
                route = route,
                payload = json.toByteArray(Charsets.UTF_8)
            )
        }
    }
}
