package xyz.jasenon.classtimetable.ui.component.weather_ifno

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import xyz.jasenon.classtimetable.network.observe.RemoteDataObservable

/**
 * 天气行内文本：只观察 [RemoteDataObservable.weatherState]。
 * 天气由 [WeatherPusher] 在应用启动时推送，无需 ViewModel。
 */
@Composable
fun WeatherInfoInLineText(modifier: Modifier = Modifier) {
    val weatherState by RemoteDataObservable.weatherState.collectAsState(initial = null)
    val displayText = weatherState?.getDisplayText() ?: "暂无天气信息"

    Text(
        text = displayText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
        fontSize = 23.sp,
        modifier = modifier
    )
}
