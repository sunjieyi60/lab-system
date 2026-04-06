package xyz.jasenon.classtimetable.network.observe

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.jasenon.classtimetable.ui.component.weather_ifno.WeatherInfo
import xyz.jasenon.classtimetable.ui.component.weather_ifno.WeatherState
import xyz.jasenon.classtimetable.dto.CourseScheduleDto
import xyz.jasenon.classtimetable.constants.WeekType

/**
 * 远程/全局数据可观察中心（Mock 静态数据版本）
 *
 * 单例对象，集中管理应用的所有数据状态。
 * 提供 [StateFlow] 供 UI 组件订阅，实现响应式数据更新。
 *
 * ## 架构设计
 *
 * 采用 **单向数据流** 架构：
 * ```
 * RemoteDataObservable (数据源)
 *            │
 *            ▼ (StateFlow 推送)
 *     UI 组件收集状态
 *            │
 *            ▼ (自动重组)
 *      Compose 界面
 * ```
 *
 * ## 协程使用
 *
 * 所有数据流都是 [StateFlow]，需要在协程中收集：
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val courses by RemoteDataObservable.timetableData.collectAsState()
 *     // courses 变化时自动触发重组
 * }
 * ```
 *
 * ## 当前实现
 *
 * 当前版本提供 **Mock 静态数据**，用于 UI 开发和演示：
 * - 课表：固定示例课程数据
 * - 门禁：固定状态
 * - 天气：固定天气数据
 *
 * ## 预留接口
 *
 * [updateTimetable]、[updateDoorStatus]、[updateWeather] 为预留接口，
 * 后续接入 RSocket 后可通过这些方法从服务器更新数据。
 *
 * @see StateFlow
 * @see androidx.compose.runtime.collectAsState
 */
object RemoteDataObservable {

    /**
     * 课表数据 StateFlow
     *
     * 提供实验室课程表数据，包含一周的所有课程安排。
     * 当前为 Mock 数据，包含 5 门示例课程。
     *
     * 数据格式：[List]<[CourseScheduleDto]>，每个元素代表一门课程
     *
     * UI 使用示例：
     * ```kotlin
     * val courses by RemoteDataObservable.timetableData.collectAsState()
     * MiddleColumn(courses = courses, currentWeek = 1)
     * ```
     */
    private val _timetableData = MutableStateFlow(getSampleCourses())

    /**
     * 课表数据只读 StateFlow
     *
     * UI 组件订阅此 Flow 获取课表更新
     */
    val timetableData: StateFlow<List<CourseScheduleDto>> = _timetableData.asStateFlow()

    /**
     * 门禁状态 StateFlow
     *
     * 提供实验室门禁的开关状态和相关信息。
     * 当前为 Mock 数据，默认显示"门禁关闭"。
     *
     * 数据格式：[DoorStatusData]，包含：
     * - [DoorStatusData.open]: Boolean，是否开启
     * - [DoorStatusData.message]: String?，状态描述
     */
    private val _doorStatus = MutableStateFlow(DoorStatusData(open = false, message = "门禁关闭"))

    /**
     * 门禁状态只读 StateFlow
     *
     * UI 组件订阅此 Flow 获取门禁状态更新
     */
    val doorStatus: StateFlow<DoorStatusData?> = _doorStatus.asStateFlow()

    /**
     * 天气状态 StateFlow
     *
     * 提供当前天气信息，包含温度、湿度、天气描述等。
     * 当前为 Mock 数据：武汉，晴，24°C，65% 湿度。
     *
     * 数据格式：[WeatherState]，包含：
     * - [WeatherState.weatherInfo]: [WeatherInfo]，天气详情
     * - [WeatherState.isLoading]: Boolean，是否加载中
     * - [WeatherState.error]: String?，错误信息
     */
    private val _weatherState = MutableStateFlow(
        WeatherState(
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
    )

    /**
     * 天气状态只读 StateFlow
     *
     * UI 组件订阅此 Flow 获取天气更新
     */
    val weatherState: StateFlow<WeatherState?> = _weatherState.asStateFlow()

    /**
     * 更新课表数据
     *
     * **预留接口**：用于后续从服务器获取真实课表数据时更新。
     * 调用后会触发 [timetableData] 的状态更新，订阅的 UI 组件会自动刷新。
     *
     * @param data 新的课表数据列表
     *
     * @sample
     * ```kotlin
     * // 从服务器获取数据后更新
     * val courses = fetchCoursesFromServer()
     * RemoteDataObservable.updateTimetable(courses)
     * ```
     */
    fun updateTimetable(data: List<CourseScheduleDto>) {
        _timetableData.value = data
    }

    /**
     * 更新门禁状态
     *
     * **预留接口**：用于后续从服务器或本地设备获取门禁状态时更新。
     * 调用后会触发 [doorStatus] 的状态更新。
     *
     * @param data 新的门禁状态，为 null 时表示清除状态
     *
     * @sample
     * ```kotlin
     * // 门禁状态变化时更新
     * RemoteDataObservable.updateDoorStatus(DoorStatusData(open = true, message = "已开启"))
     * ```
     */
    fun updateDoorStatus(data: DoorStatusData) {
        _doorStatus.value = data
    }

    /**
     * 更新天气状态
     *
     * 通过 RSocket 从服务端获取天气数据后调用此接口更新 UI。
     * 调用后会触发 [weatherState] 的状态更新。
     *
     * @param state 新的天气状态
     *
     * @sample
     * ```kotlin
     * // 通过 RSocket 收到天气数据后更新
     * RemoteDataObservable.updateWeather(WeatherState(weatherInfo = newInfo))
     * ```
     */
    fun updateWeather(state: WeatherState) {
        _weatherState.value = state
    }
}

/**
 * 门禁状态数据类
 *
 * 封装门禁的开关状态和描述信息。
 *
 * @property open Boolean，true 表示门禁开启，false 表示关闭
 * @property message String?，状态描述信息，如"已开启"、"维护中"等
 */
data class DoorStatusData(
    val open: Boolean,
    val message: String? = null
)

/**
 * 生成示例课程数据
 *
 * 用于 Mock 数据展示，提供 5 门示例课程：
 * 1. 计算机网络(B) - 周二 1-2 节
 * 2. UML与软件工程建模 - 周二 3-4 节
 * 3. 数据库原理与应用 - 周三 1-2 节
 * 4. 最优化理论与方法 - 周四 3-4 节
 * 5. Android平台智能移动开发 - 周五 9-10 节
 *
 * @return [List]<[CourseScheduleDto]> 示例课程列表
 */
private fun getSampleCourses(): List<CourseScheduleDto> {
    return listOf(
        CourseScheduleDto(
            id = 1L,
            courseId = 1L,
            courseName = "计算机网络(B)",
            teacherName = "周斌",
            laboratoryName = "11号楼 11413",
            weekType = WeekType.Both,
            startWeek = 1,
            endWeek = 16,
            weekdays = mutableListOf(2), // 星期二
            startSection = 1,
            endSection = 2
        ),
        CourseScheduleDto(
            id = 2L,
            courseId = 2L,
            courseName = "UML与软件工程建模",
            teacherName = "刘卫平",
            laboratoryName = "9号楼 S090205",
            weekType = WeekType.Both,
            startWeek = 1,
            endWeek = 16,
            weekdays = mutableListOf(2), // 星期二
            startSection = 3,
            endSection = 4
        ),
        CourseScheduleDto(
            id = 3L,
            courseId = 3L,
            courseName = "数据库原理与应用",
            teacherName = "曾广平",
            laboratoryName = "11号楼 11413",
            weekType = WeekType.Both,
            startWeek = 1,
            endWeek = 16,
            weekdays = mutableListOf(3), // 星期三
            startSection = 1,
            endSection = 2
        ),
        CourseScheduleDto(
            id = 4L,
            courseId = 4L,
            courseName = "最优化理论与方法",
            teacherName = "王曦照",
            laboratoryName = "11号楼 11413",
            weekType = WeekType.Both,
            startWeek = 1,
            endWeek = 15,
            weekdays = mutableListOf(5), // 星期四
            startSection = 3,
            endSection = 4
        ),
        CourseScheduleDto(
            id = 5L,
            courseId = 5L,
            courseName = "Android平台智能移动开发",
            teacherName = "张世华",
            laboratoryName = "11号楼 11413",
            weekType = WeekType.Both,
            startWeek = 1,
            endWeek = 16,
            weekdays = mutableListOf(6), // 星期五
            startSection = 9,
            endSection = 10
        )
    )
}
