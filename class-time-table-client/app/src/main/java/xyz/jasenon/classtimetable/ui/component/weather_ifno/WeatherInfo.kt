package xyz.jasenon.classtimetable.ui.component.weather_ifno

/**
 * 地址信息数据类
 * 从地址查询接口获取的地址信息
 * 接口文档：https://www.apihz.cn/api/jwjuhe2.html
 */
data class AddressInfo(
    val county: String,  // 县
    val city: String,     // 城市（对应接口返回的 city）
    val town: String      // 乡镇/街道（对应接口返回的 town）
)

/**
 * 时段天气数据类
 */
data class HourWeather(
    val time: String,        // 时间，格式：HH:mm
    val weather: String,     // 天气描述
    val temperature: Double,    // 温度（摄氏度）
    val humidity: Double,       // 湿度（百分比）
    val icon: String        // 天气图标URL
)

/**
 * 天气信息数据类
 * 
 * 地址信息通过经纬度查询接口获取：
 * - country: 县
 * - city: 城市
 * - town: 乡镇/街道
 */
data class WeatherInfo(
    val county: String,                    // 县
    val city: String,                       // 城市
    val town: String,                       // 乡镇/街道
    val description: String,                // 天气描述
    val temperature: Double,                   // 温度（摄氏度）
    val humidity: Double,                      // 湿度（百分比）
    val weatherIcon: String,                // 天气图标URL
    val currentHourWeather: HourWeather?    // 当前时段的天气信息
) {
    /**
     * 格式化为显示字符串
     * 格式：省份城市:描述 温度℃ 湿度%RH
     */
    fun toDisplayString(): String {
        val location = "$city$county$town"
        return "$location:$description ${temperature}℃ ${humidity}%RH"
    }
}
