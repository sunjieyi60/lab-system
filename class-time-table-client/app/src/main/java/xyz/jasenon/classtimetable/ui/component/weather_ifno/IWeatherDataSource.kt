package xyz.jasenon.classtimetable.ui.component.weather_ifno

/**
 * 天气数据源接口（仅本地，无云端请求）
 */
interface IWeatherDataSource {
    /**
     * 获取天气信息（本地数据）
     */
    suspend fun fetchWeather(): WeatherInfo
}
