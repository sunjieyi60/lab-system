package xyz.jasenon.classtimetable.network.register

import com.google.gson.annotations.SerializedName

/**
 * 设备信息 DTO，与后端 RegisterRequest 中 AES 加密内容一致
 */
data class DeviceInfoDTO(
    @SerializedName("device_id")
    val deviceId: String? = null,
    @SerializedName("device_name")
    val deviceName: String? = null,
    @SerializedName("ip_address")
    val ipAddress: String? = null,
    @SerializedName("mac_address")
    val macAddress: String? = null,
    @SerializedName("version")
    val version: String? = null
//    @SerializedName("deviceType")
//    val deviceType: String? = null,
//    @SerializedName("hardwareInfo")
//    val hardwareInfo: String? = null
)
