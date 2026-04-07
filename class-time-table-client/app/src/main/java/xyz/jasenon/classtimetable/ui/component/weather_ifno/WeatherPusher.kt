package xyz.jasenon.classtimetable.ui.component.weather_ifno

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.jasenon.classtimetable.network.observe.RemoteDataObservable

/**
 * 天气推送器（Mock 数据版本）
 *
 * 负责天气数据的获取和推送，将天气状态推送到 [RemoteDataObservable.weatherState]。
 *
 * ## 职责
 *
 * - 在应用启动时推送初始 Mock 天气数据
 * - 后续可扩展为定时从服务器获取天气数据并推送
 *
 * ## 架构位置
 *
 * ```
 * WeatherPusher (数据获取)
 *      │
 *      ▼ (推送)
 * RemoteDataObservable.weatherState
 *      │
 *      ▼ (StateFlow)
 * WeatherInfoInLineText (UI 展示)
 * ```
 *
 * ## 协程使用
 *
 * 使用外部传入的 [CoroutineScope]，通常来自：
 * - Activity 的 lifecycleScope
 * - Application 的全局 Scope
 * - 自定义的 ApplicationScope
 *
 * 协程在 [CoroutineScope.launch] 中启动，生命周期与传入的 Scope 绑定。
 *
 * ## 与 ViewModel 的区别
 *
 * 本类不使用 ViewModel，直接推送数据到 [RemoteDataObservable]。
 * UI 组件通过收集 [RemoteDataObservable.weatherState] 获取数据。
 * 这种设计简化了架构，适合中小型应用。
 *
 * ## 使用示例
 *
 * ```kotlin
 * class MainActivity : ComponentActivity() {
 *     private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         // 启动天气推送
 *         WeatherPusher.start(applicationScope)
 *     }
 * }
 * ```
 *
 * @see RemoteDataObservable.weatherState
 * @see WeatherInfoInLineText
 */
object WeatherPusher {

    /**
     * 启动天气推送
     *
     * 在应用启动时调用一次，推送初始 Mock 天气数据到 [RemoteDataObservable]。
     *
     * ## 协程上下文
     * - 在传入的 [scope] 中启动新协程
     * - 使用 [Dispatchers.Default] 或传入 Scope 的 Dispatcher
     * - 不会阻塞调用线程
     *
     * ## 多次调用
     * 多次调用会重复推送数据（当前实现），后续可优化为只推送一次或定时推送。
     *
     * @param scope 协程作用域，用于启动推送协程
     *
     * @sample
     * ```kotlin
     * val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
     * WeatherPusher.start(applicationScope)
     * ```
     */
    fun start(scope: CoroutineScope) {
        scope.launch {
            // 推送 Mock 天气数据
            val mockWeatherState = WeatherState(
                weatherInfo = WeatherInfo(
                    county = "洪山区",
                    city = "武汉",
                    town = "",
                    description = "晴",
                    temperature = 24.0,
                    humidity = 65.0,
                    weatherIcon = "",
                    currentHourWeather = null
                ),
                isLoading = false,
                error = null
            )
            RemoteDataObservable.updateWeather(mockWeatherState)
        }
    }
}
