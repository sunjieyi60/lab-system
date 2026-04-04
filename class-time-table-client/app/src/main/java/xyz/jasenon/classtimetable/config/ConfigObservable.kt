package xyz.jasenon.classtimetable.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 配置可观察中心（状态推送 + 观察者模式）
 *
 * 流程：
 * 1. 设备启动时先载入兜底 JSON 配置 → [AppConfigManager.loadFallbackAndPush] → 推送至此，视为完成配置
 * 2. T-io 初始化完成后向 server 发 REGISTER（协商 AES 密钥），服务器回复 REGISTER_ACK
 * 3. 若 REGISTER_ACK payload 非空：AES 解密 → 反序列化为 [AppConfig] → 写入兜底本地 JSON → [updateConfig] 更新 Observer
 *
 * 观察者：WeatherPusher、UI（如开门方式 Provider）通过 collect [config] 获取最新配置
 */
object ConfigObservable {

    private val _config = MutableStateFlow<AppConfig?>(null)

    /** 当前配置状态流，观察者 collect 即可响应配置加载/更新 */
    val config: StateFlow<AppConfig?> = _config.asStateFlow()

    /** 当前配置快照（用于 TioClientManager 等需要同步读取的场景） */
    fun getCurrent(): AppConfig? = _config.value

    /** 推送新配置（兜底加载完成 / REGISTER_ACK 解密写入后调用） */
    fun updateConfig(newConfig: AppConfig) {
        _config.value = newConfig
    }
}