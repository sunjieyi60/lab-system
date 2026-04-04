package xyz.jasenon.classtimetable.config

import android.icu.util.TimeUnit
import com.google.gson.annotations.SerializedName

/**
 * 应用配置数据类
 * 目前仅从本地 config.json 中读取后端服务和 Hezi 接口配置
 * deviceId 由服务端在 REGISTER_ACK 中下发并写入 config.json，未注册前为空
 */
data class AppConfig(
    @SerializedName("device_id")
    val deviceId: String? = null,
    @SerializedName("backend_server")
    val backendServer: BackendServerConfig,
    @SerializedName("weather_config")
    val weatherConfig: WeatherConfig,
    @SerializedName("hezi_config")
    val heziConfig: HeziConfig,
    @SerializedName("door_open_config")
    val doorOpenConfig: DoorOpenConfig = DoorOpenConfig()
)

/**
 * 后端服务器配置
 */
data class BackendServerConfig(
    @SerializedName("host")
    val host: String? = "localhost",
    @SerializedName("port")
    val port: Int? = 9000,
    @SerializedName("timeout")
    val timeout: Long? = 5000L,
    @SerializedName("heart")
    val heartPeriod: Long? = 5000L
)

/**
 * 天气配置（仅本地，无云端地址）
 */
data class WeatherConfig(
    @SerializedName("location")
    var location: Location? = Location(),
    @SerializedName("update_interval")
    val updateInterval: Int? = 120
)

data class Location(
    val longitude: Double? = 0.0,
    val latitude: Double? = 0.0
)

/**
 * Hezi 配置：仅保留轮询周期、id、key，不包含任何 API 地址
 */
data class HeziConfig(
    @SerializedName("poll_interval")
    val pollIntervalMinutes: Int? = 120,
    @SerializedName("open_id")
    val openId: String = "",
    @SerializedName("app_key")
    val appKey: String = ""
)

data class DoorOpenConfig(
    @SerializedName("pwd_open_config")
    val passwordOpenConfig: PasswordOpenConfig = PasswordOpenConfig(),
    @SerializedName("face_open_config")
    val faceOpenConfig: FaceOpenConfig = FaceOpenConfig()
)

/**
 * 密码开门配置
 * 继承自 MaskKeepingTime，包含密码和保持时间配置
 */
class PasswordOpenConfig(
    @SerializedName("password")
    val password: String = "123456",
    @SerializedName("keep_time")
    val keepTimeConfig: MaskKeepingTime = MaskKeepingTime()
)

class FaceOpenConfig(
    @SerializedName("precision")
    val precision: Float = 0.85f,
    @SerializedName("keep_time")
    val keepTimeConfig: MaskKeepingTime = MaskKeepingTime()
)

/**
 * 保持时间配置基类
 * 用于配置保持时间及其单位
 */
data class MaskKeepingTime(
    @SerializedName("keep_time")
    val keepTime: Int = 30,
    val keepTimeUnit: TimeUnit = TimeUnit.SECOND
)

