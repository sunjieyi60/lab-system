package xyz.jasenon.classtimetable.config

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * 班牌设备档案
 * <p>
 * 对应服务端的 ClassTimeTable 实体，包含设备在本地需要保存的配置信息：
 * - uuid: 设备唯一标识（首次启动生成，不可修改）
 * - laboratoryId: 关联的实验室ID（可配置）
 * - serverAddress: 服务器地址（可配置）
 * </p>
 * <p>
 * <strong>注意：</strong>config 字段（密码、人脸精度等）由服务端下发，不保存在本地配置中。
 * </p>
 *
 * @author Jasenon_ce
 * @see DeviceProfileManager
 * @since 1.0.0
 */
data class DeviceProfile(
    /**
     * 班牌唯一编号
     * <p>首次启动时自动生成，终身不变，不可重新配置</p>
     */
    @SerializedName("uuid")
    val uuid: String,

    /**
     * 关联的实验室ID
     * <p>需要在配置页面中设置</p>
     */
    @SerializedName("laboratory_id")
    var laboratoryId: Long? = null,

    /**
     * 服务器地址配置
     */
    @SerializedName("server_address")
    var serverAddress: ServerAddress = ServerAddress(),

    /**
     * 设备状态
     * <p>仅本地使用，不上报</p>
     */
    @SerializedName("status")
    var status: String = DeviceStatus.OFFLINE
) {
    companion object {
        /**
         * 生成新的 UUID
         * <p>使用标准 UUID 格式，去除横线</p>
         */
        fun generateUuid(): String {
            return UUID.randomUUID().toString().replace("-", "")
        }

        /**
         * 创建默认档案
         * <p>自动生成 UUID，其他字段需要后续配置</p>
         */
        fun createDefault(): DeviceProfile {
            return DeviceProfile(
                uuid = generateUuid(),
                laboratoryId = null,
                serverAddress = ServerAddress(),
                status = DeviceStatus.OFFLINE
            )
        }
    }

    /**
     * 检查档案是否已完整配置
     * <p>需要满足：laboratoryId 不为空，服务器地址有效</p>
     */
    fun isConfigured(): Boolean {
        return laboratoryId != null && serverAddress.isValid()
    }

    /**
     * 获取需要配置的字段列表
     */
    fun getMissingConfigs(): List<String> {
        val missing = mutableListOf<String>()
        if (laboratoryId == null) missing.add("laboratoryId")
        if (!serverAddress.isValid()) missing.add("serverAddress")
        return missing
    }
}

/**
 * 服务器地址配置
 */
data class ServerAddress(
    @SerializedName("host")
    var host: String = "10.0.2.2",  // 默认使用 Android 模拟器宿主地址

    @SerializedName("port")
    var port: Int = 7001,

    @SerializedName("timeout_ms")
    var timeoutMs: Long = 5000L
) {
    /**
     * 检查服务器地址是否有效
     */
    fun isValid(): Boolean {
        return host.isNotBlank() && port in 1..65535
    }

    /**
     * 获取完整地址字符串
     */
    fun toAddressString(): String = "$host:$port"
}

/**
 * 设备状态常量
 */
object DeviceStatus {
    const val ONLINE = "ONLINE"
    const val OFFLINE = "OFFLINE"
}
