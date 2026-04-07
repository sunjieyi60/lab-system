package xyz.jasenon.classtimetable.ui.component.weather_ifno

/**
 * 天气状态数据类
 * <p>
 * 用于 UI 展示的天气状态封装，包含天气信息、加载状态和错误信息。
 * 数据通过 RSocket 从服务端获取并推送到 [RemoteDataObservable.weatherState]。
 * </p>
 * <p>
 * <strong>注意：</strong>天气数据获取机制后续通过 RSocket 实现，当前仅保留状态定义。
 * </p>
 *
 * @author Jasenon_ce
 * @see WeatherInfo
 * @see xyz.jasenon.classtimetable.network.observe.RemoteDataObservable
 * @since 1.0.0
 */
data class WeatherState(
    /** 天气信息 */
    val weatherInfo: WeatherInfo? = null,
    /** 是否加载中 */
    val isLoading: Boolean = false,
    /** 错误信息 */
    val error: String? = null
) {
    /**
     * 获取用于 UI 显示的文本
     */
    fun getDisplayText(): String {
        return when {
            isLoading -> "加载中..."
            error != null -> "天气信息获取失败"
            weatherInfo != null -> weatherInfo.toDisplayString()
            else -> "暂无天气信息"
        }
    }

    companion object {
        /**
         * 空状态
         */
        fun empty(): WeatherState = WeatherState()

        /**
         * 加载中状态
         */
        fun loading(): WeatherState = WeatherState(isLoading = true)

        /**
         * 错误状态
         */
        fun error(message: String): WeatherState = WeatherState(error = message)

        /**
         * 成功状态
         */
        fun success(weatherInfo: WeatherInfo): WeatherState = WeatherState(weatherInfo = weatherInfo)
    }
}
