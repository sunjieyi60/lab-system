package xyz.jasenon.classtimetable.network.register

import com.google.gson.annotations.SerializedName

/**
 * 服务端 REGISTER_ACK 解密后的配置（与后端 DeviceConfigDTO 对应）
 * 用于解析 deviceId 与 server，合并到本地 AppConfig
 */
data class ServerConfigResponse(
    @SerializedName("deviceId")
    val deviceId: String? = null,
    @SerializedName("server")
    val server: ServerInfo? = null
) {
    data class ServerInfo(
        @SerializedName("host")
        val host: String? = null,
        @SerializedName("port")
        val port: Int? = null
    )
}
