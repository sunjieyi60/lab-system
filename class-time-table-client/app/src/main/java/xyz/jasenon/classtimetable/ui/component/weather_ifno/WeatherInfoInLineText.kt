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
 * 天气行内文本组件
 * <p>
 * 观察 [RemoteDataObservable.weatherState] 并显示天气信息。
 * 天气数据通过 RSocket 从服务端获取并推送到 [RemoteDataObservable]。
 * </p>
 *
 * @author Jasenon_ce
 * @see RemoteDataObservable
 * @since 1.0.0
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
