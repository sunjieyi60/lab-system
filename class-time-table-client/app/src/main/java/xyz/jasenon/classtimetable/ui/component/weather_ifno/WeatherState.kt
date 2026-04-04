package xyz.jasenon.classtimetable.ui.component.weather_ifno

/**
 * 天气状态数据类（状态推送用）
 *
 * 由 [RemoteDataObservable.weatherState] 推送，UI 通过观察该流显示天气。
 */
data class WeatherState(
    val weatherInfo: WeatherInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    fun getDisplayText(): String {
        return when {
            isLoading -> "加载中..."
            error != null -> "天气信息获取失败"
            weatherInfo != null -> weatherInfo.toDisplayString()
            else -> "暂无天气信息"
        }
    }
}
