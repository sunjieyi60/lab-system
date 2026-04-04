package xyz.jasenon.classtimetable.ui.component.date_time

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期时间可观察（状态推送）
 *
 * 调用 [start] 后每秒推送一次，UI 观察 [dateTime] 即可。
 */
object DateTimeObservable {

    private val _dateTime = MutableStateFlow(DateTimeState())
    val dateTime: StateFlow<DateTimeState> = _dateTime.asStateFlow()

    fun start(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                _dateTime.value = DateTimeState(
                    date = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE).format(Date()),
                    time = SimpleDateFormat("HH:mm:ss", Locale.CHINESE).format(Date())
                )
                delay(1000)
            }
        }
    }
}

data class DateTimeState(
    val date: String = "",
    val time: String = ""
)
