package xyz.jasenon.classtimetable.ui.component.date_time

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

/**
 * 日期时间行内文本：只观察 [DateTimeObservable.dateTime]。
 * 由 [DateTimeObservable.start] 在应用启动时推送，无需 ViewModel。
 */
@Composable
fun DateTimeInLineText(modifier: Modifier = Modifier) {
    val dateTime by DateTimeObservable.dateTime.collectAsState(initial = DateTimeState())

    Text(
        text = "${dateTime.date} ${dateTime.time}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        fontSize = 25.sp,
        modifier = modifier
    )
}
