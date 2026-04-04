package xyz.jasenon.classtimetable.ui.component.weather_ifno

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.jasenon.classtimetable.config.AppConfigManager
import xyz.jasenon.classtimetable.config.Location

/**
 * 本地天气数据源（纯本地，不请求云端）
 * 从配置中的 location 与本地默认值组装天气信息，无任何 HTTP 请求
 */
class LocalWeatherDataSource(
    private val appConfigManager: AppConfigManager
) : IWeatherDataSource {

    override suspend fun fetchWeather(): WeatherInfo = withContext(Dispatchers.Default) {
        val config = appConfigManager.getConfig()
        val location = config.weatherConfig.location ?: Location(114.39297, 30.48748)
        val lon = location.longitude ?: 114.39297
        val lat = location.latitude ?: 30.48748
        getDefaultWeatherData(lon, lat)
    }

    /**
     * 使用本地默认数据（经纬度仅用于展示或预留，不请求 API）
     */
    private fun getDefaultWeatherData(lon: Double, lat: Double): WeatherInfo {
        return WeatherInfo(
            county = "洪山区",
            city = "武汉",
            town = "",
            description = "晴",
            temperature = 24.0,
            humidity = 65.0,
            weatherIcon = "",
            currentHourWeather = null
        )
    }
}
