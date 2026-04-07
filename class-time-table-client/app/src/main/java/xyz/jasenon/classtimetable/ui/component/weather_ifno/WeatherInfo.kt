package xyz.jasenon.classtimetable.ui.component.weather_ifno

/**
 * 天气信息数据类
 * <p>
 * 用于展示天气信息的数据模型，数据通过 RSocket 从服务端获取。
 * </p>
 *
 * @author Jasenon_ce
 * @see WeatherState
 * @since 1.0.0
 */
data class WeatherInfo(
    /** 县 */
    val county: String = "",
    /** 城市 */
    val city: String = "",
    /** 乡镇/街道 */
    val town: String = "",
    /** 天气描述 */
    val description: String = "--",
    /** 温度（摄氏度） */
    val temperature: Double = 0.0,
    /** 湿度（百分比） */
    val humidity: Double = 0.0,
    /** 天气图标URL */
    val weatherIcon: String = "",
    /** 当前时段的天气信息 */
    val currentHourWeather: HourWeather? = null
) {
    /**
     * 格式化为显示字符串
     * 格式：城市县区:描述 温度℃ 湿度%RH
     */
    fun toDisplayString(): String {
        val location = if (city.isNotBlank() && county.isNotBlank()) {
            "$city$county"
        } else {
            city.ifBlank { county }
        }
        return "$location:$description ${temperature}℃ ${humidity}%RH"
    }
}

/**
 * 时段天气数据类
 */
data class HourWeather(
    /** 时间，格式：HH:mm */
    val time: String = "",
    /** 天气描述 */
    val weather: String = "",
    /** 温度（摄氏度） */
    val temperature: Double = 0.0,
    /** 湿度（百分比） */
    val humidity: Double = 0.0,
    /** 天气图标URL */
    val icon: String = ""
)
