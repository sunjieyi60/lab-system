package xyz.jasenon.classtimetable.ui.component.weather_ifno

import android.content.Context
import xyz.jasenon.classtimetable.config.AppConfig
import xyz.jasenon.classtimetable.config.AppConfigManager

/**
 * 天气数据源工厂
 * 仅使用本地数据源，提供 Mock 静态天气数据
 */
object WeatherDataSourceFactory {

    /**
     * 创建本地天气数据源（纯本地，无网络请求）
     * @param context Android Context（会自动转换为 ApplicationContext）
     * @param config 应用配置
     * @return IWeatherDataSource 天气数据源实例
     */
    fun create(context: Context, config: AppConfig): IWeatherDataSource {
        val applicationContext = context.applicationContext
        val configManager = AppConfigManager(applicationContext)
        return LocalWeatherDataSource(appConfigManager = configManager)
    }
}
