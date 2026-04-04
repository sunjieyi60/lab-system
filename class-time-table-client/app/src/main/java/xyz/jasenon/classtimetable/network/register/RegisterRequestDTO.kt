package xyz.jasenon.classtimetable.network.register

import com.google.gson.annotations.SerializedName

/**
 * 注册请求 DTO，与后端一致；body 为 JSON，敏感部分已加密
 */
data class RegisterRequestDTO(
    @SerializedName("encryptedDeviceInfo")
    val encryptedDeviceInfo: String,
    @SerializedName("encryptedAesKey")
    val encryptedAesKey: String,
    @SerializedName("iv")
    val iv: String,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("nonce")
    val nonce: String
)
