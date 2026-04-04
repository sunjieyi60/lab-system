package xyz.jasenon.classtimetable.network.observe

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.jasenon.classtimetable.ui.component.weather_ifno.WeatherState
import xyz.jasenon.classtimetable.dto.CourseScheduleDto

/**
 * 远程/全局数据可观察中心（状态推送 + 观察者模式）
 *
 * 所有需要驱动 UI 的数据统一由此处推送，UI/组件通过 collect 对应 StateFlow 实现页面刷新与动态监听。
 * - 课表：Tio 收到 TIMETABLE_RESP 后由 [TimetablePacketHandler] 调用 [updateTimetable]
 * - 门禁：Tio 收到 DOOR_STATUS 后由 [DoorStatusPacketHandler] 调用 [updateDoorStatus]
 * - 天气：由 [WeatherPusher] 在应用启动时根据 [ConfigObservable] 创建数据源并定时拉取后调用 [updateWeather]
 */
object RemoteDataObservable {

    private val _timetableData = MutableStateFlow<List<CourseScheduleDto>>(emptyList())
    val timetableData: StateFlow<List<CourseScheduleDto>> = _timetableData.asStateFlow()

    private val _doorStatus = MutableStateFlow<DoorStatusData?>(null)
    val doorStatus: StateFlow<DoorStatusData?> = _doorStatus.asStateFlow()

    private val _weatherState = MutableStateFlow<WeatherState?>(null)
    val weatherState: StateFlow<WeatherState?> = _weatherState.asStateFlow()

    fun updateTimetable(data: List<CourseScheduleDto>) {
        _timetableData.value = data
    }

    fun updateDoorStatus(data: DoorStatusData?) {
        _doorStatus.value = data
    }

    /** 天气状态更新，由 [WeatherPusher] 在拉取后调用 */
    fun updateWeather(state: WeatherState) {
        _weatherState.value = state
    }
}

/**
 * 门禁状态数据（示例，按协议扩展）
 */
data class DoorStatusData(
    val open: Boolean,
    val message: String? = null
)
