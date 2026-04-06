package xyz.jasenon.classtimetable.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 设备档案可观察中心
 * <p>
 * 使用 StateFlow 实现设备档案的状态推送和观察。
 * 其他组件可以通过收集 [profile] 流来响应档案的变化。
 * </p>
 * <p>
 * <strong>使用场景：</strong>
 * <ul>
 *   <li>RSocketClientManager: 监听服务器地址变化</li>
 *   <li>UI 组件: 显示当前设备信息</li>
 *   <li>注册流程: 获取 UUID 和 laboratoryId 发送给服务器</li>
 * </ul>
 * </p>
 *
 * @author Jasenon_ce
 * @see DeviceProfile
 * @since 1.0.0
 */
object DeviceProfileObservable {

    private val _profile = MutableStateFlow<DeviceProfile?>(null)

    /**
     * 当前档案状态流
     * <p>观察者通过 collect 此流来响应档案变化</p>
     */
    val profile: StateFlow<DeviceProfile?> = _profile.asStateFlow()

    /**
     * 当前档案快照
     * <p>用于需要同步读取档案的场景</p>
     *
     * @return 当前档案，可能为 null
     */
    fun getCurrent(): DeviceProfile? = _profile.value

    /**
     * 获取当前 UUID
     * <p>如果档案未加载，返回 null</p>
     */
    fun getCurrentUuid(): String? = _profile.value?.uuid

    /**
     * 获取当前实验室ID
     * <p>如果档案未加载或未设置，返回 null</p>
     */
    fun getCurrentLaboratoryId(): Long? = _profile.value?.laboratoryId

    /**
     * 获取当前服务器地址
     * <p>如果档案未加载，返回 null</p>
     */
    fun getCurrentServerAddress(): ServerAddress? = _profile.value?.serverAddress

    /**
     * 更新档案
     * <p>当档案被修改后调用，通知所有观察者</p>
     *
     * @param newProfile 新的档案
     */
    fun updateProfile(newProfile: DeviceProfile) {
        _profile.value = newProfile
    }

    /**
     * 更新实验室ID
     * <p>在现有档案基础上更新实验室ID</p>
     *
     * @param laboratoryId 新的实验室ID
     */
    fun updateLaboratoryId(laboratoryId: Long) {
        _profile.value?.let {
            it.laboratoryId = laboratoryId
            _profile.value = it  // 触发 StateFlow 更新
        }
    }

    /**
     * 更新服务器地址
     * <p>在现有档案基础上更新服务器地址</p>
     *
     * @param serverAddress 新的服务器地址
     */
    fun updateServerAddress(serverAddress: ServerAddress) {
        _profile.value?.let {
            it.serverAddress = serverAddress
            _profile.value = it  // 触发 StateFlow 更新
        }
    }

    /**
     * 检查档案是否已加载
     */
    fun hasProfile(): Boolean = _profile.value != null

    /**
     * 检查档案是否已完整配置
     */
    fun isConfigured(): Boolean = _profile.value?.isConfigured() == true

    /**
     * 清除档案
     * <p>主要用于重置场景</p>
     */
    fun clear() {
        _profile.value = null
    }
}

/**
 * 运行时配置可观察中心
 * <p>
 * 用于管理从服务端下发的运行时配置（Config）。
 * 这部分配置由服务端控制，客户端只能读取不能修改。
 * </p>
 */
object DeviceRuntimeConfigObservable {

    private val _config = MutableStateFlow<DeviceRuntimeConfig?>(null)

    /**
     * 当前运行时配置状态流
     */
    val config: StateFlow<DeviceRuntimeConfig?> = _config.asStateFlow()

    /**
     * 当前配置快照
     */
    fun getCurrent(): DeviceRuntimeConfig? = _config.value

    /**
     * 获取密码，如果未收到服务端配置则返回默认值
     */
    fun getPassword(): String = _config.value?.password ?: DeviceRuntimeConfig.default().password

    /**
     * 获取人脸精度，如果未收到服务端配置则返回默认值
     */
    fun getFacePrecision(): Float = _config.value?.facePrecision ?: DeviceRuntimeConfig.default().facePrecision

    /**
     * 获取超时时间，如果未收到服务端配置则返回默认值
     */
    fun getTimeout(): Int = _config.value?.timeout ?: DeviceRuntimeConfig.default().timeout

    /**
     * 更新运行时配置
     * <p>由 RSocket 收到服务端配置后调用</p>
     *
     * @param newConfig 新的运行时配置
     */
    fun updateConfig(newConfig: DeviceRuntimeConfig) {
        _config.value = newConfig
    }

    /**
     * 检查是否已收到服务端配置
     */
    fun hasConfig(): Boolean = _config.value != null

    /**
     * 清除配置
     */
    fun clear() {
        _config.value = null
    }
}
