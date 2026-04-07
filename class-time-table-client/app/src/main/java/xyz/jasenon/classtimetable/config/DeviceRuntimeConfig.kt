package xyz.jasenon.classtimetable.config

import com.google.gson.annotations.SerializedName
import java.util.concurrent.TimeUnit

/**
 * 班牌运行时配置
 * <p>
 * 对应服务端的 Config 实体，包含从服务端下发的配置信息：
 * - 密码开门配置
 * - 人脸开门配置
 * </p>
 * <p>
 * <strong>重要：</strong>此配置由服务端通过 RSocket 下发，客户端只能读取不能修改。
 * 服务器在设备注册成功后通过 REGISTER_ACK 或后续推送下发此配置。
 * </p>
 *
 * @author Jasenon_ce
 * @see DeviceProfile
 * @since 1.0.0
 */
data class DeviceRuntimeConfig(
    /**
     * 密码
     * <p>用于密码开门验证</p>
     */
    @SerializedName("password")
    val password: String = "123456",

    /**
     * 人脸精度
     * <p>人脸匹配的阈值，范围 0-1，值越高要求越严格</p>
     */
    @SerializedName("face_precision")
    val facePrecision: Float = 0.85f,

    /**
     * 验证页面超时时间
     * <p>开门验证界面的自动关闭时间</p>
     */
    @SerializedName("timeout")
    val timeout: Int = 30,

    /**
     * 超时时间单位
     */
    @SerializedName("timeout_unit")
    val timeoutUnit: TimeUnit = TimeUnit.SECONDS
) {
    companion object {
        /**
         * 创建默认运行时配置
         * <p>仅在没有服务端配置时使用，实际应由服务端下发</p>
         */
        fun default(): DeviceRuntimeConfig {
            return DeviceRuntimeConfig()
        }
    }
}

/**
 * 完整的设备配置（档案 + 运行时配置）
 * <p>
 * 这是应用程序使用的完整配置对象，组合了本地保存的档案和从服务端获取的运行时配置。
 * </p>
 */
data class DeviceCompleteConfig(
    /**
     * 设备档案（本地保存）
     */
    val profile: DeviceProfile,

    /**
     * 运行时配置（服务端下发）
     */
    val runtimeConfig: DeviceRuntimeConfig = DeviceRuntimeConfig.default()
) {
    val uuid: String get() = profile.uuid
    val laboratoryId: Long? get() = profile.laboratoryId
    val password: String get() = runtimeConfig.password
    val facePrecision: Float get() = runtimeConfig.facePrecision
    val timeout: Int get() = runtimeConfig.timeout
    val timeoutUnit: TimeUnit get() = runtimeConfig.timeoutUnit
}
