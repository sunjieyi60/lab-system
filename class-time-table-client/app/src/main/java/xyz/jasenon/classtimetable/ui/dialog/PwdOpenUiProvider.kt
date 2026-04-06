package xyz.jasenon.classtimetable.ui.dialog

import android.icu.util.TimeUnit
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import xyz.jasenon.classtimetable.config.DeviceRuntimeConfigObservable

class PwdOpenUiProvider(title: String, description : String,
    modifier: Modifier = Modifier.width(800.dp).height(500.dp)
) : DoorOpeningUIProvider(title, description, modifier) {

    @Composable
    override fun Ui(
        visible: Boolean,
        onTimeoutUpdate: (Long) -> Unit,
        setCardVisible: (Boolean) -> Unit
    ) {
        // 使用服务端下发的运行时配置
        val targetPwd = DeviceRuntimeConfigObservable.getPassword()
        val timeout = DeviceRuntimeConfigObservable.getTimeout()
        val limitMillis = remember(timeout) { timeout * 1000L }
        var remaining by remember { mutableStateOf(limitMillis) }
        onTimeoutUpdate(remaining)
        var password by remember { mutableStateOf("") }
        val confirmEnabled = password.length == targetPwd.length

        LaunchedEffect(limitMillis) {
            while (remaining > 0L) {
                delay(1000)
                remaining -= 1000
                onTimeoutUpdate(remaining)
            }
            setCardVisible(false)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                text = if (password.length > 0) "*".repeat(password.length) else description,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                fontSize = 32.sp
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NumKey("7") { if (password.length < targetPwd.length) password += "7" }
                        NumKey("8") { if (password.length < targetPwd.length) password += "8" }
                        NumKey("9") { if (password.length < targetPwd.length) password += "9" }
                        ActionKey("删除") { if (password.isNotEmpty()) password = password.dropLast(1) }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NumKey("4") { if (password.length < targetPwd.length) password += "4" }
                        NumKey("5") { if (password.length < targetPwd.length) password += "5" }
                        NumKey("6") { if (password.length < targetPwd.length) password += "6" }
                        ActionKey("清空") { password = "" }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NumKey("1") { if (password.length < targetPwd.length) password += "1" }
                        NumKey("2") { if (password.length < targetPwd.length) password += "2" }
                        NumKey("3") { if (password.length < targetPwd.length) password += "3" }
                        ActionKey("退出") { setCardVisible(false) }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EmptyKey()
                        NumKey("0") { if (password.length < targetPwd.length) password += "0" }
                        EmptyKey()
                        Button(
                            onClick = {
                                if (password == targetPwd) setCardVisible(false)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            enabled = confirmEnabled
                        ) {
                            Text("确认")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RowScope.NumKey(text: String, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
        ) {
            Text(text, style = MaterialTheme.typography.titleLarge)
        }
    }

    @Composable
    private fun RowScope.ActionKey(text: String, onClick: () -> Unit) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            border = BorderStroke(2.dp, color = Color.Black)
        ) {
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    private fun RowScope.EmptyKey() {
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
        )
    }

    override fun onConfirm() {}
    override fun onCancel() {}
}

fun toMillis(value: Int?, unit: TimeUnit?): Long {
    val v = value ?: 30
    return when (unit ?: TimeUnit.SECOND) {
        TimeUnit.SECOND -> v * 1000L
        TimeUnit.MINUTE -> v * 60000L
        TimeUnit.HOUR -> v * 3600000L
        TimeUnit.DAY -> v * 86400000L
        else -> v * 1000L
    }
}

@Preview(showBackground = true, widthDp = 1920, heightDp = 1080)
@Composable
private fun PwdOpenPreview() {
    DoorOpeningDialog(
        visible = true,
        onClose = {},
        type = DoorOpeningType.PASSWORD,
        uiProvider = PwdOpenUiProvider("密码开门", "请输入6位密码")
    )
}
