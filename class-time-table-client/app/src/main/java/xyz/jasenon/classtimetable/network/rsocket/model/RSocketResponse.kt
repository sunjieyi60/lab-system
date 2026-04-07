package xyz.jasenon.classtimetable.network.rsocket.model

/**
 * RSocket 响应数据类
 *
 * 封装 RSocket 响应的结果，包含数据、成功状态和错误信息。
 * 这是 [RSocketRequestHandler] 各方法的统一返回类型。
 *
 * ## 字段说明
 *
 * - [data]: 响应数据，字节数组格式，成功时非 null
 * - [isSuccess]: 请求是否成功，true 表示成功，false 表示失败
 * - [errorMessage]: 错误信息，失败时非 null
 *
 * ## 使用模式
 *
 * ### 检查响应状态
 * ```kotlin
 * val response = handler.requestResponse(request)
 * if (response.isSuccess) {
 *     // 处理成功响应
 *     val json = response.dataAsString()
 * } else {
 *     // 处理错误
 *     Log.e("RSocket", "请求失败: ${response.errorMessage}")
 * }
 * ```
 *
 * ### 使用 when 表达式
 * ```kotlin
 * when {
 *     response.isSuccess && response.data != null -> {
 *         // 处理数据
 *     }
 *     response.isSuccess -> {
 *         // 成功但无数据
 *     }
 *     else -> {
 *         // 处理错误: response.errorMessage
 *     }
 * }
 * ```
 *
 * @property data 响应数据字节数组，失败时为 null
 * @property isSuccess 请求是否成功
 * @property errorMessage 错误描述信息，成功时为 null
 *
 * @see RSocketRequestHandler.requestResponse
 */
data class RSocketResponse(
    val data: ByteArray?,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
) {

    /**
     * 将响应数据转换为 UTF-8 字符串
     *
     * @return String? 数据字符串，如果 [data] 为 null 则返回 null
     *
     * @sample
     * ```kotlin
     * response.dataAsString()?.let { json ->
     *     val data = Json.decodeFromString<MyData>(json)
     * }
     * ```
     */
    fun dataAsString(): String? {
        return data?.toString(Charsets.UTF_8)
    }

    /**
     * 比较相等性
     *
     * 比较 data、isSuccess 和 errorMessage 是否都相等。
     * data 使用 [ByteArray.contentEquals] 进行内容比较。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RSocketResponse

        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        if (isSuccess != other.isSuccess) return false
        if (errorMessage != other.errorMessage) return false

        return true
    }

    /**
     * 计算哈希码
     *
     * 基于 data、isSuccess 和 errorMessage 计算，
     * data 使用 [ByteArray.contentHashCode]。
     */
    override fun hashCode(): Int {
        var result = data?.contentHashCode() ?: 0
        result = 31 * result + isSuccess.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        return result
    }

    /**
     * 转换为字符串表示
     *
     * 用于调试，显示状态和简要信息，不包含完整数据内容。
     */
    override fun toString(): String {
        return if (isSuccess) {
            "RSocketResponse(success, dataSize=${data?.size ?: 0})"
        } else {
            "RSocketResponse(failed, error=$errorMessage)"
        }
    }

    companion object {
        /**
         * 创建成功响应
         *
         * 工厂方法，用于创建包含数据的成功响应。
         *
         * @param data 响应数据字节数组，可为 null
         * @return [RSocketResponse] 成功响应对象
         *
         * @sample
         * ```kotlin
         * val json = "{\"status\":\"ok\"}".toByteArray()
         * return RSocketResponse.success(json)
         * ```
         */
        fun success(data: ByteArray?): RSocketResponse {
            return RSocketResponse(data = data, isSuccess = true)
        }

        /**
         * 创建失败响应
         *
         * 工厂方法，用于创建包含错误信息的失败响应。
         *
         * @param errorMessage 错误描述信息
         * @return [RSocketResponse] 失败响应对象
         *
         * @sample
         * ```kotlin
         * return RSocketResponse.failure("网络连接超时")
         * ```
         */
        fun failure(errorMessage: String): RSocketResponse {
            return RSocketResponse(data = null, isSuccess = false, errorMessage = errorMessage)
        }

        /**
         * 从 JSON 字符串创建成功响应
         *
         * 便捷工厂方法，将 JSON 字符串自动转换为字节数组。
         *
         * @param json JSON 格式的响应字符串
         * @return [RSocketResponse] 成功响应对象
         *
         * @sample
         * ```kotlin
         * return RSocketResponse.fromJson("{\"data\":[]}")
         * ```
         */
        fun fromJson(json: String): RSocketResponse {
            return RSocketResponse(data = json.toByteArray(Charsets.UTF_8), isSuccess = true)
        }
    }
}
