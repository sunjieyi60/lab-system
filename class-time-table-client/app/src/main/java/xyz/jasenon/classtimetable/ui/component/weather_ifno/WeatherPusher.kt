package xyz.jasenon.classtimetable.ui.component.weather_ifno

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.jasenon.classtimetable.config.ConfigObservable
import xyz.jasenon.classtimetable.network.observe.RemoteDataObservable

/**
 * 天气推送器（唯一入口，状态推送）
 *
 * 在应用启动时调用 [start] 一次；内部观察 [ConfigObservable]，配置就绪后创建数据源并定时拉取，结果推送到 [RemoteDataObservable.weatherState]。
 * UI 只观察 [RemoteDataObservable.weatherState]，不依赖任何 ViewModel。
 */
object WeatherPusher {

    fun start(context: Context, scope: CoroutineScope) {
        scope.launch {
            val config = ConfigObservable.config.first { it != null } ?: return@launch
            val appContext = context.applicationContext
            val dataSource = WeatherDataSourceFactory.create(appContext, config)
            val intervalMinutes = config.heziConfig.pollIntervalMinutes?.toLong() ?: config.weatherConfig.updateInterval?.toLong() ?: 120L

            while (true) {
                RemoteDataObservable.updateWeather(WeatherState(isLoading = true, error = null))
                try {
                    val info = dataSource.fetchWeather()
                    RemoteDataObservable.updateWeather(
                        WeatherState(weatherInfo = info, isLoading = false, error = null)
                    )
                } catch (e: Exception) {
                    RemoteDataObservable.updateWeather(
                        WeatherState(isLoading = false, error = e.message ?: "获取天气失败")
                    )
                }
                delay(intervalMinutes * 60 * 1000)
            }
        }
    }
}
