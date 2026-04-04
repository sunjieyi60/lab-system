package xyz.jasenon.classtimetable.ui.component.weather_ifno

import android.content.Context
import xyz.jasenon.classtimetable.config.AppConfig
import xyz.jasenon.classtimetable.config.AppConfigManager
import xyz.jasenon.classtimetable.config.WeatherDataSource

/**
 * 天气数据源工厂
 * 根据配置创建合适的数据源实例
 * 
 * 注意：使用 ApplicationContext 来避免内存泄漏
 */
object WeatherDataSourceFactory {
    
    /**
     * 根据配置创建天气数据源
     * @param context Android Context（会自动转换为 ApplicationContext）
     * @param config 应用配置
     * @return IWeatherDataSource 天气数据源实例
     */
    fun create(context: Context, config: AppConfig): IWeatherDataSource {
        // 使用 ApplicationContext 避免内存泄漏
        val applicationContext = context.applicationContext
        
        return when (config.weatherConfig.dataSource) {
            WeatherDataSource.LOCAL -> {
                // 本地联网获取：设备直接调用 Hezi 天气 API
                // 创建 AppConfigManager 实例并传入，避免数据源持有 Context
                val configManager = AppConfigManager(applicationContext)
                LocalWeatherDataSource(appConfigManager = configManager)
            }
            WeatherDataSource.REMOTE -> {
                // 远程统一获取：通过后端服务器统一获取天气信息
                val baseUrl = "http://localhost:8080"
                RemoteWeatherDataSource(
                    backendBaseUrl = baseUrl
                )
            }
        }

/**
 * 天气数据源工厂
 * 仅使用本地数据源，不访问云端 API
 */
object WeatherDataSourceFactory {

    /**
     * 创建本地天气数据源（纯本地，无网络请求）
     */
    fun create(context: Context, config: AppConfig): IWeatherDataSource {
        val applicationContext = context.applicationContext
        val configManager = AppConfigManager(applicationContext)
        return LocalWeatherDataSource(appConfigManager = configManager)
    }
}
