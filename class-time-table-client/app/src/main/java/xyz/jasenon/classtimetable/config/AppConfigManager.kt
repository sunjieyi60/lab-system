package xyz.jasenon.classtimetable.config

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import xyz.jasenon.classtimetable.config.ConfigObservable
import java.io.InputStream

/**
 * 应用配置管理器
 *
 * 流程：
 * 1. 设备启动时调用 [loadFallbackAndPush]：载入兜底 JSON（本地 SP 或 assets/config.json）→ 推送 [ConfigObservable]，视为完成配置
 * 2. T-io 收到 REGISTER_ACK 且 payload 非空时，由 [RegisterAckPacketHandler] 解密后调用 [saveConfigFromServer]：写入兜底本地 JSON 并更新 Observer
 */
class AppConfigManager(private val context: Context) {

    private val gson = Gson()
    private val configFileName = "config.json"
    private val sharedPrefsName = "app_config"
    private val configKey = "app_config_json"

    /**
     * 载入兜底 JSON 配置并推送 Observer（设备启动时调用）
     * 优先读本地 SP，无则读 assets/config.json；推送后视为完成配置。
     */
    fun loadFallbackAndPush(): AppConfig {
        return try {
            val savedConfigJson = context.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE)
                .getString(configKey, null)
            val config = if (savedConfigJson != null) {
                gson.fromJson(savedConfigJson, AppConfig::class.java)
            } else {
                loadFallbackFromAssets()
            }
            ConfigObservable.updateConfig(config)
            Log.d(TAG, "兜底配置已载入并推送 Observer")
            config
        } catch (e: Exception) {
            Log.e(TAG, "载入兜底配置失败", e)
            val fallback = loadFallbackFromAssets()
            ConfigObservable.updateConfig(fallback)
            fallback
        }
    }

    /**
     * 获取当前配置：若已有则返回快照，否则先 [loadFallbackAndPush] 再返回
     */
    fun getConfig(): AppConfig {
        return ConfigObservable.getCurrent() ?: loadFallbackAndPush()
    }

    /**
     * 服务器下发的配置写入兜底本地 JSON 并更新 Observer（REGISTER_ACK 解密后调用）
     */
    fun saveConfigFromServer(config: AppConfig) {
        try {
            val configJson = gson.toJson(config)
            context.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE)
                .edit()
                .putString(configKey, configJson)
                .apply()
            ConfigObservable.updateConfig(config)
            Log.d(TAG, "服务器配置已写入兜底 JSON 并更新 Observer")
        } catch (e: Exception) {
            Log.e(TAG, "保存服务器配置失败", e)
        }
    }

    /**
     * 从 assets 目录加载兜底配置
     */
    private fun loadFallbackFromAssets(): AppConfig {
        try {
            val inputStream: InputStream = context.assets.open(configFileName)
            val json = inputStream.bufferedReader().use { it.readText() }
            val appConfig: AppConfig = gson.fromJson(json, AppConfig::class.java)
            if (appConfig.weatherConfig.location?.latitude == null || appConfig.weatherConfig.location?.longitude == null) {
                appConfig.weatherConfig.location = Location(114.39297, 30.48748)
            }
            return appConfig
        } catch (e: Exception) {
            Log.e(TAG, "加载默认配置失败", e)
            throw RuntimeException(e)
        }
    }
    
    /** 重置为兜底（清除 SP，下次 getConfig 会从 assets 加载） */
    fun resetToDefault() {
        try {
            context.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE)
                .edit()
                .remove(configKey)
                .apply()
            Log.d(TAG, "已重置为默认配置")
        } catch (e: Exception) {
            Log.e(TAG, "重置配置失败", e)
        }
    }

    companion object {
        private const val TAG = "AppConfigManager"
    }
}
